package net.shiroha233.roadweaverpg.quest.instance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;

import java.util.*;

/**
 * 委托实例 - 玩家接取后的运行时状态
 */
public class QuestInstance {
    
    private final UUID instanceId;
    private final ResourceLocation questId;
    private final UUID playerId;
    
    private QuestState state;
    private final Map<String, ObjectiveProgress> objectiveProgress;
    
    private final long acceptedTime;
    private long completedTime;
    private long expirationTime;
    private float difficultyMultiplier;
    
    public QuestInstance(ResourceLocation questId, UUID playerId, QuestDefinition definition) {
        this.instanceId = UUID.randomUUID();
        this.questId = questId;
        this.playerId = playerId;
        this.state = QuestState.IN_PROGRESS;
        this.acceptedTime = System.currentTimeMillis();
        this.completedTime = 0;
        this.difficultyMultiplier = 1.0f;
        
        this.objectiveProgress = new LinkedHashMap<>();
        for (QuestObjective obj : definition.getObjectives()) {
            objectiveProgress.put(obj.getId(), new ObjectiveProgress(obj));
        }
        
        if (definition.hasTimeLimit()) {
            this.expirationTime = acceptedTime + (definition.getTimeLimit() * 1000L);
        } else {
            this.expirationTime = 0;
        }
    }
    
    private QuestInstance(UUID instanceId, ResourceLocation questId, UUID playerId, long acceptedTime) {
        this.instanceId = instanceId;
        this.questId = questId;
        this.playerId = playerId;
        this.objectiveProgress = new LinkedHashMap<>();
        this.acceptedTime = acceptedTime;
    }
    
    // region Getters
    public UUID getInstanceId() { return instanceId; }
    public ResourceLocation getQuestId() { return questId; }
    public UUID getPlayerId() { return playerId; }
    public QuestState getState() { return state; }
    public long getAcceptedTime() { return acceptedTime; }
    public long getCompletedTime() { return completedTime; }
    public long getExpirationTime() { return expirationTime; }
    public float getDifficultyMultiplier() { return difficultyMultiplier; }
    
    public Collection<ObjectiveProgress> getObjectiveProgresses() {
        return objectiveProgress.values();
    }
    
    public ObjectiveProgress getObjectiveProgress(String objectiveId) {
        return objectiveProgress.get(objectiveId);
    }
    // endregion
    
