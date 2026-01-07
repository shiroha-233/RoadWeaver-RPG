package net.shiroha233.roadweaverpg.network.packet.shop;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 商店对话响应数据包（客户端 -> 服务端）
 */
public record ShopDialogResponsePacket(int entityId, ShopDialogOption option) {
    
    public enum ShopDialogOption {
        OPEN_SHOP,
        CANCEL
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeEnum(option);
    }
    
    public static ShopDialogResponsePacket decode(FriendlyByteBuf buf) {
        return new ShopDialogResponsePacket(buf.readInt(), buf.readEnum(ShopDialogOption.class));
    }
}
