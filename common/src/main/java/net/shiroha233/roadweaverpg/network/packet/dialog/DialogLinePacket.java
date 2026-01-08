package net.shiroha233.roadweaverpg.network.packet.dialog;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.dialog.DialogData;

/**
 * 单行对话数据包（服务端 -> 客户端）
 * 职责：传输单行对话内容，用于对话推进
 */
public record DialogLinePacket(int npcEntityId, DialogData.DialogLine line, int lineIndex) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(npcEntityId);
        line.toNetwork(buf);
        buf.writeVarInt(lineIndex);
    }
    
    public static DialogLinePacket decode(FriendlyByteBuf buf) {
        int npcEntityId = buf.readInt();
        DialogData.DialogLine line = DialogData.DialogLine.fromNetwork(buf);
        int lineIndex = buf.readVarInt();
        return new DialogLinePacket(npcEntityId, line, lineIndex);
    }
}
