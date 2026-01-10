package net.shiroha233.roadweaverpg.event.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.worlddifficulty.MonsterScalingService;
import net.shiroha233.roadweaverpg.worlddifficulty.WorldDifficultyInitializer;

/**
 * Forge平台世界难度事件处理
 * 
 * 原理：
 * - 监听怪物生成事件，应用属性缩放
 * - 使用Forge事件总线
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID)
public final class WorldDifficultyEventsForge {
    
    private static boolean initialized = false;
    
    private WorldDifficultyEventsForge() {}
    
    public static void init() {
        if (initialized) return;
        
        // 初始化世界难度系统
        WorldDifficultyInitializer.initialize();
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("[WorldDifficulty] Forge事件已注册");
    }
    
    /**
     * 实体加入世界时处理
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level) {
            try {
                MonsterScalingService.getInstance().onMobSpawn(mob, level);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("[WorldDifficulty] 处理怪物生成失败: {}", e.getMessage());
            }
        }
    }
}
