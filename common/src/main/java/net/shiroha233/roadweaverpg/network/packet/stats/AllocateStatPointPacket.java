package net.shiroha233.roadweaverpg.network.packet.stats;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.stats.StatType;

/**
 * 分配技能点请求（客户端 -> 服务端）
 */
public record AllocateStatPointPacket(String statTypeId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(statTypeId);
    }
    
    public static AllocateStatPointPacket decode(FriendlyByteBuf buf) {
        return new AllocateStatPointPacket(buf.readUtf());
    }
    
    /**
     * 获取属性类型
     */
    public StatType getStatType() {
        return StatType.fromId(statTypeId);
    }
}
