package net.shiroha233.roadweaverpg.network.packet.dialog;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 对话选择数据包（客户端 -> 服务端）
 * 职责：传输玩家的对话选择
 * 
 * 优化：添加同步版本号，用于服务端验证客户端状态
 */
public record DialogChoicePacket(String choiceId, long syncVersion) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(choiceId);
        buf.writeLong(syncVersion);
    }
    
    public static DialogChoicePacket decode(FriendlyByteBuf buf) {
        String choiceId = buf.readUtf();
        long syncVersion = buf.readLong();
        return new DialogChoicePacket(choiceId, syncVersion);
    }
}
