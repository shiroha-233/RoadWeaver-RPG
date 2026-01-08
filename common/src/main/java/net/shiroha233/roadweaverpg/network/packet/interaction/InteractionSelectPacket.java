package net.shiroha233.roadweaverpg.network.packet.interaction;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 交互选择数据包（客户端 -> 服务端）
 * 职责：传输玩家选择的交互入口
 */
public record InteractionSelectPacket(int npcEntityId, String entryId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(npcEntityId);
        buf.writeUtf(entryId);
    }
    
    public static InteractionSelectPacket decode(FriendlyByteBuf buf) {
        return new InteractionSelectPacket(buf.readInt(), buf.readUtf());
    }
}
