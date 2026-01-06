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
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托奖励服务
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
        
        Result<QuestInstance> instanceResult = ValidationUtils.validateHasActiveQuest(playerData, questId);
        if (instanceResult.isFailure()) {
            return Result.failure(instanceResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_NOT_ACTIVE),
                    instanceResult.getErrorMessage().orElse("Not active"));
        }
        QuestInstance instance = instanceResult.getValue().orElseThrow();
        
        Result<QuestInstance> stateResult = ValidationUtils.validateQuestState(instance, QuestState.COMPLETED);
        if (stateResult.isFailure()) {
            return Result.failure(stateResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_NOT_COMPLETED),
                    stateResult.getErrorMessage().orElse("Not completed"));
        }
        
        Result<QuestDefinition> defResult = ValidationUtils.validateDefinitionExists(questId);
        if (defResult.isFailure()) {
            return Result.failure(defResult.getErrorCode().orElse(QuestException.ErrorCode.DEFINITION_NOT_FOUND),
                    defResult.getErrorMessage().orElse("Definition not found"));
        }
        QuestDefinition definition = defResult.getValue().orElseThrow();
        
        consumeCollectItems(player, definition);
        grantRewards(player, definition, instance.getDifficultyMultiplier());
        
        instance.setState(QuestState.TURNED_IN);
        playerData.removeActiveQuest(questId);
        playerData.markQuestCompleted(questId);
        
        if (definition.isRepeatable() && definition.getCooldown() > 0) {
            playerData.setCooldown(questId, definition.getCooldown());
        }
        
        dataAccessor.markDirty(player);
        
        if (onQuestTurnedIn != null) {
            onQuestTurnedIn.accept(player, instance);
        }
        
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
    
    private void grantRewards(ServerPlayer player, QuestDefinition definition, float difficultyMod) {
        for (QuestReward reward : definition.getRewards()) {
            try {
                if (reward.canGrant(player)) {
                    reward.grant(player);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to grant reward {} to player {}", 
                        reward.getType(), player.getName().getString(), e);
            }
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
