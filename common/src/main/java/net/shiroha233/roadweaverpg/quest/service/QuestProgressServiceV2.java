package net.shiroha233.roadweaverpg.quest.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;
import net.shiroha233.roadweaverpg.quest.event.QuestEventBus;
import net.shiroha233.roadweaverpg.quest.index.ObjectiveIndex;
import net.shiroha233.roadweaverpg.quest.instance.ObjectiveProgress;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.ObjectiveProgressChecker;
import net.shiroha233.roadweaverpg.quest.objective.ObjectiveProgressResult;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;
import net.shiroha233.roadweaverpg.quest.state.StateTransitionLog;
import net.shiroha233.roadweaverpg.quest.sync.IncrementalSyncManager;
import net.shiroha233.roadweaverpg.quest.type.QuestState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 委托进度服务 V2（重构版）
 * 
 * 重构改进：
 * 1. 使用 ObjectiveProgressChecker 统一进度检查逻辑
 * 2. 使用 ObjectiveProgressResult 替代 int 返回值
 * 3. 条件评估与进度计算完全分离
 * 4. 批量更新优化，减少同步次数
 * 5. 更精确的进度显示去重
 */
public class QuestProgressServiceV2 {
    
    private final QuestDataAccessor dataAccessor;
    private final ObjectiveIndex objectiveIndex;
    private final QuestEventBus eventBus;
    private final IncrementalSyncManager syncManager;
    
    // 收集类目标缓存（玩家ID -> 目标ID -> 进度值）
    private final Map<UUID, Map<String, Integer>> collectCache = new ConcurrentHashMap<>();
    
    // 进度显示去重（玩家ID -> 目标ID -> 上次显示时间）
    private final Map<UUID, Map<String, Long>> lastProgressShowTime = new ConcurrentHashMap<>();
    private static final long PROGRESS_SHOW_COOLDOWN_MS = 100L;
    
    // 回调
    private BiConsumer<ServerPlayer, QuestInstance> onQuestUpdated;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestCompleted;
    
    public QuestProgressServiceV2(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
        this.objectiveIndex = ObjectiveIndex.getInstance();
        this.eventBus = QuestEventBus.getInstance();
        this.syncManager = IncrementalSyncManager.getInstance();
    }
    
    public void setOnQuestUpdated(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestUpdated = callback;
    }
    
