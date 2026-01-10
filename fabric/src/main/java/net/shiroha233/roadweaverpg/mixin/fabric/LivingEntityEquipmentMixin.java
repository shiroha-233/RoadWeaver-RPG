package net.shiroha233.roadweaverpg.mixin.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * 监听玩家装备变更事件
 * 当玩家切换手持物品时，重新同步攻击力到客户端
 * 原因：Minecraft的ATTACK_DAMAGE属性默认不同步到客户端
 */
@Mixin(LivingEntity.class)
public class LivingEntityEquipmentMixin {
    
    /**
     * 在装备变更处理完成后同步攻击力
     * handleEquipmentChanges 在属性修饰符应用之后被调用
     */
    @Inject(method = "handleEquipmentChanges", at = @At("TAIL"))
    private void roadweaver$onEquipmentChanged(Map<EquipmentSlot, ItemStack> changes, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        
        if (!(self instanceof ServerPlayer player)) return;
        
        // 检查是否有主手或副手变更
        boolean hasHandChange = changes.containsKey(EquipmentSlot.MAINHAND) || 
                                changes.containsKey(EquipmentSlot.OFFHAND);
        
        if (hasHandChange) {
            // 延迟1tick确保属性完全更新
            player.getServer().execute(() -> {
                net.shiroha233.roadweaverpg.stats.RpgStatsService.getInstance().syncRpgStats(player);
            });
        }
    }
}
