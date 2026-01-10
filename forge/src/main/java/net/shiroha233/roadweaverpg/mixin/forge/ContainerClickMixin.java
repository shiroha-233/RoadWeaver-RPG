package net.shiroha233.roadweaverpg.mixin.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.item.CurrencyItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 容器点击Mixin - 实现在容器界面Shift+左键货币存入钱包
 * Forge 专用版本
 * 
 * 原理：Minecraft的Shift+左键会触发QUICK_MOVE类型
 * 我们拦截这个操作，当目标是货币时改为存入钱包
 */
@Mixin(AbstractContainerMenu.class)
public class ContainerClickMixin {
    
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        // Shift+左键触发QUICK_MOVE，button=0
        if (clickType != ClickType.QUICK_MOVE) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        
        // 检查槽位有效性
        if (slotId < 0 || slotId >= menu.slots.size()) return;
        
        ItemStack stack = menu.slots.get(slotId).getItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof CurrencyItem)) return;
        
        // 调用存入处理器
        var handler = CurrencyItem.getDepositHandler();
        if (handler != null) {
            handler.accept(serverPlayer, stack);
            ci.cancel();
        }
    }
}
