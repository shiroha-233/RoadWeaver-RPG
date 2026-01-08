package net.shiroha233.roadweaverpg.event;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.item.CurrencyItem;
import net.shiroha233.roadweaverpg.network.NetworkHandlerFabric;
import net.shiroha233.roadweaverpg.wallet.WalletEventHandler;
import net.shiroha233.roadweaverpg.wallet.WalletService;

/**
 * Fabric端货币事件处理
 * - Shift+右键存入钱包
 * - 拾取货币自动存入钱包（通过Mixin）
 */
public final class CoinEventsFabric {
    
    private CoinEventsFabric() {}
    
    public static void register() {
        // 设置CurrencyItem的存入处理器
        CurrencyItem.setDepositHandler((player, stack) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                long copperValue = WalletService.depositCurrencyStack(serverPlayer, stack);
                if (copperValue > 0) {
                    long total = WalletService.getCoins(serverPlayer);
                    NetworkHandlerFabric.sendSyncWallet(serverPlayer, total, copperValue);
                }
            }
        });
        
        // 设置钱包同步回调（供Mixin使用）
        WalletEventHandler.setWalletSyncCallback((player, addedCopper) -> {
            long total = WalletService.getCoins(player);
            NetworkHandlerFabric.sendSyncWallet(player, total, addedCopper);
        });
    }
}
