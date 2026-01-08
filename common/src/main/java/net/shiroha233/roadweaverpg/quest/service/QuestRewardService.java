package net.shiroha233.roadweaverpg.quest.service;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.common.exception.QuestException;
import net.shiroha233.roadweaverpg.common.result.Result;
import net.shiroha233.roadweaverpg.common.util.ValidationUtils;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.CollectObjective;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;
import net.shiroha233.roadweaverpg.quest.reward.PriorityRewardQueue;
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;
import net.shiroha233.roadweaverpg.quest.event.QuestEventBus;
import net.shiroha233.roadweaverpg.quest.index.ObjectiveIndex;
import net.shiroha233.roadweaverpg.quest.state.StateTransitionLog;
import net.shiroha233.roadweaverpg.quest.sync.IncrementalSyncManager;
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托奖励服务（优化版）
 * 
 * 改进点：
 * - 使用 PriorityRewardQueue 分级处理奖励
 * - 集成事件总线
 * - 增量同步支持
 * - 更完善的事务处理
 */
public class QuestRewardService {
    
    private final QuestDataAccessor dataAccessor;
    private final PriorityRewardQueue rewardQueue;
    private final QuestEventBus eventBus;
    private final IncrementalSyncManager syncManager;
    private final ObjectiveIndex objectiveIndex;
    
    private BiConsumer<ServerPlayer, QuestInstance> onQuestTurnedIn;
    private BiConsumer<ServerPlayer, PlayerQuestData> onSyncReputation;
    
