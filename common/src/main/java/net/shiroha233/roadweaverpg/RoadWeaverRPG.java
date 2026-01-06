package net.shiroha233.roadweaverpg;

import net.shiroha233.roadweaverpg.init.QuestSystemInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RoadWeaver RPG 附属模组主类
 * 为 RoadWeaver 添加 RPG 玩法元素
 */
public class RoadWeaverRPG {
    
    public static final String MOD_ID = "roadweaver_rpg";
    public static final String MOD_NAME = "RoadWeaver RPG";
    
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    /**
     * 初始化 RPG 模组（完整模式 - 需要 RoadWeaver）
     */
    public static void initialize() {
        LOGGER.info("Initializing {} (Full Mode)...", MOD_NAME);
        
        // 初始化委托系统
        QuestSystemInitializer.initialize();
        
        LOGGER.info("RoadWeaver RPG expansion loaded successfully with RoadWeaver integration!");
    }
    
    /**
     * 初始化 RPG 模组（独立模式 - 无 RoadWeaver）
     */
    public static void initializeStandalone() {
        LOGGER.info("Initializing {} (Standalone Mode)...", MOD_NAME);
        LOGGER.warn("Running without RoadWeaver - limited functionality available.");
        
        // 初始化委托系统
        QuestSystemInitializer.initialize();
        
        LOGGER.info("RoadWeaver RPG loaded in standalone mode.");
    }
    
    /**
     * 检查 RoadWeaver 主模组是否可用
     */
    public static boolean isRoadWeaverAvailable() {
        try {
            Class.forName("net.shiroha233.roadweaver.RoadWeaver");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.error("RoadWeaver main mod not found! RoadWeaver RPG requires RoadWeaver to function.");
            return false;
        }
    }
}
