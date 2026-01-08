package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 同步玩家冒险等级数据（服务端 -> 客户端）
 */
public record SyncPlayerAdventurePacket(int exp, int level) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(exp);
        buf.writeVarInt(level);
    }
    
    public static SyncPlayerAdventurePacket decode(FriendlyByteBuf buf) {
        int exp = buf.readVarInt();
        int level = buf.readVarInt();
        return new SyncPlayerAdventurePacket(exp, level);
    }
}
