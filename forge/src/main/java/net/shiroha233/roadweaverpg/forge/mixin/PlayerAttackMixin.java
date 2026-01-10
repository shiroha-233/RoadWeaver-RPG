package net.shiroha233.roadweaverpg.forge.mixin;

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
 * 玩家攻击Mixin - 拦截hurt方法中的actuallyHurt调用
 */
@Mixin(LivingEntity.class)
public class PlayerAttackMixin {
    
    @Unique
    private DamageSource roadweaver$currentSource;
    
    @Inject(method = "hurt", at = @At("HEAD"))
    private void roadweaver$captureSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.roadweaver$currentSource = source;
    }
    
    @ModifyArg(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            index = 1
    )
    private float roadweaver$modifyDamage(float amount) {
        if (roadweaver$currentSource != null && roadweaver$currentSource.getEntity() instanceof ServerPlayer attacker) {
            LivingEntity self = (LivingEntity)(Object)this;
            return CombatEventHandler.onPlayerAttack(attacker, self, amount);
        }
        return amount;
    }
}
