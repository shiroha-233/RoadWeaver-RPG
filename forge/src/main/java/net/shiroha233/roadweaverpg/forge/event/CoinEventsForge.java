package net.shiroha233.roadweaverpg.forge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.config.RoadWeaverConfig;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.forge.network.NetworkHandlerForge;
import net.shiroha233.roadweaverpg.item.CurrencyItem;
import net.shiroha233.roadweaverpg.wallet.WalletEventHandler;
import net.shiroha233.roadweaverpg.wallet.WalletService;

/**
 * Forge端货币事件处理
 * - Shift+左键存入钱包（通过CurrencyItem回调）
 * - 拾取货币自动存入钱包（可配置）
 * - 战利品表注入货币奖励（通过Fabric LootTableEvents.MODIFY事件）
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID)
public class CoinEventsForge {
    
    /**
     * 初始化货币事件处理（在模组初始化时调用）
     */
    public static void init() {
        // 设置CurrencyItem的存入处理器
        CurrencyItem.setDepositHandler((player, stack) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                long copperValue = WalletService.depositCurrencyStack(serverPlayer, stack);
                if (copperValue > 0) {
                    long total = WalletService.getCoins(serverPlayer);
                    NetworkHandlerForge.sendSyncWallet(serverPlayer, total, copperValue);
                }
            }
        });
        
        // 设置钱包同步回调（供Mixin使用）
        WalletEventHandler.setWalletSyncCallback((player, addedCopper) -> {
            long total = WalletService.getCoins(player);
            NetworkHandlerForge.sendSyncWallet(player, total, addedCopper);
        });
    }
    
    /**
     * 拾取货币时自动存入钱包（可配置）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        // 检查配置是否启用自动存入
        if (!RoadWeaverConfig.get().wallet.autoDepositOnPickup) return;
        
        Player player = event.getEntity();
        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        
        // 检查是否是货币
        if (!(stack.getItem() instanceof CurrencyItem currencyItem)) return;
        
        // 服务端处理
        if (player instanceof ServerPlayer serverPlayer) {
            long copperValue = CurrencyUtils.calculateCopperValue(
                    currencyItem.getCurrencyType(), stack.getCount());
            
            // 存入钱包
            WalletService.addCoins(serverPlayer, copperValue);
            
            // 同步到客户端
            long total = WalletService.getCoins(serverPlayer);
            NetworkHandlerForge.sendSyncWallet(serverPlayer, total, copperValue);
            
            // 清空物品实体，取消默认拾取行为
            stack.setCount(0);
            event.setCanceled(true);
        }
    }
}
