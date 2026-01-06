package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 请求委托进度数据包（客户端 -> 服务端）
 * 用于右键委托书时请求最新进度
 */
public record RequestQuestProgressPacket(ResourceLocation questId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
    }
    
    public static RequestQuestProgressPacket decode(FriendlyByteBuf buf) {
        return new RequestQuestProgressPacket(buf.readResourceLocation());
    }
}
