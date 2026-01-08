package net.shiroha233.roadweaverpg.network.packet.dialog;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 对话推进数据包（客户端 -> 服务端）
 * 职责：通知服务端玩家请求下一行对话
 */
public record DialogAdvancePacket() {
    
    public void encode(FriendlyByteBuf buf) {
        // 无需额外数据
    }
    
    public static DialogAdvancePacket decode(FriendlyByteBuf buf) {
        return new DialogAdvancePacket();
    }
}
