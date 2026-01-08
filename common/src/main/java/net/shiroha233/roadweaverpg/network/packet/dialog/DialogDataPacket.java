package net.shiroha233.roadweaverpg.network.packet.dialog;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.dialog.DialogData;

/**
 * 完整对话数据包
 * 职责：传输完整的对话数据到客户端
 * 
 * 优化：添加同步版本号，用于验证客户端-服务器状态一致性
 */
public record DialogDataPacket(int npcEntityId, DialogData dialog, long syncVersion) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(npcEntityId);
        dialog.toNetwork(buf);
        buf.writeLong(syncVersion);
    }
    
    public static DialogDataPacket decode(FriendlyByteBuf buf) {
        int npcEntityId = buf.readInt();
        DialogData dialog = DialogData.fromNetwork(buf);
        long syncVersion = buf.readLong();
        return new DialogDataPacket(npcEntityId, dialog, syncVersion);
    }
}