    public void setOnQuestCompleted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestCompleted = callback;
    }
    
    /**
     * 更新进度（主入口）
     */
    public void updateProgress(ServerPlayer player, String eventType, Object eventData) {
        UUID playerId = player.getUUID();
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        
        // 查询相关目标
        List<ObjectiveIndex.IndexEntry> entries = queryRelevantObjectives(playerId, eventType, playerData);
        if (entries.isEmpty()) {
            return;
        }
        
        // 收集进度更新
        List<ProgressUpdateRecord> updates = new ArrayList<>();
        
        for (ObjectiveIndex.IndexEntry entry : entries) {
            QuestInstance instance = playerData.getActiveQuest(entry.questId());
            if (instance == null || instance.getState() != QuestState.IN_PROGRESS) {
                continue;
            }
            
            // 检查过期
            if (checkAndHandleExpiration(player, instance, playerData)) {
                continue;
            }
            
            // 获取目标定义
            Optional<QuestObjective> objectiveOpt = objectiveIndex.getCachedObjective(entry.cacheKey());
            if (objectiveOpt.isEmpty()) {
                continue;
            }
            
            QuestObjective objective = objectiveOpt.get();
            ObjectiveProgress progress = instance.getObjectiveProgress(entry.objectiveId());
            if (progress == null || progress.isCompleted()) {
                continue;
            }
            
            // 使用统一检查器检查进度
            ObjectiveProgressResult result = ObjectiveProgressChecker.check(
                    objective, player, eventType, eventData);
            
            // 处理检查结果
            if (!result.shouldSkip()) {
                int oldProgress = progress.getCurrentProgress();
                int newProgress = calculateNewProgress(result, oldProgress, objective);
                
                if (newProgress != oldProgress) {
                    updates.add(new ProgressUpdateRecord(
                            instance, entry.objectiveId(), objective, oldProgress, newProgress));
                }
            }
        }
        
        // 批量应用更新
        if (!updates.isEmpty()) {
            applyProgressUpdates(player, playerData, updates);
        }
    }
    
    /**
     * 检查收集类目标（专用方法）
     */
    public void checkCollectObjectives(ServerPlayer player) {
        // 清除收集缓存，强制重新计算
        collectCache.remove(player.getUUID());
        updateProgress(player, "inventory_check", null);
    }
    
    /**
     * 使收集缓存失效
     */
    public void invalidateCollectCache(UUID playerId) {
        collectCache.remove(playerId);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 查询相关目标（带索引重建）
     */
    private List<ObjectiveIndex.IndexEntry> queryRelevantObjectives(UUID playerId, 
                                                                     String eventType,
                                                                     PlayerQuestData playerData) {
        List<ObjectiveIndex.IndexEntry> entries = objectiveIndex.query(playerId, eventType);
        
        // 索引为空但有活跃任务，尝试重建
        if (entries.isEmpty() && !playerData.getActiveQuests().isEmpty()) {
            objectiveIndex.rebuildIndex(playerId, playerData.getActiveQuests(), playerData.getVersion());
            entries = objectiveIndex.query(playerId, eventType);
        }
        
        return entries;
    }
    
    /**
     * 检查并处理过期
     */
    private boolean checkAndHandleExpiration(ServerPlayer player, 
                                              QuestInstance instance,
                                              PlayerQuestData playerData) {
        if (!instance.isExpired()) {
            return false;
        }
        
        QuestState oldState = instance.getState();
        instance.setState(QuestState.EXPIRED);
        playerData.markQuestFailed(instance.getQuestId());
        updateScrollState(player, instance);
        
        // 记录状态转移
        StateTransitionLog.getInstance().log(
                instance.getInstanceId(), instance.getQuestId(), 
                oldState, QuestState.EXPIRED, "timeout");
        
        // 发布事件
        eventBus.publish(new QuestEvent.QuestExpiredEvent(player, instance));
        
        // 记录增量同步
        syncManager.recordStateChange(player.getUUID(), instance.getQuestId(), 
                oldState, QuestState.EXPIRED);
        
        RoadWeaverRPG.LOGGER.info("Quest {} expired for player {}", 
                instance.getQuestId(), player.getName().getString());
        
        return true;
    }
    
    /**
     * 计算新进度值
     */
    private int calculateNewProgress(ObjectiveProgressResult result, 
                                      int oldProgress,
                                      QuestObjective objective) {
        if (result instanceof ObjectiveProgressResult.Progress p) {
            if (p.isAbsolute()) {
                // 绝对值（收集类）
                return Math.min(p.progress(), objective.getRequiredAmount());
            } else {
                // 累加值（击杀类）
                return Math.min(oldProgress + p.progress(), objective.getRequiredAmount());
            }
        }
        return oldProgress;
    }
    
    /**
     * 批量应用进度更新
     */
    private void applyProgressUpdates(ServerPlayer player, 
                                       PlayerQuestData playerData,
                                       List<ProgressUpdateRecord> updates) {
        Set<QuestInstance> updatedInstances = new HashSet<>();
        
        for (ProgressUpdateRecord update : updates) {
            QuestInstance instance = update.instance();
            ObjectiveProgress progress = instance.getObjectiveProgress(update.objectiveId());
            if (progress == null) {
                continue;
            }
            
            // 应用进度
            progress.setProgress(update.newProgress());
            
            // 发布进度事件
            eventBus.publish(new QuestEvent.QuestProgressEvent(
                    player, instance, update.objectiveId(), 
                    update.oldProgress(), update.newProgress()));
            
            // 记录增量同步
            syncManager.recordProgressChange(player.getUUID(), instance.getQuestId(),
                    update.objectiveId(), update.oldProgress(), update.newProgress());
            
            // 显示进度更新
            showProgressUpdate(player, update.objective(), update.newProgress());
            
            updatedInstances.add(instance);
            
            // 检查目标是否完成
            if (progress.isCompleted()) {
                objectiveIndex.markObjectiveComplete(
                        player.getUUID(), instance.getInstanceId(), update.objectiveId());
            }
        }
        
        // 处理状态变更
        for (QuestInstance instance : updatedInstances) {
            checkAndHandleCompletion(player, instance, playerData);
        }
        
        if (!updatedInstances.isEmpty()) {
            dataAccessor.markDirty(player);
        }
    }
    
    /**
     * 检查并处理委托完成
     */
    private void checkAndHandleCompletion(ServerPlayer player, 
                                           QuestInstance instance,
                                           PlayerQuestData playerData) {
        // 检查是否所有目标都完成
        if (!instance.areAllObjectivesComplete()) {
            // 未完成，触发更新回调
            if (onQuestUpdated != null) {
                onQuestUpdated.accept(player, instance);
            }
            return;
        }
        
        // 状态已经是COMPLETED，跳过
        if (instance.getState() == QuestState.COMPLETED) {
            return;
        }
        
        // 标记为完成
        QuestState oldState = instance.getState();
        instance.setState(QuestState.COMPLETED);
        
        // 更新委托书状态
        updateScrollState(player, instance);
        
        // 获取定义并显示完成提示
        QuestDefinition definition = QuestDefinitionLoader.getInstance()
                .getDefinition(instance.getQuestId());
        if (definition != null) {
            showQuestCompleted(player, definition);
        }
        
        // 记录状态转移
        StateTransitionLog.getInstance().log(
                instance.getInstanceId(), instance.getQuestId(),
                oldState, QuestState.COMPLETED, "all_objectives_complete");
        
        // 发布完成事件
        eventBus.publish(new QuestEvent.QuestCompletedEvent(player, instance, definition));
        
        // 记录增量同步
        syncManager.recordStateChange(player.getUUID(), instance.getQuestId(),
                oldState, QuestState.COMPLETED);
        
        // 触发完成回调
        if (onQuestCompleted != null) {
            onQuestCompleted.accept(player, instance);
        }
        
        RoadWeaverRPG.LOGGER.info("Player {} completed quest {}", 
                player.getName().getString(), instance.getQuestId());
    }
    
    /**
     * 显示进度更新（带去重）
     */
    private void showProgressUpdate(ServerPlayer player, QuestObjective objective, int currentProgress) {
        UUID playerId = player.getUUID();
        String objectiveKey = objective.getId();
        long now = System.currentTimeMillis();
        
        // 检查冷却
        Map<String, Long> playerShowTimes = lastProgressShowTime
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Long lastShow = playerShowTimes.get(objectiveKey);
        
        if (lastShow != null && (now - lastShow) < PROGRESS_SHOW_COOLDOWN_MS) {
            return;
        }
        
        playerShowTimes.put(objectiveKey, now);
        
        // 构建消息
        Component message = objective.getDescription().copy()
                .append(Component.literal(" (" + currentProgress + "/" + objective.getRequiredAmount() + ")"))
                .withStyle(style -> style.withColor(0xFFAA00));
        
        player.displayClientMessage(message, true);
    }
    
    /**
     * 显示委托完成提示
     */
    private void showQuestCompleted(ServerPlayer player, QuestDefinition definition) {
        Component title = Component.literal("✓ ")
                .append(definition.getTitle())
                .withStyle(style -> style.withColor(0x55FF55).withBold(true));
        
        player.displayClientMessage(title, false);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
    }
    
    /**
     * 更新委托书状态
     */
    private void updateScrollState(ServerPlayer player, QuestInstance instance) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem) {
                Optional<UUID> scrollInstanceId = QuestScrollItem.getInstanceId(stack);
                if (scrollInstanceId.isPresent() && scrollInstanceId.get().equals(instance.getInstanceId())) {
                    QuestScrollItem.updateState(stack, instance.getState());
                    break;
                }
            }
        }
    }
    
    /**
     * 清理玩家数据（登出时调用）
     */
    public void clearPlayerData(UUID playerId) {
        collectCache.remove(playerId);
        lastProgressShowTime.remove(playerId);
    }
    
    // ==================== 内部记录类 ====================
    
    /** 进度更新记录 */
    private record ProgressUpdateRecord(
            QuestInstance instance,
            String objectiveId,
            QuestObjective objective,
            int oldProgress,
            int newProgress
    ) {}
}
