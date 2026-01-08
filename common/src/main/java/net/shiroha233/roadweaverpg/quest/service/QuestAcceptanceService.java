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
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;
import net.shiroha233.roadweaverpg.quest.event.QuestEventBus;
import net.shiroha233.roadweaverpg.quest.index.ObjectiveIndex;
import net.shiroha233.roadweaverpg.quest.state.QuestStateMachine;
import net.shiroha233.roadweaverpg.quest.state.StateTransitionLog;
import net.shiroha233.roadweaverpg.quest.sync.IncrementalSyncManager;
import net.shiroha233.roadweaverpg.quest.transaction.QuestTransaction;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托接取服务（优化版）
 * 
 * 改进点：
 * - 使用事务确保原子性
 * - 集成状态机验证状态转移
 * - 发布事件到事件总线
 * - 更新目标索引
 * - 增量同步支持
 */
public class QuestAcceptanceService {
    
    private final QuestDataAccessor dataAccessor;
    private final QuestStateMachine stateMachine;
    private final QuestEventBus eventBus;
    private final ObjectiveIndex objectiveIndex;
    private final IncrementalSyncManager syncManager;
    
    private BiConsumer<ServerPlayer, QuestInstance> onQuestAccepted;
    
    public QuestAcceptanceService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
        this.stateMachine = QuestStateMachine.getInstance();
        this.eventBus = QuestEventBus.getInstance();
        this.objectiveIndex = ObjectiveIndex.getInstance();
        this.syncManager = IncrementalSyncManager.getInstance();
    }
    
    public void setOnQuestAccepted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestAccepted = callback;
    }
    
    /**
     * 接取委托（使用事务确保原子性）
     */
    public Result<QuestInstance> acceptQuest(ServerPlayer player, ResourceLocation questId) {
        // 第一阶段：验证（不修改任何数据）
        Result<AcceptContext> validationResult = validateAcceptance(player, questId);
        if (validationResult.isFailure()) {
            return Result.failure(
                    validationResult.getErrorCode().orElse(QuestException.ErrorCode.UNKNOWN_ERROR),
                    validationResult.getErrorMessage().orElse("Validation failed"));
        }
        
        AcceptContext context = validationResult.getValue().orElseThrow();
        
        // 第二阶段：使用事务执行接取操作
        return executeAcceptTransaction(player, context);
    }
    
    /**
     * 验证接取条件（纯验证，不修改数据）
     */
    private Result<AcceptContext> validateAcceptance(ServerPlayer player, ResourceLocation questId) {
        // 验证委托定义存在
        Result<QuestDefinition> defResult = ValidationUtils.validateDefinitionExists(questId);
        if (defResult.isFailure()) {
            RoadWeaverRPG.LOGGER.debug("Quest definition not found: {}", questId);
            return Result.failure(QuestException.ErrorCode.DEFINITION_NOT_FOUND, "Definition not found");
        }
        QuestDefinition definition = defResult.getValue().orElseThrow();
        
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        
        // 按优先级验证条件（简单条件优先）
        // 1. 验证未持有该委托
        Result<Void> notActiveResult = ValidationUtils.validateNotActiveQuest(playerData, questId);
        if (notActiveResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.QUEST_ALREADY_ACTIVE, "Already active");
        }
        
        // 2. 验证单次任务
        Result<Void> oneTimeResult = ValidationUtils.validateOneTimeQuest(definition, playerData);
        if (oneTimeResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.QUEST_ONE_TIME_COMPLETED, "One-time quest completed");
        }
        
        // 3. 验证可重复性
        Result<Void> repeatResult = ValidationUtils.validateRepeatable(definition, playerData);
        if (repeatResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_REPEATABLE, "Not repeatable");
        }
        
        // 4. 验证冷却
        Result<Void> cooldownResult = ValidationUtils.validateCooldown(playerData, questId);
        if (cooldownResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.QUEST_ON_COOLDOWN, "On cooldown");
        }
        
        // 5. 验证前置条件
        Result<Void> prereqResult = ValidationUtils.validatePrerequisites(questId, playerData);
        if (prereqResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.QUEST_PREREQUISITES_NOT_MET, "Prerequisites not met");
        }
        
        // 6. 验证声望等级
        Result<Void> reputationResult = ValidationUtils.validateReputationLevel(definition, playerData);
        if (reputationResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.REPUTATION_TOO_LOW, "Reputation too low");
        }
        
        // 7. 验证周期内领取次数限制
        Result<Void> acceptLimitResult = ValidationUtils.validateAcceptLimit(definition, playerData);
        if (acceptLimitResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.QUEST_ACCEPT_LIMIT_REACHED, "Accept limit reached");
        }
        
        // 计算动态难度
        float difficulty = calculateDifficulty(player, definition, playerData);
        
        return Result.success(new AcceptContext(definition, playerData, difficulty));
    }
    
    /**
     * 执行接取事务
     */
    private Result<QuestInstance> executeAcceptTransaction(ServerPlayer player, AcceptContext context) {
        QuestDefinition definition = context.definition();
        PlayerQuestData playerData = context.playerData();
        ResourceLocation questId = definition.getId();
        
        // 创建委托实例
        QuestInstance instance = new QuestInstance(questId, player.getUUID(), definition);
        instance.setDifficultyMultiplier(context.difficulty());
        
        // 使用事务执行
        Result<Void> txResult = QuestTransaction.begin(player)
                .withTimeout(5000)
                // 添加到玩家数据
                .execute(() -> {
                    playerData.addActiveQuest(instance);
                    if (definition.hasAcceptLimit()) {
                        playerData.recordAcceptance(questId);
                    }
                }, () -> {
                    playerData.removeActiveQuest(questId);
                    RoadWeaverRPG.LOGGER.debug("Rolled back quest acceptance: {}", questId);
                })
                // 给予委托书
                .execute(() -> giveQuestScroll(player, definition, instance), () -> {
                    removeQuestScroll(player, instance.getInstanceId());
                })
                // 初始化定点击杀资源
                .execute(() -> LocationKillService.getInstance().onQuestAccepted(player, instance, definition))
                // 更新索引
                .execute(() -> objectiveIndex.updateIndex(player.getUUID(), instance, true))
                // 记录状态转移日志
                .execute(() -> StateTransitionLog.getInstance().log(
                        instance.getInstanceId(), questId, QuestState.AVAILABLE, QuestState.IN_PROGRESS, "accepted"))
                // 记录增量同步
                .execute(() -> syncManager.recordQuestAccepted(player.getUUID(), instance))
                .commit();
        
        if (txResult.isFailure()) {
            return Result.failure(
                    txResult.getErrorCode().orElse(QuestException.ErrorCode.TRANSACTION_EXECUTION_FAILED),
                    txResult.getErrorMessage().orElse("Transaction failed"));
        }
        
        // 标记数据需要保存
        dataAccessor.markDirty(player);
        
        // 发布事件
        eventBus.publish(new QuestEvent.QuestAcceptedEvent(player, instance, definition));
        
        // 触发回调
        if (onQuestAccepted != null) {
            onQuestAccepted.accept(player, instance);
        }
        
        RoadWeaverRPG.LOGGER.info("Player {} accepted quest {}", player.getName().getString(), questId);
        return Result.success(instance);
    }
    
    /** 接取上下文（验证阶段的结果） */
    private record AcceptContext(QuestDefinition definition, PlayerQuestData playerData, float difficulty) {}
    
    /**
     * 放弃委托（使用事务和状态机）
     */
    public Result<Void> abandonQuest(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        QuestInstance instance = playerData.getActiveQuest(questId);
        
        if (instance == null) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_ACTIVE, "Quest not active: " + questId);
        }
        
        // 验证状态转移合法性
        QuestState oldState = instance.getState();
        var transitionResult = stateMachine.transition(
                instance.getInstanceId(), oldState, QuestState.ABANDONED, player);
        
        if (transitionResult.isFailure()) {
            return Result.failure(QuestException.ErrorCode.UNKNOWN_ERROR, transitionResult.error());
        }
        
        // 使用事务执行放弃操作
        Result<Void> txResult = QuestTransaction.begin(player)
                .execute(() -> {
                    instance.setState(QuestState.ABANDONED);
                    playerData.removeActiveQuest(questId);
                })
                .execute(() -> removeQuestScroll(player, instance.getInstanceId()))
                .execute(() -> LocationKillService.getInstance().cleanupQuest(player, instance))
                .execute(() -> objectiveIndex.updateIndex(player.getUUID(), instance, false))
                .execute(() -> StateTransitionLog.getInstance().log(
                        instance.getInstanceId(), questId, oldState, QuestState.ABANDONED, "abandoned"))
                .execute(() -> syncManager.recordQuestRemoved(player.getUUID(), questId))
                .commit();
        
        if (txResult.isFailure()) {
            return txResult;
        }
        
        dataAccessor.markDirty(player);
        
        // 发布事件
        eventBus.publish(new QuestEvent.QuestAbandonedEvent(player, instance));
        
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
