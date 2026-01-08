package net.shiroha233.roadweaverpg.quest.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager;
import net.shiroha233.roadweaverpg.quest.index.ObjectiveIndex;
import net.shiroha233.roadweaverpg.quest.loot.QuestLootCollector;
import net.shiroha233.roadweaverpg.quest.reward.PriorityRewardQueue;
import net.shiroha233.roadweaverpg.quest.service.*;
import net.shiroha233.roadweaverpg.quest.spawn.QuestMobSpawnManager;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;

/**
 * 委托事件处理器（重构版）
 * 
 * 改进点：
 * - 遵循单一职责原则（SRP）：将不同职责委托给专门的服务类
 * - 降低耦合度：通过服务层进行通信
 * - 提高可维护性：每个服务类职责清晰
 * 
 * 职责：
 * - 接收游戏事件
 * - 委托给对应的服务类处理
 * - 协调各个服务之间的交互
 */
public final class QuestEventHandler {
    
    private static final int CHECK_INTERVAL = net.shiroha233.roadweaverpg.config.QuestSystemConfig.OBJECTIVE_CHECK_INTERVAL;
    
    private QuestEventHandler() {}
    
    /**
     * 处理实体击杀事件
     */
    public static void onEntityKilled(ServerPlayer killer, Entity victim) {
        if (victim instanceof LivingEntity) {
            PlayerQuestService.getInstance().updateProgress(killer, "entity_kill", victim);
            
            // 处理委托怪物死亡
            QuestMobSpawnManager spawnManager = QuestMobSpawnManager.getInstance();
            if (spawnManager.isQuestMob(victim)) {
                spawnManager.onMobDeath(victim);
            }
        }
    }
    
    /**
     * 处理物品拾取事件
     * 
     * 重要修复：
     * - 在物品拾取的瞬间评估条件
     * - 将条件状态传递给检查方法
     * - 这样可以正确处理 in_water、in_lava 等瞬时条件
     */
    public static void onItemPickup(ServerPlayer player) {
        // 立即检查，使用当前玩家状态
        PlayerQuestService.getInstance().checkCollectObjectives(player);
    }
    
    /**
     * 处理玩家 Tick 事件（优化版）
     * 
     * 原理：将不同职责委托给专门的服务类，避免单个方法承担过多职责
     * 修复：分离收集检查和移动检查，避免重复调用updateProgress
     */
    public static void onPlayerTick(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        
        // 1. 收集类目标检查（每 CHECK_INTERVAL tick）
        if (gameTime % CHECK_INTERVAL == 0) {
            PlayerQuestService service = PlayerQuestService.getInstance();
            // 收集检查（内部会调用updateProgress）
            service.checkCollectObjectives(player);
        }
        
        // 2. 移动类目标检查（每 CHECK_INTERVAL tick，错开1tick避免同帧重复显示）
        if ((gameTime + 1) % CHECK_INTERVAL == 0) {
            PlayerQuestService.getInstance().updateProgress(player, "player_move", null);
        }
        
        // 3. 奖励处理（每5秒，100 ticks）
        if (gameTime % 100 == 0) {
            PriorityRewardQueue.getInstance().processPlayerRewards(player);
        }
    }
    
    /**
     * 处理服务端Tick事件（添加定期清理）
     */
    public static void onServerLevelTick(ServerLevel level) {
        QuestMobSpawnManager.getInstance().tick(level);
        QuestLootCollector.getInstance().tick(level);
        
        long gameTime = level.getGameTime();
        
        // 定期清理过期数据（每5分钟，6000 ticks）
        if (gameTime % 6000 == 0) {
            cleanupExpiredData();
        }
    }
    
