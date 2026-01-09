package net.shiroha233.roadweaverpg.network.packet.stats;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.stats.StatType;

/**
 * 减少技能点请求（客户端 -> 服务端）
 */
public record DeallocateStatPointPacket(String statTypeId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(statTypeId);
    }
    
    public static DeallocateStatPointPacket decode(FriendlyByteBuf buf) {
        return new DeallocateStatPointPacket(buf.readUtf());
    }
    
    public StatType getStatType() {
        return StatType.fromId(statTypeId);
    }
}
