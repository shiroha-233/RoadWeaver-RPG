package net.shiroha233.roadweaverpg.mixin.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.combat.CombatEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家攻击Mixin - 计算暴击伤害并显示真实扣血量
 */
@Mixin(LivingEntity.class)
public class PlayerAttackMixin {
    
    @Unique private DamageSource roadweaver$currentSource;
    @Unique private float roadweaver$healthBefore;
    @Unique private boolean roadweaver$isCritical;
    
    @Inject(method = "hurt", at = @At("HEAD"))
    private void roadweaver$captureHealthBefore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.roadweaver$currentSource = source;
        this.roadweaver$isCritical = false;
        LivingEntity self = (LivingEntity)(Object)this;
        this.roadweaver$healthBefore = self.getHealth();
    }
    
    @ModifyArg(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            index = 1
    )
    private float roadweaver$modifyDamage(float amount) {
        if (roadweaver$currentSource != null && roadweaver$currentSource.getEntity() instanceof ServerPlayer attacker) {
            var result = CombatEventHandler.calculateCriticalDamage(attacker, amount);
            this.roadweaver$isCritical = result.isCritical();
            return result.damage();
        }
        return amount;
    }
    
    @Inject(method = "hurt", at = @At("RETURN"))
    private void roadweaver$sendDamageIndicator(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (roadweaver$currentSource == null) return;
        if (!(roadweaver$currentSource.getEntity() instanceof ServerPlayer attacker)) return;
        
        LivingEntity self = (LivingEntity)(Object)this;
        float actualDamage = roadweaver$healthBefore - self.getHealth();
        
        if (actualDamage > 0) {
            CombatEventHandler.sendDamageIndicator(attacker, self, actualDamage, roadweaver$isCritical);
        }
    }
}
