package net.shiroha233.roadweaverpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;

/**
 * 玩家委托数据
 */
public class PlayerQuestData {
    
    private final UUID playerId;
    private final Map<ResourceLocation, QuestInstance> activeQuests = new LinkedHashMap<>();
    private final Set<ResourceLocation> completedQuests = new HashSet<>();
    private final Map<ResourceLocation, Integer> completionCounts = new HashMap<>();
    private final Map<ResourceLocation, Long> cooldowns = new HashMap<>();
    private final Map<ResourceLocation, Integer> reputationXp = new HashMap<>();
    private final Map<ResourceLocation, Integer> reputationLevels = new HashMap<>();
    private int totalQuestsCompleted = 0;
    private int totalQuestsFailed = 0;
    
    public PlayerQuestData(UUID playerId) {
        this.playerId = playerId;
    }
    
    // region Getters
    public UUID getPlayerId() { return playerId; }
    
    public Collection<QuestInstance> getActiveQuests() {
        return Collections.unmodifiableCollection(activeQuests.values());
    }
    
    public QuestInstance getActiveQuest(ResourceLocation questId) {
        return activeQuests.get(questId);
    }
    
    public boolean hasActiveQuest(ResourceLocation questId) {
        return activeQuests.containsKey(questId);
    }
    
    public Set<ResourceLocation> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }
    
    public boolean hasCompletedQuest(ResourceLocation questId) {
        return completedQuests.contains(questId);
    }
    
    public int getCompletionCount(ResourceLocation questId) {
        return completionCounts.getOrDefault(questId, 0);
    }
    
    public Set<ResourceLocation> getActiveQuestIds() {
        return Collections.unmodifiableSet(activeQuests.keySet());
    }
    
    public int getReputationXp(ResourceLocation factionId) {
        return reputationXp.getOrDefault(factionId, 0);
    }
    
    public int getReputationLevel(ResourceLocation factionId) {
        return reputationLevels.getOrDefault(factionId, 0);
    }
    
    public Map<ResourceLocation, Integer> getAllReputationXp() {
        return Collections.unmodifiableMap(reputationXp);
    }
    
    public Map<ResourceLocation, Integer> getAllReputationLevels() {
        return Collections.unmodifiableMap(reputationLevels);
    }
    
    public int getTotalQuestsCompleted() { return totalQuestsCompleted; }
    public int getTotalQuestsFailed() { return totalQuestsFailed; }
    // endregion
    
    // region 委托操作
    public void addActiveQuest(QuestInstance instance) {
        activeQuests.put(instance.getQuestId(), instance);
    }
    
    public QuestInstance removeActiveQuest(ResourceLocation questId) {
        return activeQuests.remove(questId);
    }
    
    public void markQuestCompleted(ResourceLocation questId) {
        completedQuests.add(questId);
        completionCounts.merge(questId, 1, Integer::sum);
        totalQuestsCompleted++;
    }
    
    public void markQuestFailed(ResourceLocation questId) {
        totalQuestsFailed++;
    }
    
    public void setCooldown(ResourceLocation questId, int seconds) {
        cooldowns.put(questId, System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public boolean isOnCooldown(ResourceLocation questId) {
        Long endTime = cooldowns.get(questId);
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(questId);
            return false;
        }
        return true;
    }
    
    public int getCooldownRemaining(ResourceLocation questId) {
        Long endTime = cooldowns.get(questId);
        if (endTime == null) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }
    
    public void addReputationXp(ResourceLocation factionId, int amount) {
        reputationXp.merge(factionId, amount, Integer::sum);
    }

    public void setReputationLevel(ResourceLocation factionId, int level) {
        reputationLevels.put(factionId, level);
    }
    // endregion
    
    // region NBT序列化
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        
        ListTag activeList = new ListTag();
        for (QuestInstance instance : activeQuests.values()) {
            activeList.add(instance.toNbt());
        }
        tag.put("activeQuests", activeList);
        
        ListTag completedList = new ListTag();
        for (ResourceLocation id : completedQuests) {
            completedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("completedQuests", completedList);
        
        CompoundTag countsTag = new CompoundTag();
        completionCounts.forEach((id, count) -> countsTag.putInt(id.toString(), count));
        tag.put("completionCounts", countsTag);
        
        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach((id, time) -> cooldownTag.putLong(id.toString(), time));
        tag.put("cooldowns", cooldownTag);
        
        CompoundTag xpTag = new CompoundTag();
        reputationXp.forEach((id, val) -> xpTag.putInt(id.toString(), val));
        tag.put("reputationXp", xpTag);
        
        CompoundTag levelTag = new CompoundTag();
        reputationLevels.forEach((id, val) -> levelTag.putInt(id.toString(), val));
        tag.put("reputationLevels", levelTag);
        
        tag.putInt("totalCompleted", totalQuestsCompleted);
        tag.putInt("totalFailed", totalQuestsFailed);
        
        return tag;
    }
    
    public static PlayerQuestData fromNbt(CompoundTag tag) {
        UUID playerId = tag.getUUID("playerId");
        PlayerQuestData data = new PlayerQuestData(playerId);
        
        ListTag activeList = tag.getList("activeQuests", Tag.TAG_COMPOUND);
        for (int i = 0; i < activeList.size(); i++) {
            QuestInstance instance = QuestInstance.fromNbt(activeList.getCompound(i));
            data.activeQuests.put(instance.getQuestId(), instance);
        }
        
        ListTag completedList = tag.getList("completedQuests", Tag.TAG_STRING);
        for (int i = 0; i < completedList.size(); i++) {
            data.completedQuests.add(new ResourceLocation(completedList.getString(i)));
        }
        
        CompoundTag countsTag = tag.getCompound("completionCounts");
        for (String key : countsTag.getAllKeys()) {
            data.completionCounts.put(new ResourceLocation(key), countsTag.getInt(key));
        }
        
        CompoundTag cooldownTag = tag.getCompound("cooldowns");
        for (String key : cooldownTag.getAllKeys()) {
            data.cooldowns.put(new ResourceLocation(key), cooldownTag.getLong(key));
        }
        
        if (tag.contains("reputationXp")) {
            CompoundTag xpTag = tag.getCompound("reputationXp");
            for (String key : xpTag.getAllKeys()) {
                data.reputationXp.put(new ResourceLocation(key), xpTag.getInt(key));
            }
        }
        
        if (tag.contains("reputationLevels")) {
            CompoundTag levelTag = tag.getCompound("reputationLevels");
            for (String key : levelTag.getAllKeys()) {
                data.reputationLevels.put(new ResourceLocation(key), levelTag.getInt(key));
            }
        }
        
        data.totalQuestsCompleted = tag.getInt("totalCompleted");
        data.totalQuestsFailed = tag.getInt("totalFailed");
        
        return data;
    }
    // endregion
}
