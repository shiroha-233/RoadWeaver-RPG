package net.shiroha233.roadweaverpg.quest.map;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * 委托地图标点同步处理器
 * 
 * 职责：
 * - 将标点数据同步到客户端
 * - 处理标点的网络传输
 */
public final class QuestMapMarkerSyncHandler {
    
    private QuestMapMarkerSyncHandler() {}
    
    /**
     * 同步标点到客户端
     * 由平台特定的网络处理器实现具体发送
     */
    public static void syncToClient(ServerPlayer player, Collection<QuestMapMarkerManager.QuestMarker> markers) {
        // 由平台特定的网络处理器调用sendSyncQuestMarkers方法
        // Forge: NetworkHandlerForge.sendSyncQuestMarkers(player, new SyncQuestMarkersPacket(markers))
        // Fabric: NetworkHandlerFabric.sendSyncQuestMarkers(player, new SyncQuestMarkersPacket(markers))
    }
}
