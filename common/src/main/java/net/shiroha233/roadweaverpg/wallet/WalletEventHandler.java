package net.shiroha233.roadweaverpg.wallet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.item.CurrencyItem;

import java.util.function.BiConsumer;

/**
 * 钱包事件处理器 - 处理货币物品的自动存入和网络同步
 */
public final class WalletEventHandler {
    
    // 钱包同步回调（由平台特定代码设置）
    private static BiConsumer<ServerPlayer, Long> walletSyncCallback;
    
    private WalletEventHandler() {}
    
    /**
     * 设置钱包同步回调
     */
    public static void setWalletSyncCallback(BiConsumer<ServerPlayer, Long> callback) {
        walletSyncCallback = callback;
    }
    
    /**
     * 处理货币拾取事件（由Mixin调用）
     * @param copperValue 铜币价值
     * @return true 表示已处理
     */
    public static boolean onCurrencyPickup(ServerPlayer player, long copperValue) {
        if (copperValue <= 0) return false;
        
        long total = WalletService.addCoins(player, copperValue);
        
        // 同步到客户端
        if (walletSyncCallback != null) {
            walletSyncCallback.accept(player, copperValue);
        }
        
        RoadWeaverRPG.LOGGER.debug("Auto-deposited {} copper for {}, total: {}", 
                copperValue, player.getName().getString(), total);
        return true;
    }
    
    /**
     * 处理玩家拾取物品事件
     * 如果是货币物品，自动存入钱包
     * @return true 表示已处理（取消原始拾取），false 表示未处理
     */
    public static boolean onItemPickup(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CurrencyItem currencyItem)) {
            return false;
        }
        
        long copperValue = CurrencyUtils.calculateCopperValue(
                currencyItem.getCurrencyType(), stack.getCount());
        WalletService.addCoins(player, copperValue);
        stack.setCount(0);
        
        // 同步到客户端
        if (walletSyncCallback != null) {
            walletSyncCallback.accept(player, copperValue);
        }
        
        RoadWeaverRPG.LOGGER.debug("Auto-deposited {} copper for {}", 
                copperValue, player.getName().getString());
        return true;
    }
    
    /**
     * 处理玩家使用货币物品（Shift+右键）
     */
    public static void onCurrencyUse(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CurrencyItem)) {
            return;
        }
        
        long copperValue = WalletService.depositCurrencyStack(player, stack);
        if (copperValue > 0) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.roadweaver_rpg.currency_deposited", 
                            CurrencyUtils.formatCurrencyCompact(copperValue)), 
                    true);
            
            // 同步到客户端
            if (walletSyncCallback != null) {
                walletSyncCallback.accept(player, copperValue);
            }
        }
    }
    
    /**
     * 玩家登录时同步钱包数据
     */
    public static void onPlayerLogin(ServerPlayer player) {
        // 同步钱包数据到客户端（由平台特定代码调用）
    }
}