    public QuestRewardService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
        this.rewardQueue = PriorityRewardQueue.getInstance();
        this.eventBus = QuestEventBus.getInstance();
        this.syncManager = IncrementalSyncManager.getInstance();
        this.objectiveIndex = ObjectiveIndex.getInstance();
    }
    
    public void setOnQuestTurnedIn(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestTurnedIn = callback;
    }
    
    public void setOnSyncReputation(BiConsumer<ServerPlayer, PlayerQuestData> callback) {
        this.onSyncReputation = callback;
    }
    
    public Result<Void> turnInQuestByScroll(ServerPlayer player, ItemStack scroll) {
        Result<ResourceLocation> scrollResult = ValidationUtils.validateQuestScroll(scroll);
        if (scrollResult.isFailure()) {
            return Result.failure(scrollResult.getErrorCode().orElse(QuestException.ErrorCode.SCROLL_INVALID),
                    scrollResult.getErrorMessage().orElse("Invalid scroll"));
        }
        
        ResourceLocation questId = scrollResult.getValue().orElseThrow();
        Result<Void> result = turnInQuestInternal(player, questId);
        
        if (result.isSuccess()) {
            scroll.shrink(1);
        }
        return result;
    }
    
    public Result<Void> turnInQuest(ServerPlayer player, ResourceLocation questId) {
        Result<Void> result = turnInQuestInternal(player, questId);
        
        if (result.isSuccess()) {
            PlayerQuestData playerData = dataAccessor.getPlayerData(player);
            QuestInstance instance = playerData.getActiveQuest(questId);
            if (instance != null) {
                removeQuestScroll(player, instance.getInstanceId());
            }
        }
        return result;
    }
    
    private Result<Void> turnInQuestInternal(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        QuestInstance instance = playerData.getActiveQuest(questId);
        
        // 验证
        if (instance == null) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_ACTIVE, "Quest not active");
        }
        if (instance.getState() != QuestState.COMPLETED) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_COMPLETED, "Quest not completed");
        }
        
        QuestDefinition definition = QuestDefinitionLoader.getInstance().getDefinition(questId);
        if (definition == null) {
            return Result.failure(QuestException.ErrorCode.DEFINITION_NOT_FOUND, "Definition not found");
        }
        
        QuestState oldState = instance.getState();
        
        // 使用事务执行提交操作
        Result<Void> txResult = net.shiroha233.roadweaverpg.quest.transaction.QuestTransaction.begin(player)
                // 消耗收集物品
                .execute(() -> consumeCollectItems(player, definition), () -> {
                    RoadWeaverRPG.LOGGER.warn("Cannot rollback consumed items for player {}", player.getName().getString());
                })
                // 发放奖励（使用优先级队列）
                .execute(() -> grantRewards(player, definition, instance.getDifficultyMultiplier()))
                // 更新状态
                .execute(() -> {
                    instance.setState(QuestState.TURNED_IN);
                    playerData.removeActiveQuest(questId);
                    playerData.markQuestCompleted(questId);
                    
                    if (definition.isRepeatable() && definition.getCooldown() > 0) {
                        playerData.setCooldown(questId, definition.getCooldown());
                    }
                })
                // 更新索引
                .execute(() -> objectiveIndex.updateIndex(player.getUUID(), instance, false))
                // 记录状态转移
                .execute(() -> StateTransitionLog.getInstance().log(
                        instance.getInstanceId(), questId, oldState, QuestState.TURNED_IN, "turned_in"))
                // 记录增量同步
                .execute(() -> syncManager.recordQuestRemoved(player.getUUID(), questId))
                .commit();
        
        if (txResult.isFailure()) {
            return txResult;
        }
        
        dataAccessor.markDirty(player);
        
        // 发布事件
        eventBus.publish(new QuestEvent.QuestTurnedInEvent(player, questId, definition));
        
        // 触发回调
        if (onQuestTurnedIn != null) {
            onQuestTurnedIn.accept(player, instance);
        }
        
        RoadWeaverRPG.LOGGER.info("Player {} turned in quest {}", player.getName().getString(), questId);
        return Result.success(null);
    }
    
    private void consumeCollectItems(ServerPlayer player, QuestDefinition definition) {
        for (QuestObjective obj : definition.getObjectives()) {
            if (obj instanceof CollectObjective collectObj && collectObj.shouldConsumeOnComplete()) {
                Optional<Item> targetItem = RegistryHelper.getItem(collectObj.getTargetResource());
                if (targetItem.isEmpty()) continue;
                
                int totalToConsume = collectObj.getRequiredAmount();
                int consumed = 0;
                
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.getItem() == targetItem.get() && consumed < totalToConsume) {
                        int canConsume = Math.min(stack.getCount(), totalToConsume - consumed);
                        stack.shrink(canConsume);
                        consumed += canConsume;
                        if (consumed >= totalToConsume) break;
                    }
                }
            }
        }
    }
    
    /**
     * 发放奖励（使用优先级队列）
     */
    private void grantRewards(ServerPlayer player, QuestDefinition definition, float difficultyMod) {
        ResourceLocation questId = definition.getId();
        
        for (QuestReward reward : definition.getRewards()) {
            // 确定优先级
            PriorityRewardQueue.RewardPriority priority = determinePriority(reward);
            
            // 先尝试直接发放
            if (tryGrantRewardDirect(player, reward, questId)) {
                // 发布奖励事件
                eventBus.publish(new QuestEvent.RewardGrantedEvent(player, questId, reward.getType().name(), reward));
                continue;
            }
            
            // 直接发放失败，加入优先级队列
            if (!rewardQueue.enqueue(player.getUUID(), questId, reward, priority)) {
                RoadWeaverRPG.LOGGER.debug("Reward already in queue: quest={}, type={}", questId, reward.getType());
            }
        }
        
        // 处理队列中的待发放奖励
        int granted = rewardQueue.processPlayerRewards(player);
        if (granted > 0) {
            RoadWeaverRPG.LOGGER.debug("Processed {} queued rewards for player {}", granted, player.getName().getString());
        }
    }
    
    /**
     * 确定奖励优先级
     */
    private PriorityRewardQueue.RewardPriority determinePriority(QuestReward reward) {
        return switch (reward.getType()) {
            case ITEM -> PriorityRewardQueue.RewardPriority.HIGH;
            case EXPERIENCE -> PriorityRewardQueue.RewardPriority.LOW;
            case COIN -> PriorityRewardQueue.RewardPriority.NORMAL;
            case REPUTATION -> PriorityRewardQueue.RewardPriority.NORMAL;
            case UNLOCK_QUEST -> PriorityRewardQueue.RewardPriority.NORMAL;
            case COMMAND -> PriorityRewardQueue.RewardPriority.NORMAL;
        };
    }
    
    /**
     * 尝试直接发放奖励（不经过队列）
     */
    private boolean tryGrantRewardDirect(ServerPlayer player, QuestReward reward, ResourceLocation questId) {
        try {
            if (!reward.canGrant(player)) {
                RoadWeaverRPG.LOGGER.debug("Cannot grant reward {} to player {}: condition not met",
                        reward.getType(), player.getName().getString());
                return false;
            }
            
            reward.grant(player);
            RoadWeaverRPG.LOGGER.debug("Reward granted directly: player={}, quest={}, type={}",
                    player.getName().getString(), questId, reward.getType());
            return true;
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.warn("Direct reward grant failed, will queue for retry: {}",
                    e.getMessage());
            return false;
        }
    }
    
    private void removeQuestScroll(ServerPlayer player, UUID instanceId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem) {
                Optional<UUID> scrollInstanceId = QuestScrollItem.getInstanceId(stack);
                if (scrollInstanceId.isPresent() && scrollInstanceId.get().equals(instanceId)) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }
    
    public void addReputationXp(ServerPlayer player, ResourceLocation factionId, int amount) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        data.addReputationXp(factionId, amount);
        checkReputationLevelUp(player, data, factionId);
        dataAccessor.markDirty(player);
        syncReputationToClient(player);
        
        String sign = amount >= 0 ? "+" : "";
        player.sendSystemMessage(Component.translatable("message.roadweaver_rpg.reputation_gained", 
                sign + amount, factionId.getPath()));
    }
    
    private void checkReputationLevelUp(ServerPlayer player, PlayerQuestData data, ResourceLocation factionId) {
        ReputationManager manager = ReputationManager.getInstance();
        if (manager == null) return;
        
        int currentXp = data.getReputationXp(factionId);
        int currentLevel = data.getReputationLevel(factionId);
        int newLevel = manager.getLevelForExperience(currentXp);
        
        if (newLevel > currentLevel) {
            for (int i = currentLevel + 1; i <= newLevel; i++) {
                ReputationLevel levelInfo = manager.getLevelInfo(i);
                if (levelInfo != null) {
                    for (QuestReward reward : levelInfo.getRewards()) {
                        try {
                            if (reward.canGrant(player)) {
                                reward.grant(player);
                            }
                        } catch (Exception e) {
                            RoadWeaverRPG.LOGGER.error("Failed to grant reputation level reward", e);
                        }
                    }
                    player.sendSystemMessage(Component.translatable(
                            "message.roadweaver_rpg.reputation_level_up", factionId.toString(), i));
                }
            }
            data.setReputationLevel(factionId, newLevel);
            dataAccessor.markDirty(player);
        }
    }
    
    public void syncReputationToClient(ServerPlayer player) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        if (onSyncReputation != null) {
            onSyncReputation.accept(player, data);
        }
    }
}
