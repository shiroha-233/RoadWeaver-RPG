package net.shiroha233.roadweaverpg.network.packet.quest;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 提交委托数据包（客户端 -> 服务端）
 * 玩家手持已完成的委托书与公会女仆交互时发送
 */
public record TurnInQuestPacket(ResourceLocation questId, int entityId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeInt(entityId);
    }
    
    public static TurnInQuestPacket decode(FriendlyByteBuf buf) {
        return new TurnInQuestPacket(buf.readResourceLocation(), buf.readInt());
    }
}
