package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 药水效果条件判定
 */
public final class EffectCondition implements PlayerCondition<ConditionContext> {
    
    private final ResourceLocation effectId;
    private final int minLevel;
    private final boolean checkLevel;
    
    private EffectCondition(ResourceLocation effectId, int minLevel, boolean checkLevel) {
        this.effectId = effectId;
        this.minLevel = minLevel;
        this.checkLevel = checkLevel;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) return false;
        
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null) return false;
        
        if (checkLevel) {
            return instance.getAmplifier() >= minLevel - 1; // amplifier从0开始
        }
        return true;
    }
    
    public static EffectCondition hasEffect(String effectId) {
        return new EffectCondition(new ResourceLocation(effectId), 0, false);
    }
    
    public static EffectCondition hasEffectLevel(String effectId, int minLevel) {
        return new EffectCondition(new ResourceLocation(effectId), minLevel, true);
    }
    
    public ResourceLocation getEffectId() {
        return effectId;
    }
}
