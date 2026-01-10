package net.shiroha233.roadweaverpg.mixin.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.config.RoadWeaverConfig;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.item.CurrencyItem;
import net.shiroha233.roadweaverpg.wallet.WalletEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家物品拾取Mixin - 拦截货币拾取，直接存入钱包（可配置）
 * Forge 专用版本
 */
@Mixin(ItemEntity.class)
public class PlayerItemPickupMixin {
    
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // 检查配置是否启用自动存入
        if (!RoadWeaverConfig.get().wallet.autoDepositOnPickup) return;
        
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        
        // 检查是否是货币
        if (!(stack.getItem() instanceof CurrencyItem currencyItem)) return;
        
        // 检查拾取延迟
        if (self.hasPickUpDelay()) return;
        
        // 计算铜币价值并存入钱包
        long copperValue = CurrencyUtils.calculateCopperValue(
                currencyItem.getCurrencyType(), stack.getCount());
        if (WalletEventHandler.onCurrencyPickup(serverPlayer, copperValue)) {
            // 移除物品实体
            self.discard();
            ci.cancel();
        }
    }
}