    /**
     * 清理过期数据（防止内存泄漏）
     * 
     * 原理：定期清理各个组件中的过期缓存和历史记录
     */
    private static void cleanupExpiredData() {
        try {
            // 清理目标索引
            ObjectiveIndex.getInstance().cleanupExpiredData();
            
            // 清理状态转移日志（保留24小时）
            net.shiroha233.roadweaverpg.quest.state.StateTransitionLog.getInstance()
                    .cleanupExpiredLogs(86400_000L);
            
            // 清理奖励历史
            PriorityRewardQueue.getInstance().cleanupExpiredHistory();
            
            // 清理目标进度缓存
            net.shiroha233.roadweaverpg.quest.cache.ObjectiveProgressCache.getInstance()
                    .cleanupExpiredCaches();
            
            RoadWeaverRPG.LOGGER.debug("Completed periodic cleanup of expired quest data");
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("清理过期数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理方块破坏事件
     */
    public static void onBlockBroken(ServerPlayer player, BlockState blockState) {
        PlayerQuestService.getInstance().updateProgress(player, "block_break", blockState);
    }
    
    /**
     * 处理玩家登录事件
     * 
     * 修复：
     * - 确保数据加载后立即标记脏数据
     * - 增强日志便于调试
     * - 确保所有数据正确同步到客户端
     */
    public static void onPlayerLogin(ServerPlayer player) {
        RoadWeaverRPG.LOGGER.info("Processing login for player: {}", player.getName().getString());
        
        try {
            // 获取玩家数据（会自动创建并标记脏数据）
            net.shiroha233.roadweaverpg.data.PlayerQuestData playerData = 
                    net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().getPlayerData(player);
            
            // 记录加载的数据信息
            RoadWeaverRPG.LOGGER.info("Loaded player data: {} active quests, {} completed quests, reputation levels: {}", 
                    playerData.getActiveQuests().size(),
                    playerData.getCompletedQuests().size(),
                    playerData.getAllReputationLevels());
            
            // 验证并修复背包中的委托书
            PlayerQuestService.getInstance().validateInventoryScrolls(player);
            
            // 检查并刷新每日委托
            DailyQuestManager.getInstance().checkAndRefreshDailyQuests(player);
            
            // 重建目标索引
            ObjectiveIndex.getInstance().rebuildIndex(player.getUUID(), playerData.getActiveQuests(), playerData.getVersion());
            
            // 同步所有任务定义到客户端
            PlayerQuestService.getInstance().syncAllDefinitionsToClient(player);
            
            // 同步所有正在进行的任务实例到客户端
            PlayerQuestService.getInstance().syncAllQuestsToClient(player);
            
            // 同步声望等级定义
            syncReputationDefinitions(player);
            
            // 同步玩家声望数据
            syncPlayerReputation(player);
            
            // 同步每日委托数据
            syncDailyQuests(player);
            
            // 强制进行一次数据验证
            net.shiroha233.roadweaverpg.quest.sync.QuestSyncValidator.getInstance().forceValidate(player, playerData);
            
            // 确保数据被标记为需要保存
            net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().markDirty(player);
            
            RoadWeaverRPG.LOGGER.info("Player login processing completed: {}", player.getName().getString());
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error processing player login for {}: {}", 
                    player.getName().getString(), e.getMessage(), e);
        }
    }
    
    public static void syncReputationDefinitions(ServerPlayer player) {
        ReputationManager manager = ReputationManager.getInstance();
        if (manager != null) {
            manager.syncToClient(player);
        }
    }

    public static void syncPlayerReputation(ServerPlayer player) {
        PlayerQuestService.getInstance().syncReputationToClient(player);
    }
    
    public static void syncDailyQuests(ServerPlayer player) {
        PlayerQuestService.getInstance().syncDailyQuestsToClient(player);
    }
    
    /**
     * 处理玩家登出事件（优化版）
     * 
     * 原理：清理所有与玩家相关的缓存和临时数据，防止内存泄漏
     */
    public static void onPlayerLogout(ServerPlayer player) {
        java.util.UUID playerId = player.getUUID();
        
        // 清理进度服务缓存（V2新增）
        PlayerQuestService.getInstance().clearPlayerCache(playerId);
        
        // 清理目标索引
        ObjectiveIndex.getInstance().clearPlayerIndex(playerId);
        
        // 清理目标进度缓存
        net.shiroha233.roadweaverpg.quest.cache.ObjectiveProgressCache.getInstance().clearCache(playerId);
        
        // 清理同步验证器数据
        net.shiroha233.roadweaverpg.quest.sync.QuestSyncValidator.getInstance().clearPlayerData(playerId);
        
        // 清理增量同步数据
        net.shiroha233.roadweaverpg.quest.sync.IncrementalSyncManager.getInstance().clearPlayerData(playerId);
        
        // 清理奖励队列中的玩家数据
        PriorityRewardQueue.getInstance().clearPlayerData(playerId);
        
        // 清理怪物刷新数据
        QuestMobSpawnManager.getInstance().clearPlayerData(playerId);
        
        // 清理掉落物收集数据
        QuestLootCollector.getInstance().clearPlayerData(playerId);
        
        // 清理地图标点数据
        net.shiroha233.roadweaverpg.quest.map.QuestMapMarkerManager.getInstance().clearPlayerData(playerId);
        
        // 清理过期的领取记录（保留24小时）
        net.shiroha233.roadweaverpg.data.PlayerQuestData playerData = 
                net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().getPlayerData(player);
        if (playerData != null) {
            playerData.cleanupExpiredAcceptanceHistory(86400);
        }
        
        // 标记数据需要保存
        net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().markDirty(player);
        
        RoadWeaverRPG.LOGGER.debug("Cleaned up all quest data for player: {}", player.getName().getString());
    }
}