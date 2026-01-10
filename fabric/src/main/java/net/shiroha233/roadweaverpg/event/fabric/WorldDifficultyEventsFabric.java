package net.shiroha233.roadweaverpg.event.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.worlddifficulty.MonsterScalingService;
import net.shiroha233.roadweaverpg.worlddifficulty.WorldDifficultyInitializer;

/**
 * Fabric平台世界难度事件处理
 * 
 * 原理：
 * - 监听怪物生成事件，应用属性缩放
 * - 使用Fabric API的实体事件系统
 */
public final class WorldDifficultyEventsFabric {
    
    private WorldDifficultyEventsFabric() {}
    
    public static void register() {
        // 初始化世界难度系统
        WorldDifficultyInitializer.initialize();
        
        // 监听实体加载事件（怪物生成）
        ServerEntityEvents.ENTITY_LOAD.register(WorldDifficultyEventsFabric::onEntityLoad);
        
        RoadWeaverRPG.LOGGER.info("[WorldDifficulty] Fabric事件已注册");
    }
    
    /**
     * 实体加载时处理
     */
    private static void onEntityLoad(Entity entity, ServerLevel level) {
        if (entity instanceof Mob mob) {
            try {
                MonsterScalingService.getInstance().onMobSpawn(mob, level);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("[WorldDifficulty] 处理怪物生成失败: {}", e.getMessage());
            }
        }
    }
}
