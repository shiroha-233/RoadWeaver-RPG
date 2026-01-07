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
import net.shiroha233.roadweaverpg.quest.reward.RewardQueueManager;
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托奖励服务
 * 
 * 改进点：
 * - 使用RewardQueueManager确保奖励不丢失
 * - 去重机制防止重复发放
 * - 指数退避重试机制
 */
public class QuestRewardService {
    
    private final QuestDataAccessor dataAccessor;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestTurnedIn;
    private BiConsumer<ServerPlayer, PlayerQuestData> onSyncReputation;
    
    public QuestRewardService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
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
        
        // 使用事务确保原子性
        return net.shiroha233.roadweaverpg.quest.transaction.QuestTransaction.begin(player)
                // 验证步骤
                .validate(() -> ValidationUtils.validateHasActiveQuest(playerData, questId)
                        .map(instance -> null))
                .validate(() -> {
                    QuestInstance instance = playerData.getActiveQuest(questId);
                    return ValidationUtils.validateQuestState(instance, QuestState.COMPLETED)
                            .map(i -> null);
                })
                .validate(() -> ValidationUtils.validateDefinitionExists(questId)
                        .map(def -> null))
                
                // 执行步骤（带回滚）
                .execute(() -> {
                    QuestDefinition definition = QuestDefinitionLoader.getInstance().getDefinition(questId);
                    
                    // 消耗物品
                    consumeCollectItems(player, definition);
                }, () -> {
                    // 回滚：恢复物品（实际上很难完美回滚，所以先验证再执行）
                    RoadWeaverRPG.LOGGER.warn("Rollback consume items for player {}", player.getName().getString());
                })
                
                .execute(() -> {
                    QuestInstance instance = playerData.getActiveQuest(questId);
                    QuestDefinition definition = QuestDefinitionLoader.getInstance().getDefinition(questId);
                    
                    // 发放奖励
                    grantRewards(player, definition, instance != null ? instance.getDifficultyMultiplier() : 1.0f);
                }, () -> {
                    // 回滚：移除奖励（实际上很难完美回滚）
                    RoadWeaverRPG.LOGGER.warn("Rollback grant rewards for player {}", player.getName().getString());
                })
                
                .execute(() -> {
                    QuestInstance instance = playerData.getActiveQuest(questId);
                    QuestDefinition definition = QuestDefinitionLoader.getInstance().getDefinition(questId);
                    
                    // 更新状态
                    instance.setState(QuestState.TURNED_IN);
                    playerData.removeActiveQuest(questId);
                    playerData.markQuestCompleted(questId);
                    
                    if (definition.isRepeatable() && definition.getCooldown() > 0) {
                        playerData.setCooldown(questId, definition.getCooldown());
                    }
                    
                    dataAccessor.markDirty(player);
                }, () -> {
                    // 回滚：恢复状态
                    QuestInstance instance = playerData.getActiveQuest(questId);
                    if (instance != null) {
                        instance.setState(QuestState.COMPLETED);
                    }
                })
                
                .execute(() -> {
                    // 触发回调
                    if (onQuestTurnedIn != null) {
                        QuestInstance instance = playerData.getActiveQuest(questId);
                        if (instance != null) {
                            onQuestTurnedIn.accept(player, instance);
                        }
                    }
                })
                
                .commit();
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
     * 发放奖励（使用队列系统）
     * 
     * 改进原理：
     * - 使用RewardQueueManager管理奖励发放
     * - 去重机制防止重复发放
     * - 失败的奖励进入重试队列，指数退避重试
     * - 确保奖励不丢失
     */
    private void grantRewards(ServerPlayer player, QuestDefinition definition, float difficultyMod) {
        RewardQueueManager queueManager = RewardQueueManager.getInstance();
        ResourceLocation questId = definition.getId();
        
        for (QuestReward reward : definition.getRewards()) {
            // 先尝试直接发放
            if (tryGrantRewardDirect(player, reward, questId)) {
                continue;
            }
            
            // 直接发放失败，加入队列等待重试
            if (!queueManager.enqueue(player.getUUID(), questId, reward)) {
                RoadWeaverRPG.LOGGER.debug("Reward already in queue or history: quest={}, type={}",
                        questId, reward.getType());
            }
        }
        
        // 处理队列中的待发放奖励
        int granted = queueManager.processPlayerRewards(player);
        if (granted > 0) {
            RoadWeaverRPG.LOGGER.debug("Processed {} queued rewards for player {}",
                    granted, player.getName().getString());
        }
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
