package net.shiroha233.roadweaverpg.quest.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.type.QuestState;

import java.util.UUID;

/**
 * 委托事件基类
 * 
 * 设计原理：
 * - 所有委托相关事件的基类
 * - 支持事件取消
 * - 携带通用上下文信息
 */
public abstract class QuestEvent {
    
    private final long timestamp;
    private final UUID eventId;
    private boolean cancelled = false;
    
    protected QuestEvent() {
        this.timestamp = System.currentTimeMillis();
        this.eventId = UUID.randomUUID();
    }
    
    public long getTimestamp() { return timestamp; }
    public UUID getEventId() { return eventId; }
    
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    // ==================== 具体事件类型 ====================
    
    /** 委托接取事件 */
    public static class QuestAcceptedEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        private final QuestDefinition definition;
        
        public QuestAcceptedEvent(ServerPlayer player, QuestInstance instance, QuestDefinition definition) {
            this.player = player;
            this.instance = instance;
            this.definition = definition;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
        public QuestDefinition getDefinition() { return definition; }
    }
    
    /** 委托进度更新事件 */
    public static class QuestProgressEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        private final String objectiveId;
        private final int oldProgress;
        private final int newProgress;
        
        public QuestProgressEvent(ServerPlayer player, QuestInstance instance, 
                                  String objectiveId, int oldProgress, int newProgress) {
            this.player = player;
            this.instance = instance;
            this.objectiveId = objectiveId;
            this.oldProgress = oldProgress;
            this.newProgress = newProgress;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
        public String getObjectiveId() { return objectiveId; }
        public int getOldProgress() { return oldProgress; }
        public int getNewProgress() { return newProgress; }
        public int getProgressDelta() { return newProgress - oldProgress; }
    }
    
    /** 委托完成事件（所有目标完成） */
    public static class QuestCompletedEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        private final QuestDefinition definition;
        
        public QuestCompletedEvent(ServerPlayer player, QuestInstance instance, QuestDefinition definition) {
            this.player = player;
            this.instance = instance;
            this.definition = definition;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
        public QuestDefinition getDefinition() { return definition; }
    }
    
    /** 委托提交事件（领取奖励） */
    public static class QuestTurnedInEvent extends QuestEvent {
        private final ServerPlayer player;
        private final ResourceLocation questId;
        private final QuestDefinition definition;
        
        public QuestTurnedInEvent(ServerPlayer player, ResourceLocation questId, QuestDefinition definition) {
            this.player = player;
            this.questId = questId;
            this.definition = definition;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public ResourceLocation getQuestId() { return questId; }
        public QuestDefinition getDefinition() { return definition; }
    }
    
    /** 委托放弃事件 */
    public static class QuestAbandonedEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        
        public QuestAbandonedEvent(ServerPlayer player, QuestInstance instance) {
            this.player = player;
            this.instance = instance;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
    }
    
    /** 委托过期事件 */
    public static class QuestExpiredEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        
        public QuestExpiredEvent(ServerPlayer player, QuestInstance instance) {
            this.player = player;
            this.instance = instance;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
    }
    
    /** 委托失败事件 */
    public static class QuestFailedEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        private final String reason;
        
        public QuestFailedEvent(ServerPlayer player, QuestInstance instance, String reason) {
            this.player = player;
            this.instance = instance;
            this.reason = reason;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
        public String getReason() { return reason; }
    }
    
    /** 委托状态变更事件 */
    public static class QuestStateChangedEvent extends QuestEvent {
        private final ServerPlayer player;
        private final QuestInstance instance;
        private final QuestState oldState;
        private final QuestState newState;
        
        public QuestStateChangedEvent(ServerPlayer player, QuestInstance instance, 
                                      QuestState oldState, QuestState newState) {
            this.player = player;
            this.instance = instance;
            this.oldState = oldState;
            this.newState = newState;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public QuestInstance getInstance() { return instance; }
        public QuestState getOldState() { return oldState; }
        public QuestState getNewState() { return newState; }
    }
    
    /** 奖励发放事件 */
    public static class RewardGrantedEvent extends QuestEvent {
        private final ServerPlayer player;
        private final ResourceLocation questId;
        private final String rewardType;
        private final Object rewardData;
        
        public RewardGrantedEvent(ServerPlayer player, ResourceLocation questId, 
                                  String rewardType, Object rewardData) {
            this.player = player;
            this.questId = questId;
            this.rewardType = rewardType;
            this.rewardData = rewardData;
        }
        
        public ServerPlayer getPlayer() { return player; }
        public ResourceLocation getQuestId() { return questId; }
        public String getRewardType() { return rewardType; }
        public Object getRewardData() { return rewardData; }
    }
}
