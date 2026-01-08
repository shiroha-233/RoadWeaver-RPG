package net.shiroha233.roadweaverpg.playerlevel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 玩家等级事件处理器
 * 处理击杀怪物获得经验等事件
 */
public final class PlayerLevelEventHandler {
    
    private PlayerLevelEventHandler() {}
    
    /**
     * 处理实体死亡事件
     * 当玩家击杀怪物时给予玩家经验
     */
    public static void onEntityKilled(LivingEntity entity, Player killer) {
        if (!(killer instanceof ServerPlayer serverPlayer)) return;
        if (entity instanceof Player) return; // 不计算击杀玩家
        
        try {
            if (!PlayerExpSourceManager.isInitialized()) return;
            
            ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            
            PlayerExpSourceManager sourceManager = PlayerExpSourceManager.getInstance();
            int exp = sourceManager.getExpForEntity(entityType);
            
            if (exp > 0) {
                PlayerLevelDataService.getInstance().addPlayerExp(serverPlayer, exp);
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error processing entity kill for player exp: {}", e.getMessage());
        }
    }
    
    /**
     * 处理玩家登录事件
     * 同步玩家等级数据到客户端并刷新效果
     */
    public static void onPlayerLogin(ServerPlayer player) {
        try {
            // 同步等级定义
            syncPlayerLevelDefinitions(player);
            // 同步经验来源（可选）
            // 同步玩家数据
            syncPlayerLevelData(player);
            // 刷新效果
            PlayerLevelDataService.getInstance().refreshEffects(player);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error syncing player level data on login: {}", e.getMessage());
        }
    }
    
    /**
     * 处理玩家重生事件
     * 重新应用等级效果
     */
    public static void onPlayerRespawn(ServerPlayer player) {
        try {
            // 延迟一tick应用效果，确保玩家状态已重置
            player.getServer().execute(() -> {
                PlayerLevelDataService.getInstance().refreshEffects(player);
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
}
