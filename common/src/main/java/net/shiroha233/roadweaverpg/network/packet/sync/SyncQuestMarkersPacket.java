package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.quest.map.QuestMapMarkerManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 同步委托地图标点网络包
 * 
 * 方向：服务端 -> 客户端
 * 用途：同步玩家的所有委托地图标点
 */
public class SyncQuestMarkersPacket {
    
    private final List<QuestMapMarkerManager.QuestMarker> markers;
    
    public SyncQuestMarkersPacket(Collection<QuestMapMarkerManager.QuestMarker> markers) {
        this.markers = new ArrayList<>(markers);
    }
    
    public SyncQuestMarkersPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        this.markers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            markers.add(QuestMapMarkerManager.QuestMarker.fromNetwork(buf));
        }
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(markers.size());
        for (QuestMapMarkerManager.QuestMarker marker : markers) {
            marker.toNetwork(buf);
        }
    }
    
    public static SyncQuestMarkersPacket decode(FriendlyByteBuf buf) {
        return new SyncQuestMarkersPacket(buf);
    }
    
    public List<QuestMapMarkerManager.QuestMarker> getMarkers() {
        return markers;
    }
}
