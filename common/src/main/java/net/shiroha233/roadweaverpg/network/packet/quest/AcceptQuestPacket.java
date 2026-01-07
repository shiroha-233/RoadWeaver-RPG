package net.shiroha233.roadweaverpg.network.packet.quest;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 接受委托数据包（客户端 -> 服务端）
 */
public record AcceptQuestPacket(ResourceLocation questId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
    }
    
    public static AcceptQuestPacket decode(FriendlyByteBuf buf) {
        return new AcceptQuestPacket(buf.readResourceLocation());
    }
}
