package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 商店购买请求数据包（客户端 -> 服务端）
 */
public record ShopPurchasePacket(int entityId, ResourceLocation itemId, int quantity) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeResourceLocation(itemId);
        buf.writeVarInt(quantity);
    }
    
    public static ShopPurchasePacket decode(FriendlyByteBuf buf) {
        return new ShopPurchasePacket(
                buf.readInt(),
                buf.readResourceLocation(),
                buf.readVarInt()
        );
    }
}
