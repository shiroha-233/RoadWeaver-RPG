package net.shiroha233.roadweaverpg.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.shiroha233.roadweaverpg.entity.npc.NPCDialogBubbleHandler;
import net.shiroha233.roadweaverpg.entity.npc.ShopMaidEntity;
import net.shiroha233.roadweaverpg.network.packet.shop.*;
import net.shiroha233.roadweaverpg.network.packet.sync.*;
import net.shiroha233.roadweaverpg.shop.ShopManager;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 商店数据包处理器 - 公共逻辑
 */
public final class ShopPacketHandler {
    
    private ShopPacketHandler() {}
    
    /**
     * 处理商店对话响应
     */
    public static void handleShopDialogResponse(ServerPlayer player, ShopDialogResponsePacket packet,
                                                 Consumer<ServerPlayer> openShopCallback) {
        Entity entity = player.level().getEntity(packet.entityId());
        if (!(entity instanceof ShopMaidEntity)) return;
        
        switch (packet.option()) {
            case OPEN_SHOP -> {
                NPCDialogBubbleHandler.handleShopMaidDialog(player, packet.entityId(), "OPEN_SHOP");
                openShopCallback.accept(player);
            }
            case CANCEL -> {} // 客户端已关闭界面
        }
    }
    
    /**
     * 处理购买请求 - 使用钱包系统
     */
    public static void handlePurchase(ServerPlayer player, ShopPurchasePacket packet,
                                       BiConsumer<ServerPlayer, SyncCoinsPacket> syncCoins) {
        Entity entity = player.level().getEntity(packet.entityId());
        if (!(entity instanceof ShopMaidEntity)) return;
        
        ShopManager.PurchaseResult result = ShopManager.getInstance()
                .purchase(player, packet.itemId(), packet.quantity());
        
        switch (result) {
            case SUCCESS -> {
                player.displayClientMessage(
                        Component.translatable("gui.roadweaver_rpg.shop.purchase_success"), false);
                // 同步钱包金币数量（使用int兼容旧接口）
                long coins = net.shiroha233.roadweaverpg.wallet.WalletService.getCoins(player);
                syncCoins.accept(player, new SyncCoinsPacket((int) Math.min(coins, Integer.MAX_VALUE)));
            }
            case INSUFFICIENT_COINS -> player.displayClientMessage(
                    Component.translatable("gui.roadweaver_rpg.shop.insufficient_coins"), false);
            case ITEM_NOT_FOUND -> player.displayClientMessage(
                    Component.translatable("gui.roadweaver_rpg.shop.item_not_found"), false);
            case INSUFFICIENT_LEVEL -> player.displayClientMessage(
                    Component.translatable("gui.roadweaver_rpg.shop.insufficient_level"), false);
            default -> {}
        }
    }
    
    /**
     * 创建打开商店数据包 - 使用钱包系统
     */
    public static OpenShopPacket createOpenShopPacket(ServerPlayer player, int entityId) {
        return new OpenShopPacket(
                entityId,
                ShopManager.getInstance().getAllItems(),
                net.shiroha233.roadweaverpg.wallet.WalletService.getCoins(player)
        );
    }
}
