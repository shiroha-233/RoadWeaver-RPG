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
import net.shiroha233.roadweaverpg.quest.cache.ObjectiveProgressCache;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.type.QuestType;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托进度服务
 * 
 * 性能优化：
 * - 使用ObjectiveProgressCache减少遍历
 * - 按事件类型过滤目标
 * - 缓存收集类目标的检查结果
 */
public class QuestProgressService {
    
    private final QuestDataAccessor dataAccessor;
    private final ObjectiveProgressCache cache;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestUpdated;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestCompleted;
    
    public QuestProgressService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
        this.cache = ObjectiveProgressCache.getInstance();
    }
    
    public void setOnQuestUpdated(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestUpdated = callback;
    }
    
    public void setOnQuestCompleted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestCompleted = callback;
    }
    
    /**
     * 更新进度（优化版本）
     * 
     * 优化原理：
     * 1. 使用版本号机制检测数据变更
     * 2. 分级缓存减少重复计算
     * 3. 复杂度从O(n³)降低到O(n)
     */
    public void updateProgress(ServerPlayer player, String eventType, Object eventData) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        UUID playerId = player.getUUID();
        
        // 使用版本号机制确保缓存有效
        cache.ensureCacheValid(player, playerData.getVersion(), playerData.getActiveQuests());
        
        // 只获取与此事件类型相关的目标
        var cachedObjectives = cache.getObjectivesForEvent(playerId, eventType);
        if (cachedObjectives.isEmpty()) return;
        
        boolean anyUpdated = false;
        
        for (var cached : cachedObjectives) {
            QuestInstance instance = cached.instance();
            
            // 检查过期
            if (handleExpiration(player, instance, playerData)) {
                anyUpdated = true;
                continue;
            }
            
            if (instance.getState() != QuestState.IN_PROGRESS) continue;
            
            QuestDefinition definition = QuestDefinitionLoader.getInstance()
                    .getDefinition(instance.getQuestId());
            if (definition == null) continue;
            
            // 处理单个目标
            boolean updated = processObjective(player, instance, cached.objective(), 
                    cached.objectiveId(), eventType, eventData);
            
            if (updated) {
                anyUpdated = true;
                handleStateChange(player, instance, definition);
            }
        }
        
        if (anyUpdated) {
            dataAccessor.markDirty(player);
            // 使L1缓存失效，下次访问时从L2获取
            cache.invalidateL1Cache(playerId);
        }
    }
    
    /**
     * 检查收集类目标
     * 背包变化时调用，使缓存失效
     */
    public void checkCollectObjectives(ServerPlayer player) {
        cache.invalidateCollectCache(player.getUUID());
        updateProgress(player, "inventory_check", null);
    }
    
    private boolean handleExpiration(ServerPlayer player, QuestInstance instance, PlayerQuestData playerData) {
        if (instance.isExpired()) {
            instance.setState(QuestState.EXPIRED);
            playerData.markQuestFailed(instance.getQuestId());
            updateScrollState(player, instance);
            RoadWeaverRPG.LOGGER.info("Quest {} expired for player {}", 
                    instance.getQuestId(), player.getName().getString());
            return true;
        }
        return false;
    }
    
    /**
     * 处理单个目标（优化版本）
     */
    private boolean processObjective(ServerPlayer player, QuestInstance instance,
                                     QuestObjective objective, String objectiveId,
                                     String eventType, Object eventData) {
        int oldProgress = instance.getObjectiveProgress(objectiveId).getCurrentProgress();
        
        // 使用缓存优化收集类目标
        int progress;
        if (objective.getType() == QuestType.COLLECT && "inventory_check".equals(eventType)) {
            progress = getCachedCollectProgress(player, objective, objectiveId);
        } else {
            progress = objective.checkProgress(player, eventType, eventData);
        }
        
        boolean shouldUpdate = progress > 0 || 
                (objective.getType() == QuestType.COLLECT && "inventory_check".equals(eventType));
        
        if (shouldUpdate) {
            if (objective.getType() == QuestType.COLLECT) {
                instance.setObjectiveProgress(objectiveId, progress);
            } else {
                instance.updateObjectiveProgress(objectiveId, progress);
            }
            
            int newProgress = instance.getObjectiveProgress(objectiveId).getCurrentProgress();
            if (newProgress != oldProgress) {
                showProgressUpdate(player, objective, newProgress);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取缓存的收集进度
     */
    private int getCachedCollectProgress(ServerPlayer player, QuestObjective objective, String objectiveId) {
        UUID playerId = player.getUUID();
        
        // 尝试从缓存获取
        Optional<Integer> cached = cache.getCachedCollectResult(playerId, objectiveId);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 缓存未命中，执行检查并缓存结果
        int progress = objective.checkProgress(player, "inventory_check", null);
        cache.cacheCollectResult(playerId, objectiveId, progress);
        return progress;
    }
    
    private void handleStateChange(ServerPlayer player, QuestInstance instance, QuestDefinition definition) {
        if (instance.getState() == QuestState.COMPLETED) {
            updateScrollState(player, instance);
            showQuestCompleted(player, definition);
            if (onQuestCompleted != null) {
                onQuestCompleted.accept(player, instance);
            }
        } else if (onQuestUpdated != null) {
            onQuestUpdated.accept(player, instance);
        }
    }
    
    private void showProgressUpdate(ServerPlayer player, QuestObjective objective, int currentProgress) {
        Component message = Component.literal(objective.getDescription().getString() + " ")
                .append(Component.literal("(" + currentProgress + "/" + objective.getRequiredAmount() + ")"))
                .withStyle(style -> style.withColor(0xFFAA00));
        player.displayClientMessage(message, true);
    }
    
    private void showQuestCompleted(ServerPlayer player, QuestDefinition definition) {
        Component title = Component.literal("✓ ")
                .append(definition.getTitle())
                .withStyle(style -> style.withColor(0x55FF55).withBold(true));
        player.displayClientMessage(title, false);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
    }
    
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
}
