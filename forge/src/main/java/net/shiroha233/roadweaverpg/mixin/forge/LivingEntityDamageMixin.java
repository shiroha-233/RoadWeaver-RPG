package net.shiroha233.roadweaverpg.mixin.forge;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.shiroha233.roadweaverpg.worlddifficulty.DamageResistanceService;
import net.shiroha233.roadweaverpg.worlddifficulty.MonsterData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 怪物受伤Mixin (Forge版本)
 * 
 * 原理：
 * - 在怪物受到伤害时应用物理/魔法抗性
 * - 只影响有难度数据的怪物
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    
    /**
     * 修改实际受到的伤害值
     */
    @ModifyVariable(
            method = "hurt",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float roadweaver_rpg$modifyDamage(float damage, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        // 只处理Mob类型的实体
        if (!(self instanceof Mob)) {
            return damage;
        }
        
        // 检查是否有难度数据
        MonsterData data = MonsterData.fromEntity(self);
        if (data == null || !data.isScaled()) {
            return damage;
        }
        
        // 环境伤害不受抗性影响
        if (DamageResistanceService.isEnvironmentalDamage(source)) {
            return damage;
        }
        
        // 应用抗性减免
        return DamageResistanceService.calculateReducedDamage(self, source, damage);
    }
}
