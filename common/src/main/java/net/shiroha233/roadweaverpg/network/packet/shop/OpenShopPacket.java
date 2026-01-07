package net.shiroha233.roadweaverpg.network.packet.shop;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.shop.ShopItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 打开商店界面数据包（服务端 -> 客户端）
 */
public record OpenShopPacket(int entityId, List<ShopItem> items, int playerCoins) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(items.size());
        for (ShopItem item : items) {
            item.toNetwork(buf);
        }
        buf.writeVarInt(playerCoins);
    }
    
    public static OpenShopPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int size = buf.readVarInt();
        List<ShopItem> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(ShopItem.fromNetwork(buf));
        }
        int playerCoins = buf.readVarInt();
        return new OpenShopPacket(entityId, items, playerCoins);
    }
}
