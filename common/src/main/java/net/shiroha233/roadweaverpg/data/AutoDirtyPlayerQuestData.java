package net.shiroha233.roadweaverpg.data;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;
import java.util.function.Consumer;

/**
 * 自动标记脏数据的玩家委托数据包装器
 * 
 * 设计原则：
 * - 装饰器模式：包装原始PlayerQuestData，自动标记变更
 * - 单一职责：只负责自动标记，不改变业务逻辑
 * - 开闭原则：通过包装扩展功能，不修改原有类
 * 
 * 解决问题：
 * - 避免手动调用markDirty()导致的遗漏
 * - 确保所有数据修改都被持久化
 */
public class AutoDirtyPlayerQuestData {
    
    private final PlayerQuestData delegate;
    private final Consumer<PlayerQuestData> onDirty;
    
    public AutoDirtyPlayerQuestData(PlayerQuestData delegate, Consumer<PlayerQuestData> onDirty) {
        this.delegate = delegate;
        this.onDirty = onDirty;
    }
    
    // region 自动标记的修改方法
    
    public void addActiveQuest(QuestInstance instance) {
        delegate.addActiveQuest(instance);
        markDirty();
    }
    
    public QuestInstance removeActiveQuest(ResourceLocation questId) {
        QuestInstance result = delegate.removeActiveQuest(questId);
        if (result != null) {
            markDirty();
        }
        return result;
    }
    
    public void markQuestCompleted(ResourceLocation questId) {
        delegate.markQuestCompleted(questId);
        markDirty();
    }
    
    public void markQuestFailed(ResourceLocation questId) {
        delegate.markQuestFailed(questId);
        markDirty();
    }
    
    public void setCooldown(ResourceLocation questId, int seconds) {
        delegate.setCooldown(questId, seconds);
        markDirty();
    }
    
    public void addReputationXp(ResourceLocation factionId, int amount) {
        delegate.addReputationXp(factionId, amount);
        markDirty();
    }
    
    public void setReputationLevel(ResourceLocation factionId, int level) {
        delegate.setReputationLevel(factionId, level);
        markDirty();
    }
    
    public void setDailyQuests(List<ResourceLocation> quests) {
        delegate.setDailyQuests(quests);
        markDirty();
    }
    
    public void setLastDailyRefreshDate(String date) {
        delegate.setLastDailyRefreshDate(date);
        markDirty();
    }
    
    // endregion
    
    // region 只读方法（不需要标记）
    
    public UUID getPlayerId() {
        return delegate.getPlayerId();
    }
    
    public Collection<QuestInstance> getActiveQuests() {
        return delegate.getActiveQuests();
    }
    
    public QuestInstance getActiveQuest(ResourceLocation questId) {
        return delegate.getActiveQuest(questId);
    }
    
    public boolean hasActiveQuest(ResourceLocation questId) {
        return delegate.hasActiveQuest(questId);
    }
    
    public Set<ResourceLocation> getCompletedQuests() {
        return delegate.getCompletedQuests();
    }
    
    public boolean hasCompletedQuest(ResourceLocation questId) {
        return delegate.hasCompletedQuest(questId);
    }
    
    public int getCompletionCount(ResourceLocation questId) {
        return delegate.getCompletionCount(questId);
    }
    
    public Set<ResourceLocation> getActiveQuestIds() {
        return delegate.getActiveQuestIds();
    }
    
    public int getReputationXp(ResourceLocation factionId) {
        return delegate.getReputationXp(factionId);
    }
    
    public int getReputationLevel(ResourceLocation factionId) {
        return delegate.getReputationLevel(factionId);
    }
    
    public Map<ResourceLocation, Integer> getAllReputationXp() {
        return delegate.getAllReputationXp();
    }
    
    public Map<ResourceLocation, Integer> getAllReputationLevels() {
        return delegate.getAllReputationLevels();
    }
    
    public int getTotalQuestsCompleted() {
        return delegate.getTotalQuestsCompleted();
    }
    
    public int getTotalQuestsFailed() {
        return delegate.getTotalQuestsFailed();
    }
    
    public List<ResourceLocation> getDailyQuests() {
        return delegate.getDailyQuests();
    }
    
    public String getLastDailyRefreshDate() {
        return delegate.getLastDailyRefreshDate();
    }
    
    public boolean isOnCooldown(ResourceLocation questId) {
        return delegate.isOnCooldown(questId);
    }
    
    public int getCooldownRemaining(ResourceLocation questId) {
        return delegate.getCooldownRemaining(questId);
    }
    
    // endregion
    
    /**
     * 获取原始委托数据（用于序列化等场景）
     */
    public PlayerQuestData getDelegate() {
        return delegate;
    }
    
    /**
     * 标记数据为脏
     */
    private void markDirty() {
        if (onDirty != null) {
            onDirty.accept(delegate);
        }
    }
}
