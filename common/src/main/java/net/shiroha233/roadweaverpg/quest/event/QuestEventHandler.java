package net.shiroha233.roadweaverpg.quest.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;

/**
 * 委托事件处理器
 * 提供静态方法供平台特定事件调用
 * 
 * 设计原则：
 * - 依赖倒置：不直接依赖平台事件系统
 * - 单一职责：只负责将事件转发给管理器
 */
public final class QuestEventHandler {
    
    private static final int CHECK_INTERVAL = net.shiroha233.roadweaverpg.config.QuestSystemConfig.OBJECTIVE_CHECK_INTERVAL;
    
    private QuestEventHandler() {}
    
    /**
     * 处理实体击杀事件
     * 由平台特定代码在 LivingDeathEvent 中调用
     */
    public static void onEntityKilled(ServerPlayer killer, Entity victim) {
        if (victim instanceof LivingEntity) {
            PlayerQuestService.getInstance().updateProgress(killer, "entity_kill", victim);
        }
    }
    
    /**
     * 处理物品拾取事件
     * 由平台特定代码在 ItemPickupEvent 中调用
     */
    public static void onItemPickup(ServerPlayer player) {
        PlayerQuestService.getInstance().checkCollectObjectives(player);
    }
    
    /**
     * 处理玩家 Tick 事件（用于定期检查）
     * 建议每 tick 调用，内部会控制检查频率
     * 
     * 优化：
     * - 定期验证数据一致性
     * - 自动修复同步问题
     */
    public static void onPlayerTick(ServerPlayer player) {
        // 使用玩家的游戏时间进行周期性检查，避免多玩家共享计数器导致的频率异常
        if (player.level().getGameTime() % CHECK_INTERVAL == 0) {
            // 检查收集类目标
            PlayerQuestService.getInstance().checkCollectObjectives(player);
            // 检查探索类目标
            PlayerQuestService.getInstance().updateProgress(player, "player_move", null);
            
            // 定期验证数据一致性（每分钟一次）
            validateDataConsistency(player);
        }
    }
    
    /**
     * 验证数据一致性
     * 
     * 解决问题：
     * - 客户端与服务端数据不同步
     * - 委托书状态不一致
     * - 过期委托未及时处理
     */
    private static void validateDataConsistency(ServerPlayer player) {
        net.shiroha233.roadweaverpg.quest.sync.QuestSyncValidator validator = 
                net.shiroha233.roadweaverpg.quest.sync.QuestSyncValidator.getInstance();
        
        if (validator.shouldValidate(player)) {
            net.shiroha233.roadweaverpg.data.PlayerQuestData playerData = 
                    net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().getPlayerData(player);
            validator.validateAndFix(player, playerData);
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
     * 用于同步委托数据到客户端
     */
    public static void onPlayerLogin(ServerPlayer player) {
        // 验证并修复背包中的委托书
        PlayerQuestService.getInstance().validateInventoryScrolls(player);
        
        // 检查并刷新每日委托
        DailyQuestManager.getInstance().checkAndRefreshDailyQuests(player);
        
        // 同步所有任务定义
        PlayerQuestService.getInstance().syncAllDefinitionsToClient(player);
        // 同步所有正在进行的任务实例
        PlayerQuestService.getInstance().syncAllQuestsToClient(player);
        
        // 同步声望等级定义
        syncReputationDefinitions(player);
        // 同步玩家声望数据
        syncPlayerReputation(player);
        
        // 同步每日委托数据
        syncDailyQuests(player);
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
    
    /**
     * 同步每日委托数据到客户端
     */
    public static void syncDailyQuests(ServerPlayer player) {
        PlayerQuestService.getInstance().syncDailyQuestsToClient(player);
    }
    
    /**
     * 处理玩家登出事件
     * 清理缓存和验证器数据，防止内存泄漏
     */
    public static void onPlayerLogout(ServerPlayer player) {
        java.util.UUID playerId = player.getUUID();
        
        // 清理目标进度缓存
        net.shiroha233.roadweaverpg.quest.cache.ObjectiveProgressCache.getInstance().clearCache(playerId);
        
        // 清理同步验证器数据
        net.shiroha233.roadweaverpg.quest.sync.QuestSyncValidator.getInstance().clearPlayerData(playerId);
        
        // 清理过期的领取记录（保留最长周期的2倍时间）
        net.shiroha233.roadweaverpg.data.PlayerQuestData playerData = 
                net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().getPlayerData(player);
        if (playerData != null) {
            playerData.cleanupExpiredAcceptanceHistory(86400); // 默认保留1天
        }
    }
}
