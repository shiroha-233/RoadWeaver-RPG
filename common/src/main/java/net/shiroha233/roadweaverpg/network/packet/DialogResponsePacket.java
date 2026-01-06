package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 对话选项响应数据包（客户端 -> 服务端）
 */
public record DialogResponsePacket(int entityId, DialogOption option) {
    
    public enum DialogOption {
        WHO_ARE_YOU,      // 你是？
        SHOW_QUESTS,      // 有什么委托吗？
        COMPLETE_QUEST,   // 委托完成
        VIEW_REPUTATION,  // 查看声望
        RETRIEVE_SCROLL   // 找回丢失的委托书
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeEnum(option);
    }
    
    public static DialogResponsePacket decode(FriendlyByteBuf buf) {
        return new DialogResponsePacket(buf.readInt(), buf.readEnum(DialogOption.class));
    }
}
