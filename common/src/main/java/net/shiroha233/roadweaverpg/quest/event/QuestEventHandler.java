package net.shiroha233.roadweaverpg.quest.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    
    private static final int CHECK_INTERVAL = 20; // 每秒检查一次
    
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
     */
    public static void onPlayerTick(ServerPlayer player) {
        // 使用玩家的游戏时间进行周期性检查，避免多玩家共享计数器导致的频率异常
        if (player.level().getGameTime() % CHECK_INTERVAL == 0) {
            // 检查收集类目标
            PlayerQuestService.getInstance().checkCollectObjectives(player);
            // 检查探索类目标
            PlayerQuestService.getInstance().updateProgress(player, "player_move", null);
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
        // 同步所有任务定义
        PlayerQuestService.getInstance().syncAllDefinitionsToClient(player);
        // 同步所有正在进行的任务实例
        PlayerQuestService.getInstance().syncAllQuestsToClient(player);
        
        // 同步声望等级定义
        syncReputationDefinitions(player);
        // 同步玩家声望数据
        syncPlayerReputation(player);
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
}
