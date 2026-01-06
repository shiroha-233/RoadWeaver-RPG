package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 同步玩家金币数量数据包（服务端 -> 客户端）
 */
public record SyncCoinsPacket(int coins) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(coins);
    }
    
    public static SyncCoinsPacket decode(FriendlyByteBuf buf) {
        return new SyncCoinsPacket(buf.readVarInt());
    }
}
