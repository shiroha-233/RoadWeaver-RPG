package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 打开对话选项界面的数据包
 */
public record OpenDialogPacket(int entityId) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }
    
    public static OpenDialogPacket decode(FriendlyByteBuf buf) {
        return new OpenDialogPacket(buf.readInt());
    }
}
