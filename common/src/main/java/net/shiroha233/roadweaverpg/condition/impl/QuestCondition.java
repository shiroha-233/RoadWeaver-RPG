package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;

/**
 * 委托相关条件判定
 */
public final class QuestCondition implements PlayerCondition<ConditionContext> {
    
    public enum QuestCheckType {
        COMPLETED,
        ACTIVE,
        COMPLETION_COUNT,
        REPUTATION_LEVEL,
        REPUTATION_XP
    }
    
    private final QuestCheckType type;
    private final ResourceLocation questId;
    private final ResourceLocation factionId;
    private final int value;
    
    private QuestCondition(QuestCheckType type, ResourceLocation questId, 
                           ResourceLocation factionId, int value) {
        this.type = type;
        this.questId = questId;
        this.factionId = factionId;
        this.value = value;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        
        return switch (type) {
            case COMPLETED -> data.hasCompletedQuest(questId);
            case ACTIVE -> data.hasActiveQuest(questId);
            case COMPLETION_COUNT -> data.getCompletionCount(questId) >= value;
            case REPUTATION_LEVEL -> data.getReputationLevel(factionId) >= value;
            case REPUTATION_XP -> data.getReputationXp(factionId) >= value;
        };
    }
    
    // 工厂方法
    public static QuestCondition completed(String questId) {
        return new QuestCondition(QuestCheckType.COMPLETED, new ResourceLocation(questId), null, 0);
    }
    
    public static QuestCondition active(String questId) {
        return new QuestCondition(QuestCheckType.ACTIVE, new ResourceLocation(questId), null, 0);
    }
    
    public static QuestCondition completionCount(String questId, int count) {
        return new QuestCondition(QuestCheckType.COMPLETION_COUNT, new ResourceLocation(questId), null, count);
    }
    
    public static QuestCondition reputationLevel(String factionId, int level) {
        return new QuestCondition(QuestCheckType.REPUTATION_LEVEL, null, new ResourceLocation(factionId), level);
    }
    
    public static QuestCondition reputationXp(String factionId, int xp) {
        return new QuestCondition(QuestCheckType.REPUTATION_XP, null, new ResourceLocation(factionId), xp);
    }
    
    public QuestCheckType getType() {
        return type;
    }
}
