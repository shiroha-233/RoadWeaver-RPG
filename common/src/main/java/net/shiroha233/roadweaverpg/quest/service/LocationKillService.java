package net.shiroha233.roadweaverpg.quest.service;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.loot.QuestLootCollector;
import net.shiroha233.roadweaverpg.quest.map.QuestMapMarkerManager;
import net.shiroha233.roadweaverpg.quest.map.RoadWeaverMapIntegration;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;
import net.shiroha233.roadweaverpg.quest.spawn.QuestMobSpawnManager;

/**
 * 定点击杀目标服务
 * 
 * 负责：
 * 1. 委托接取时初始化刷新点、地图标点、掉落物收集
 * 2. 委托完成/放弃时清理相关资源
 * 3. 协调各子系统的交互
 * 
 * 设计原则：
 * - 单一职责：只负责LocationKillObjective的生命周期管理
 * - 依赖倒置：通过接口与各子系统交互
 */
public class LocationKillService {
    
    private static volatile LocationKillService instance;
    private static final Object LOCK = new Object();
    
    private LocationKillService() {
        // 初始化地图集成
        QuestMapMarkerManager.getInstance().setMapIntegration(
                RoadWeaverMapIntegration.getInstance());
    }
    
    public static LocationKillService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new LocationKillService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 委托接取时调用
     */
    public void onQuestAccepted(ServerPlayer player, QuestInstance quest, QuestDefinition definition) {
        for (QuestObjective objective : definition.getObjectives()) {
            if (objective instanceof LocationKillObjective locKill) {
                initializeLocationKillObjective(player, quest, locKill);
            }
        }
    }
    
    /**
     * 初始化定点击杀目标
     */
    private void initializeLocationKillObjective(ServerPlayer player, QuestInstance quest,
                                                   LocationKillObjective objective) {
        // 注册怪物刷新点
        if (objective.getSpawnConfig() != null && objective.getSpawnConfig().enabled()) {
            QuestMobSpawnManager.getInstance().registerSpawnPoint(quest, objective);
            RoadWeaverRPG.LOGGER.debug("Registered spawn point for quest {} objective {}",
                    quest.getQuestId(), objective.getId());
        }
        
        // 添加地图标点
        if (objective.isShowOnMap()) {
            QuestMapMarkerManager.getInstance().addMarkersForQuest(player, quest, objective);
            RoadWeaverRPG.LOGGER.debug("Added map markers for quest {} objective {}",
                    quest.getQuestId(), objective.getId());
        }
        
        // 注册掉落物收集区域
        if (objective.isAutoCollectDrops()) {
            QuestLootCollector.getInstance().registerCollectionZone(quest, objective);
            RoadWeaverRPG.LOGGER.debug("Registered loot collection for quest {} objective {}",
                    quest.getQuestId(), objective.getId());
        }
    }
    
    /**
     * 委托完成/放弃时调用
     */
    public void onQuestEnded(ServerPlayer player, QuestInstance quest, QuestDefinition definition) {
        for (QuestObjective objective : definition.getObjectives()) {
            if (objective instanceof LocationKillObjective locKill) {
                cleanupLocationKillObjective(player, quest, locKill);
            }
        }
    }
    
    /**
     * 清理定点击杀目标资源
     */
    private void cleanupLocationKillObjective(ServerPlayer player, QuestInstance quest,
                                                LocationKillObjective objective) {
        // 注销怪物刷新点
        QuestMobSpawnManager.getInstance().unregisterSpawnPoint(
                quest.getQuestId(), objective.getId());
        
        // 移除地图标点
        QuestMapMarkerManager.getInstance().removeMarkersForObjective(
                player, quest.getQuestId(), objective.getId());
        
        // 注销掉落物收集区域
        QuestLootCollector.getInstance().unregisterCollectionZone(
                quest.getQuestId(), objective.getId());
        
        RoadWeaverRPG.LOGGER.debug("Cleaned up location kill objective for quest {} objective {}",
                quest.getQuestId(), objective.getId());
    }
    
    /**
     * 清理指定委托的所有资源
     */
    public void cleanupQuest(ServerPlayer player, QuestInstance quest) {
        QuestDefinition definition = QuestDefinitionLoader.getInstance()
                .getDefinition(quest.getQuestId());
        if (definition != null) {
            onQuestEnded(player, quest, definition);
        }
    }
}
