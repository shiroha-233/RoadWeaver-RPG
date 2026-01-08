package net.shiroha233.roadweaverpg.quest.map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective.TargetLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 委托地图标点管理器
 * 
 * 功能：
 * 1. 管理委托目标在地图上的标点
 * 2. 与RoadWeaver地图系统联动
 * 3. 支持标点的增删改查
 * 4. 客户端-服务端同步
 * 
 * 设计原则：
 * - 松耦合：通过接口与RoadWeaver地图交互
 * - 线程安全：使用ConcurrentHashMap
 * - 可扩展：支持自定义标点类型
 */
public class QuestMapMarkerManager {
    
    private static volatile QuestMapMarkerManager instance;
    private static final Object LOCK = new Object();
    
    // 玩家的标点数据：playerId -> (markerId -> MarkerData)
    private final Map<UUID, Map<String, QuestMarker>> playerMarkers = new ConcurrentHashMap<>();
    
    // RoadWeaver地图集成回调
    private MapIntegration mapIntegration;
    
    private QuestMapMarkerManager() {}
    
    public static QuestMapMarkerManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new QuestMapMarkerManager();
                }
            }
        }
        return instance;
    }

    /**
     * 设置地图集成回调
     */
    public void setMapIntegration(MapIntegration integration) {
        this.mapIntegration = integration;
    }
    
    /**
     * 为委托添加标点
     */
    public void addMarkersForQuest(ServerPlayer player, QuestInstance quest, 
                                    LocationKillObjective objective) {
        if (!objective.isShowOnMap()) return;
        
        UUID playerId = player.getUUID();
        Map<String, QuestMarker> markers = playerMarkers.computeIfAbsent(
                playerId, k -> new ConcurrentHashMap<>());
        
        int index = 0;
        for (TargetLocation loc : objective.getLocations()) {
            String markerId = makeMarkerId(quest.getQuestId(), objective.getId(), index++);
            
            QuestMarker marker = new QuestMarker(
                    markerId,
                    quest.getQuestId(),
                    objective.getId(),
                    loc.center(),
                    loc.radius(),
                    loc.name() != null ? loc.name() : getDefaultMarkerName(objective, index),
                    objective.getMapIcon(),
                    objective.getMapColor(),
                    loc.dimension()
            );
            
            markers.put(markerId, marker);
            
            // 通知地图系统
            if (mapIntegration != null) {
                mapIntegration.addMarker(player, marker);
            }
        }
        
        // 同步到客户端
        syncMarkersToClient(player);
        
        RoadWeaverRPG.LOGGER.debug("Added {} map markers for quest {} objective {}",
                objective.getLocations().size(), quest.getQuestId(), objective.getId());
    }
    
    /**
     * 移除委托的标点
     */
    public void removeMarkersForQuest(ServerPlayer player, ResourceLocation questId) {
        UUID playerId = player.getUUID();
        Map<String, QuestMarker> markers = playerMarkers.get(playerId);
        if (markers == null) return;
        
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, QuestMarker> entry : markers.entrySet()) {
            if (entry.getValue().questId().equals(questId)) {
                toRemove.add(entry.getKey());
                
                if (mapIntegration != null) {
                    mapIntegration.removeMarker(player, entry.getValue());
                }
            }
        }
        
        toRemove.forEach(markers::remove);
        syncMarkersToClient(player);
        
        RoadWeaverRPG.LOGGER.debug("Removed {} map markers for quest {}", 
                toRemove.size(), questId);
    }
    
    /**
     * 移除指定目标的标点
     */
    public void removeMarkersForObjective(ServerPlayer player, ResourceLocation questId, 
                                           String objectiveId) {
        UUID playerId = player.getUUID();
        Map<String, QuestMarker> markers = playerMarkers.get(playerId);
        if (markers == null) return;
        
        markers.entrySet().removeIf(entry -> {
            QuestMarker marker = entry.getValue();
            if (marker.questId().equals(questId) && marker.objectiveId().equals(objectiveId)) {
                if (mapIntegration != null) {
                    mapIntegration.removeMarker(player, marker);
                }
                return true;
            }
            return false;
        });
        
        syncMarkersToClient(player);
    }

    /**
     * 获取玩家的所有标点
     */
    public Collection<QuestMarker> getPlayerMarkers(UUID playerId) {
        Map<String, QuestMarker> markers = playerMarkers.get(playerId);
        return markers != null ? Collections.unmodifiableCollection(markers.values()) 
                               : Collections.emptyList();
    }
    
    /**
     * 获取指定委托的标点
     */
    public List<QuestMarker> getMarkersForQuest(UUID playerId, ResourceLocation questId) {
        Map<String, QuestMarker> markers = playerMarkers.get(playerId);
        if (markers == null) return Collections.emptyList();
        
        List<QuestMarker> result = new ArrayList<>();
        for (QuestMarker marker : markers.values()) {
            if (marker.questId().equals(questId)) {
                result.add(marker);
            }
        }
        return result;
    }
    
    /**
     * 清理玩家数据
     */
    public void clearPlayerData(UUID playerId) {
        playerMarkers.remove(playerId);
    }
    
    /**
     * 同步标点到客户端
     */
    private void syncMarkersToClient(ServerPlayer player) {
        // 通过网络包同步
        Collection<QuestMarker> markers = getPlayerMarkers(player.getUUID());
        net.shiroha233.roadweaverpg.quest.map.QuestMapMarkerSyncHandler.syncToClient(player, markers);
    }
    
    /**
     * 生成标点ID
     */
    private String makeMarkerId(ResourceLocation questId, String objectiveId, int index) {
        return questId.toString() + ":" + objectiveId + ":" + index;
    }
    
    /**
     * 获取默认标点名称
     */
    private String getDefaultMarkerName(LocationKillObjective objective, int index) {
        return objective.getDescription().getString() + " #" + index;
    }
    
    /**
     * 地图集成接口
     */
    public interface MapIntegration {
        void addMarker(ServerPlayer player, QuestMarker marker);
        void removeMarker(ServerPlayer player, QuestMarker marker);
        void updateMarker(ServerPlayer player, QuestMarker marker);
    }
    
    /**
     * 委托标点数据
     */
    public record QuestMarker(
            String markerId,
            ResourceLocation questId,
            String objectiveId,
            BlockPos position,
            int radius,
            String name,
            String icon,
            int color,
            ResourceLocation dimension
    ) {
        public net.minecraft.nbt.CompoundTag toNbt() {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("markerId", markerId);
            tag.putString("questId", questId.toString());
            tag.putString("objectiveId", objectiveId);
            tag.putInt("x", position.getX());
            tag.putInt("y", position.getY());
            tag.putInt("z", position.getZ());
            tag.putInt("radius", radius);
            tag.putString("name", name);
            tag.putString("icon", icon);
            tag.putInt("color", color);
            if (dimension != null) {
                tag.putString("dimension", dimension.toString());
            }
            return tag;
        }
        
        public static QuestMarker fromNbt(net.minecraft.nbt.CompoundTag tag) {
            return new QuestMarker(
                    tag.getString("markerId"),
                    new ResourceLocation(tag.getString("questId")),
                    tag.getString("objectiveId"),
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    tag.getInt("radius"),
                    tag.getString("name"),
                    tag.getString("icon"),
                    tag.getInt("color"),
                    tag.contains("dimension") ? new ResourceLocation(tag.getString("dimension")) : null
            );
        }
        
        public void toNetwork(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeUtf(markerId);
            buf.writeResourceLocation(questId);
            buf.writeUtf(objectiveId);
            buf.writeBlockPos(position);
            buf.writeVarInt(radius);
            buf.writeUtf(name);
            buf.writeUtf(icon);
            buf.writeVarInt(color);
            buf.writeBoolean(dimension != null);
            if (dimension != null) {
                buf.writeResourceLocation(dimension);
            }
        }
        
        public static QuestMarker fromNetwork(net.minecraft.network.FriendlyByteBuf buf) {
            return new QuestMarker(
                    buf.readUtf(),
                    buf.readResourceLocation(),
                    buf.readUtf(),
                    buf.readBlockPos(),
                    buf.readVarInt(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readBoolean() ? buf.readResourceLocation() : null
            );
        }
    }
}
