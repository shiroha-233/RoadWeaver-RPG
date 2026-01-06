package net.shiroha233.roadweaverpg.quest.instance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;

/**
 * 目标进度追踪
 */
public class ObjectiveProgress {
    
    private final String objectiveId;
    private int currentProgress;
    private final int requiredAmount;
    private boolean completed;
    
    public ObjectiveProgress(String objectiveId, int requiredAmount) {
        this.objectiveId = objectiveId;
        this.requiredAmount = requiredAmount;
        this.currentProgress = 0;
        this.completed = false;
    }
    
    public ObjectiveProgress(QuestObjective objective) {
        this(objective.getId(), objective.getRequiredAmount());
    }
    
    public String getObjectiveId() { return objectiveId; }
    public int getCurrentProgress() { return currentProgress; }
    public int getRequiredAmount() { return requiredAmount; }
    public boolean isCompleted() { return completed; }
    
    public void addProgress(int amount) {
        if (completed) return;
        this.currentProgress = Math.min(currentProgress + amount, requiredAmount);
        checkCompletion();
    }
    
    public void setProgress(int amount) {
        if (completed) return;
        this.currentProgress = Math.min(amount, requiredAmount);
        checkCompletion();
    }
    
    private void checkCompletion() {
        if (currentProgress >= requiredAmount) {
            completed = true;
        }
    }
    
    public float getProgressPercent() {
        return requiredAmount > 0 ? (float) currentProgress / requiredAmount : 0f;
    }
    
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(objectiveId);
        buf.writeVarInt(currentProgress);
        buf.writeVarInt(requiredAmount);
        buf.writeBoolean(completed);
    }
    
    public static ObjectiveProgress fromNetwork(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int current = buf.readVarInt();
        int required = buf.readVarInt();
        boolean done = buf.readBoolean();
        
        ObjectiveProgress progress = new ObjectiveProgress(id, required);
        progress.currentProgress = current;
        progress.completed = done;
        return progress;
    }
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", objectiveId);
        tag.putInt("current", currentProgress);
        tag.putInt("required", requiredAmount);
        tag.putBoolean("completed", completed);
        return tag;
    }
    
    public static ObjectiveProgress fromNbt(CompoundTag tag) {
        String id = tag.getString("id");
        int required = tag.getInt("required");
        ObjectiveProgress progress = new ObjectiveProgress(id, required);
        progress.currentProgress = tag.getInt("current");
        progress.completed = tag.getBoolean("completed");
        return progress;
    }
}
