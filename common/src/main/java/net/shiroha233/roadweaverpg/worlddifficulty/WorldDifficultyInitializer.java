package net.shiroha233.roadweaverpg.worlddifficulty;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 世界难度系统初始化器
 * 
 * 设计原理：
 * - 统一管理系统初始化
 * - 确保初始化顺序正确
 * - 防止重复初始化
 */
public final class WorldDifficultyInitializer {
    
    private static volatile boolean initialized = false;
    private static final Object LOCK = new Object();
    
    private WorldDifficultyInitializer() {}
    
    /**
     * 初始化世界难度系统
     */
    public static void initialize() {
        if (initialized) {
            RoadWeaverRPG.LOGGER.warn("世界难度系统已初始化，跳过");
            return;
        }
        
        synchronized (LOCK) {
            if (initialized) return;
            
            RoadWeaverRPG.LOGGER.info("初始化世界难度系统...");
            
            // 初始化服务
            MonsterScalingService.getInstance();
            
            initialized = true;
            RoadWeaverRPG.LOGGER.info("世界难度系统初始化完成");
        }
    }
    
    /**
     * 关闭系统
     */
    public static void shutdown() {
        if (!initialized) return;
        
        synchronized (LOCK) {
            RoadWeaverRPG.LOGGER.info("关闭世界难度系统...");
            initialized = false;
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
