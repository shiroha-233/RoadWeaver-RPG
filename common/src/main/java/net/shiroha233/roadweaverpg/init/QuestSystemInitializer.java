package net.shiroha233.roadweaverpg.init;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.QuestCache;
import net.shiroha233.roadweaverpg.event.QuestEventBus;
import net.shiroha233.roadweaverpg.common.util.PerformanceMonitor;
import net.shiroha233.roadweaverpg.common.util.QuestLogger;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;

/**
 * 委托系统初始化器
 * 统一管理所有组件的初始化
 */
public final class QuestSystemInitializer {
    
    private static boolean initialized = false;
    
    private QuestSystemInitializer() {}
    
    public static void initialize() {
        if (initialized) {
            RoadWeaverRPG.LOGGER.warn("Quest system already initialized!");
            return;
        }
        
        RoadWeaverRPG.LOGGER.info("Initializing Quest System...");
        
        initializeRegistries();
        initializeServices();
        initializeEventBus();
        initializeCache();
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("Quest System initialized successfully!");
    }
    
    private static void initializeRegistries() {
        try {
            Class.forName("net.shiroha233.roadweaverpg.quest.objective.ObjectiveRegistry");
            Class.forName("net.shiroha233.roadweaverpg.quest.reward.RewardRegistry");
        } catch (ClassNotFoundException e) {
            RoadWeaverRPG.LOGGER.error("Failed to load registries", e);
        }
        RoadWeaverRPG.LOGGER.debug("Registries initialized");
    }
    
    private static void initializeServices() {
        PlayerQuestService.getInstance();
        RoadWeaverRPG.LOGGER.debug("Services initialized");
    }
    
    private static void initializeEventBus() {
        QuestEventBus.getInstance();
        RoadWeaverRPG.LOGGER.debug("Event bus initialized");
    }
    
    private static void initializeCache() {
        QuestCache.getInstance();
        RoadWeaverRPG.LOGGER.debug("Cache initialized");
    }
    
    public static void shutdown() {
        if (!initialized) return;
        
        RoadWeaverRPG.LOGGER.info("Shutting down Quest System...");
        
        if (PerformanceMonitor.isEnabled()) {
            PerformanceMonitor.logStatistics();
        }
        
        QuestCache.getInstance().clear();
        QuestEventBus.getInstance().clear();
        
        initialized = false;
        RoadWeaverRPG.LOGGER.info("Quest System shutdown complete");
    }
    
    public static void setDebugMode(boolean enabled) {
        QuestLogger.setDebugEnabled(enabled);
        PerformanceMonitor.setEnabled(enabled);
        
        if (enabled) {
            RoadWeaverRPG.LOGGER.info("Quest System debug mode enabled");
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
