package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 生命值条件判定
 */
public final class HealthCondition implements PlayerCondition<ConditionContext> {
    
    public enum CompareType {
        AT_LEAST,
        AT_MOST,
        PERCENT_AT_LEAST,
        PERCENT_AT_MOST
    }
    
    private final CompareType type;
    private final float value;
    
    private HealthCondition(CompareType type, float value) {
        this.type = type;
        this.value = value;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        
        return switch (type) {
            case AT_LEAST -> health >= value;
            case AT_MOST -> health <= value;
            case PERCENT_AT_LEAST -> (health / maxHealth) >= value;
            case PERCENT_AT_MOST -> (health / maxHealth) <= value;
        };
    }
    
    public static HealthCondition atLeast(float health) {
        return new HealthCondition(CompareType.AT_LEAST, health);
    }
    
    public static HealthCondition atMost(float health) {
        return new HealthCondition(CompareType.AT_MOST, health);
    }
    
    public static HealthCondition percentAtLeast(float percent) {
        return new HealthCondition(CompareType.PERCENT_AT_LEAST, percent);
    }
    
    public static HealthCondition percentAtMost(float percent) {
        return new HealthCondition(CompareType.PERCENT_AT_MOST, percent);
    }
    
    public CompareType getType() {
        return type;
    }
    
    public float getValue() {
        return value;
    }
}
