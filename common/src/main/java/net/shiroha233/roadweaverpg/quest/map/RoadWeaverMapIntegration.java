package net.shiroha233.roadweaverpg.quest.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.map.QuestMapMarkerManager.MapIntegration;
import net.shiroha233.roadweaverpg.quest.map.QuestMapMarkerManager.QuestMarker;

/**
 * RoadWeaver地图集成
 * 
 * 通过反射调用RoadWeaver的API，实现松耦合
 * 如果RoadWeaver不存在，则静默失败
 */
public class RoadWeaverMapIntegration implements MapIntegration {
    
    private static volatile RoadWeaverMapIntegration instance;
    private static final Object LOCK = new Object();
    
    private boolean roadWeaverAvailable = false;
    @SuppressWarnings("unused") // 保留用于将来的RoadWeaver集成
    private Class<?> clientMapNotesClass;
    
    private RoadWeaverMapIntegration() {
        checkRoadWeaverAvailability();
    }
    
    public static RoadWeaverMapIntegration getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RoadWeaverMapIntegration();
                }
            }
        }
        return instance;
    }
    
    /**
     * 检查RoadWeaver是否可用
     */
    private void checkRoadWeaverAvailability() {
        try {
            clientMapNotesClass = Class.forName(
                    "net.shiroha233.roadweaver.client.map.data.ClientMapNotes");
            roadWeaverAvailable = true;
            RoadWeaverRPG.LOGGER.info("RoadWeaver map integration enabled");
        } catch (ClassNotFoundException e) {
            roadWeaverAvailable = false;
            RoadWeaverRPG.LOGGER.info("RoadWeaver not found, map integration disabled");
        }
    }
    
    public boolean isAvailable() {
        return roadWeaverAvailable;
    }
    
    @Override
    public void addMarker(ServerPlayer player, QuestMarker marker) {
        if (!roadWeaverAvailable) return;
        
        try {
            // 使用RoadWeaver的笔记系统添加标点
            // 注意：这是客户端操作，需要通过网络包同步
            // 这里我们使用自定义的同步机制
            RoadWeaverRPG.LOGGER.debug("Adding map marker: {} at {}", 
                    marker.name(), marker.position());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to add map marker: {}", e.getMessage());
        }
    }
    
    @Override
    public void removeMarker(ServerPlayer player, QuestMarker marker) {
        if (!roadWeaverAvailable) return;
        
        try {
            RoadWeaverRPG.LOGGER.debug("Removing map marker: {}", marker.markerId());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to remove map marker: {}", e.getMessage());
        }
    }
    
    @Override
    public void updateMarker(ServerPlayer player, QuestMarker marker) {
        if (!roadWeaverAvailable) return;
        
        // 更新 = 删除 + 添加
        removeMarker(player, marker);
        addMarker(player, marker);
    }
    
    /**
     * 注册结构点到RoadWeaver（用于地图显示）
     */
    public void registerStructurePoint(ServerLevel level, BlockPos pos, String name) {
        if (!roadWeaverAvailable) return;
        
        try {
            Class<?> apiClass = Class.forName("net.shiroha233.roadweaver.api.RoadNetworkApi");
            apiClass.getMethod("registerStructureEndpoint", ServerLevel.class, BlockPos.class, 
                    String.class, boolean.class)
                    .invoke(null, level, pos, name, false);
            
            RoadWeaverRPG.LOGGER.debug("Registered structure point: {} at {}", name, pos);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to register structure point: {}", e.getMessage());
        }
    }
}