    // region 状态管理
    /**
     * 设置状态（内部使用，不验证状态转移）
     * 外部应通过服务层进行状态变更以确保验证
     */
    public void setState(QuestState newState) {
        if (this.state.isTerminal()) return;
        this.state = newState;
        if (newState == QuestState.COMPLETED || newState == QuestState.TURNED_IN) {
            this.completedTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 安全地设置状态（验证状态转移合法性）
     * @return 是否成功转移
     */
    public boolean setStateSafe(QuestState newState) {
        if (this.state.isTerminal()) return false;
        
        // 使用状态机验证
        var stateMachine = net.shiroha233.roadweaverpg.quest.state.QuestStateMachine.getInstance();
        if (!stateMachine.canTransition(this.state, newState)) {
            return false;
        }
        
        this.state = newState;
        if (newState == QuestState.COMPLETED || newState == QuestState.TURNED_IN) {
            this.completedTime = System.currentTimeMillis();
        }
        return true;
    }
    
    public void setDifficultyMultiplier(float multiplier) {
        this.difficultyMultiplier = multiplier;
    }
    
    public boolean isExpired() {
        return expirationTime > 0 && System.currentTimeMillis() > expirationTime;
    }
    
    public int getRemainingTime() {
        if (expirationTime <= 0) return -1;
        long remaining = expirationTime - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }
    
    public boolean areAllObjectivesComplete() {
        return objectiveProgress.values().stream().allMatch(ObjectiveProgress::isCompleted);
    }
    
    public void updateObjectiveProgress(String objectiveId, int progress) {
        ObjectiveProgress op = objectiveProgress.get(objectiveId);
        if (op != null) {
            op.addProgress(progress);
            checkCompletion();
        }
    }
    
    public void setObjectiveProgress(String objectiveId, int progress) {
        ObjectiveProgress op = objectiveProgress.get(objectiveId);
        if (op != null) {
            op.setProgress(progress);
            checkCompletion();
        }
    }
    
    private void checkCompletion() {
        if (state == QuestState.IN_PROGRESS && areAllObjectivesComplete()) {
            setState(QuestState.COMPLETED);
        }
    }
    
    public float getTotalProgress() {
        if (objectiveProgress.isEmpty()) return 0f;
        float total = 0f;
        for (ObjectiveProgress op : objectiveProgress.values()) {
            total += op.getProgressPercent();
        }
        return total / objectiveProgress.size();
    }
    // endregion
    
    // region 序列化
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(instanceId);
        buf.writeResourceLocation(questId);
        buf.writeUUID(playerId);
        buf.writeEnum(state);
        buf.writeLong(acceptedTime);
        buf.writeLong(completedTime);
        buf.writeLong(expirationTime);
        buf.writeFloat(difficultyMultiplier);
        
        buf.writeVarInt(objectiveProgress.size());
        for (ObjectiveProgress op : objectiveProgress.values()) {
            op.toNetwork(buf);
        }
    }
    
    public static QuestInstance fromNetwork(FriendlyByteBuf buf) {
        UUID instId = buf.readUUID();
        ResourceLocation qId = buf.readResourceLocation();
        UUID pId = buf.readUUID();
        QuestState state = buf.readEnum(QuestState.class);
        long acceptedTime = buf.readLong();
        long completedTime = buf.readLong();
        long expirationTime = buf.readLong();
        float difficultyMultiplier = buf.readFloat();
        
        QuestInstance instance = new QuestInstance(instId, qId, pId, acceptedTime);
        instance.state = state;
        instance.completedTime = completedTime;
        instance.expirationTime = expirationTime;
        instance.difficultyMultiplier = difficultyMultiplier;
        
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            ObjectiveProgress op = ObjectiveProgress.fromNetwork(buf);
            instance.objectiveProgress.put(op.getObjectiveId(), op);
        }
        
        return instance;
    }
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("instanceId", instanceId);
        tag.putString("questId", questId.toString());
        tag.putUUID("playerId", playerId);
        tag.putString("state", state.getSerializedName());
        tag.putLong("acceptedTime", acceptedTime);
        tag.putLong("completedTime", completedTime);
        tag.putLong("expirationTime", expirationTime);
        tag.putFloat("difficulty", difficultyMultiplier);
        
        ListTag progressList = new ListTag();
        for (ObjectiveProgress op : objectiveProgress.values()) {
            progressList.add(op.toNbt());
        }
        tag.put("progress", progressList);
        
        return tag;
    }
    
    public static QuestInstance fromNbt(CompoundTag tag) {
        UUID instId = tag.getUUID("instanceId");
        ResourceLocation qId = new ResourceLocation(tag.getString("questId"));
        UUID pId = tag.getUUID("playerId");
        long acceptedTime = tag.getLong("acceptedTime");
        
        QuestInstance instance = new QuestInstance(instId, qId, pId, acceptedTime);
        instance.state = QuestState.fromString(tag.getString("state"));
        instance.completedTime = tag.getLong("completedTime");
        instance.expirationTime = tag.getLong("expirationTime");
        instance.difficultyMultiplier = tag.getFloat("difficulty");
        
        ListTag progressList = tag.getList("progress", Tag.TAG_COMPOUND);
        for (int i = 0; i < progressList.size(); i++) {
            ObjectiveProgress op = ObjectiveProgress.fromNbt(progressList.getCompound(i));
            instance.objectiveProgress.put(op.getObjectiveId(), op);
        }
        
        return instance;
    }
    // endregion
}
