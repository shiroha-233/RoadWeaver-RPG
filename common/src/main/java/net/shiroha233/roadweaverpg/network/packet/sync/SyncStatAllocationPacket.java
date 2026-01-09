package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.stats.StatAllocationData;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 同步属性分配数据（服务端 -> 客户端）
 */
public record SyncStatAllocationPacket(int availablePoints, Map<StatType, Integer> allocatedPoints) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(availablePoints);
        
        // 只写入可分配的属性
        int count = 0;
        for (StatType type : StatType.values()) {
            if (type.isAllocatable()) count++;
        }
        buf.writeVarInt(count);
        
        for (StatType type : StatType.values()) {
            if (type.isAllocatable()) {
                buf.writeUtf(type.getId());
                buf.writeVarInt(allocatedPoints.getOrDefault(type, 0));
            }
        }
    }
    
    public static SyncStatAllocationPacket decode(FriendlyByteBuf buf) {
        int availablePoints = buf.readVarInt();
        int count = buf.readVarInt();
        
        Map<StatType, Integer> allocated = new EnumMap<>(StatType.class);
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            int points = buf.readVarInt();
            StatType type = StatType.fromId(id);
            if (type != null) {
                allocated.put(type, points);
            }
        }
        
        return new SyncStatAllocationPacket(availablePoints, allocated);
    }
    
    /**
     * 从StatAllocationData创建数据包
     */
    public static SyncStatAllocationPacket fromData(StatAllocationData data) {
        Map<StatType, Integer> allocated = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            if (type.isAllocatable()) {
                allocated.put(type, data.getAllocatedPoints(type));
            }
        }
        return new SyncStatAllocationPacket(data.getAvailablePoints(), allocated);
    }
}
