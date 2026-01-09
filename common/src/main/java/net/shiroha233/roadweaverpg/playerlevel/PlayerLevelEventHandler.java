package net.shiroha233.roadweaverpg.playerlevel;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 玩家等级事件处理器
 * 玩家等级通过完成委托和使用经验书升级，不再通过击杀怪物
 */
public final class PlayerLevelEventHandler {
    
    private PlayerLevelEventHandler() {}
    
    /**
     * 处理玩家登录事件
     * 同步玩家等级数据到客户端并刷新效果
     */
    public static void onPlayerLogin(ServerPlayer player) {
        try {
            // 同步等级定义
            syncPlayerLevelDefinitions(player);
            // 同步玩家数据
            syncPlayerLevelData(player);
            // 刷新效果
            PlayerLevelDataService.getInstance().refreshEffects(player);
            // 同步技能点分配数据并刷新属性效果
            syncStatAllocationData(player);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error syncing player level data on login: {}", e.getMessage());
        }
    }
    
    /**
     * 处理玩家重生事件
     * 重新应用等级效果和技能点属性
     */
    public static void onPlayerRespawn(ServerPlayer player) {
        try {
            // 延迟一tick应用效果，确保玩家状态已重置
            player.getServer().execute(() -> {
                PlayerLevelDataService.getInstance().refreshEffects(player);
                // 重新应用技能点属性
                net.shiroha233.roadweaverpg.stats.StatAllocationService.getInstance().refreshAllStats(player);
            });
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error refreshing effects on respawn: {}", e.getMessage());
        }
    }
    
    /**
     * 同步玩家等级定义到客户端
     */
    public static void syncPlayerLevelDefinitions(ServerPlayer player) {
        if (PlayerLevelManager.isInitialized()) {
            PlayerLevelManager.getInstance().syncToClient(player);
        }
    }
    
    /**
     * 同步玩家等级数据到客户端
     */
    public static void syncPlayerLevelData(ServerPlayer player) {
        PlayerLevelDataService.getInstance().syncToClient(player);
    }
    
    /**
     * 同步技能点分配数据到客户端并刷新属性效果
     */
    public static void syncStatAllocationData(ServerPlayer player) {
        var service = net.shiroha233.roadweaverpg.stats.StatAllocationService.getInstance();
        service.refreshAllStats(player);
        service.syncToClient(player);
    }
}
