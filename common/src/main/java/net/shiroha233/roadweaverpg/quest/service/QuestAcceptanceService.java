package net.shiroha233.roadweaverpg.quest.service;

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
import net.shiroha233.roadweaverpg.item.ModItems;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托接取服务
 * 负责委托的接取和放弃逻辑
 */
public class QuestAcceptanceService {
    
    private final QuestDataAccessor dataAccessor;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestAccepted;
    
    public QuestAcceptanceService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }
    
    public void setOnQuestAccepted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestAccepted = callback;
    }
    
    public Result<QuestInstance> acceptQuest(ServerPlayer player, ResourceLocation questId) {
        // 验证委托定义存在
        Result<QuestDefinition> defResult = ValidationUtils.validateDefinitionExists(questId);
        if (defResult.isFailure()) {
            RoadWeaverRPG.LOGGER.error("Failed to accept quest: definition {} not found", questId);
            return Result.failure(defResult.getErrorCode().orElse(QuestException.ErrorCode.DEFINITION_NOT_FOUND),
                    defResult.getErrorMessage().orElse("Definition not found"));
        }
        QuestDefinition definition = defResult.getValue().orElseThrow();
        
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        
        // 验证未持有该委托
        Result<Void> notActiveResult = ValidationUtils.validateNotActiveQuest(playerData, questId);
        if (notActiveResult.isFailure()) {
            return Result.failure(notActiveResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_ALREADY_ACTIVE),
                    notActiveResult.getErrorMessage().orElse("Already active"));
        }
        
        // 验证前置条件
        Result<Void> prereqResult = ValidationUtils.validatePrerequisites(questId, playerData);
        if (prereqResult.isFailure()) {
            return Result.failure(prereqResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_PREREQUISITES_NOT_MET),
                    prereqResult.getErrorMessage().orElse("Prerequisites not met"));
        }
        
        // 验证冷却
        Result<Void> cooldownResult = ValidationUtils.validateCooldown(playerData, questId);
        if (cooldownResult.isFailure()) {
            return Result.failure(cooldownResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_ON_COOLDOWN),
                    cooldownResult.getErrorMessage().orElse("On cooldown"));
        }

        // 验证声望等级
        Result<Void> reputationResult = ValidationUtils.validateReputationLevel(definition, playerData);
        if (reputationResult.isFailure()) {
            return Result.failure(reputationResult.getErrorCode().orElse(QuestException.ErrorCode.REPUTATION_TOO_LOW),
                    reputationResult.getErrorMessage().orElse("Reputation too low"));
        }
        
        // 验证可重复性
        Result<Void> repeatResult = ValidationUtils.validateRepeatable(definition, playerData);
        if (repeatResult.isFailure()) {
            return Result.failure(repeatResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_NOT_REPEATABLE),
                    repeatResult.getErrorMessage().orElse("Not repeatable"));
        }
        
        // 验证单次任务
        Result<Void> oneTimeResult = ValidationUtils.validateOneTimeQuest(definition, playerData);
        if (oneTimeResult.isFailure()) {
            return Result.failure(oneTimeResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_ONE_TIME_COMPLETED),
                    oneTimeResult.getErrorMessage().orElse("One-time quest already completed"));
        }
        
        // 验证周期内领取次数限制
        Result<Void> acceptLimitResult = ValidationUtils.validateAcceptLimit(definition, playerData);
        if (acceptLimitResult.isFailure()) {
            return Result.failure(acceptLimitResult.getErrorCode().orElse(QuestException.ErrorCode.QUEST_ACCEPT_LIMIT_REACHED),
                    acceptLimitResult.getErrorMessage().orElse("Accept limit reached"));
        }
        
        // 创建委托实例
        QuestInstance instance = new QuestInstance(questId, player.getUUID(), definition);
        
        // 计算动态难度
        float difficulty = calculateDifficulty(player, definition, playerData);
        instance.setDifficultyMultiplier(difficulty);
        
        // 添加到玩家数据
        playerData.addActiveQuest(instance);
        
        // 记录领取时间（用于周期限制）
        if (definition.hasAcceptLimit()) {
            playerData.recordAcceptance(questId);
        }
        
        dataAccessor.markDirty(player);
        
        // 给予委托书
        giveQuestScroll(player, definition, instance);
        
        // 触发回调
        if (onQuestAccepted != null) {
            onQuestAccepted.accept(player, instance);
        }
        
        RoadWeaverRPG.LOGGER.info("Player {} accepted quest {}", player.getName().getString(), questId);
        return Result.success(instance);
    }
    
    public Result<Void> abandonQuest(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        QuestInstance instance = playerData.removeActiveQuest(questId);
        
        if (instance == null) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_ACTIVE,
                    "Quest not active: " + questId);
        }
        
        instance.setState(QuestState.ABANDONED);
        removeQuestScroll(player, instance.getInstanceId());
        dataAccessor.markDirty(player);
        
        RoadWeaverRPG.LOGGER.info("Player {} abandoned quest {}", player.getName().getString(), questId);
        return Result.success(null);
    }
    
    private void giveQuestScroll(ServerPlayer player, QuestDefinition definition, QuestInstance instance) {
        if (ModItems.QUEST_SCROLL == null) {
            RoadWeaverRPG.LOGGER.error("Quest scroll supplier not initialized!");
            return;
        }
        
        Item scrollItem = ModItems.QUEST_SCROLL.get();
        if (scrollItem == null) {
            RoadWeaverRPG.LOGGER.error("Quest scroll item is null!");
            return;
        }
        
        ItemStack scroll = QuestScrollItem.createWithQuest(scrollItem, definition, instance);
        if (scroll.isEmpty()) {
            RoadWeaverRPG.LOGGER.error("Failed to create quest scroll ItemStack!");
            return;
        }
        
        if (!player.getInventory().add(scroll)) {
            player.drop(scroll, false);
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
    
    private float calculateDifficulty(ServerPlayer player, QuestDefinition definition, PlayerQuestData data) {
        float base = definition.getBaseDifficulty();
        int playerLevel = player.experienceLevel;
        float levelMod = 1.0f + (playerLevel * net.shiroha233.roadweaverpg.config.QuestSystemConfig.DIFFICULTY_PER_LEVEL);
        int completions = data.getCompletionCount(definition.getId());
        float repeatMod = 1.0f + (completions * net.shiroha233.roadweaverpg.config.QuestSystemConfig.DIFFICULTY_PER_REPEAT);
        return base * levelMod * repeatMod;
    }
}
