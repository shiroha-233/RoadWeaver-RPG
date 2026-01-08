package net.shiroha233.roadweaverpg.adventure;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 冒险等级事件处理器
 * 处理击杀怪物获得经验等事件
 */
public class AdventureEventHandler {
    
    private AdventureEventHandler() {}
    
    /**
     * 处理实体死亡事件
     * 当玩家击杀怪物时给予冒险经验
     */
    public static void onEntityKilled(LivingEntity entity, Player killer) {
        if (!(killer instanceof ServerPlayer serverPlayer)) return;
        if (entity instanceof Player) return; // 不计算击杀玩家
        
        try {
            ResourceLocation entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(entity.getType());
            
            AdventureExpSourceManager sourceManager = AdventureExpSourceManager.getInstance();
            int exp = sourceManager.getExpForEntity(entityType);
            
            if (exp > 0) {
                AdventureDataService.getInstance().addAdventureExp(serverPlayer, exp);
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error processing entity kill for adventure exp: {}", e.getMessage());
        }
    }
    
    /**
     * 处理玩家登录事件
     * 同步冒险等级数据到客户端
     */
    public static void onPlayerLogin(ServerPlayer player) {
        try {
            // 同步等级定义
            syncAdventureLevelDefinitions(player);
            // 同步玩家数据
            syncPlayerAdventureData(player);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Error syncing adventure data on login: {}", e.getMessage());
        }
    }
    
    /**
     * 同步冒险等级定义到客户端
     */
    public static void syncAdventureLevelDefinitions(ServerPlayer player) {
        AdventureLevelManager manager = AdventureLevelManager.getInstance();
        if (manager != null) {
            manager.syncToClient(player);
        }
    }
    
    /**
     * 同步玩家冒险数据到客户端
     */
    public static void syncPlayerAdventureData(ServerPlayer player) {
        AdventureDataService.getInstance().syncToClient(player);
    }
}
