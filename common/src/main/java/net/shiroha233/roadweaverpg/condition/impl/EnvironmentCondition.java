package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 环境条件判定 - 使用单例缓存优化
 */
public final class EnvironmentCondition implements PlayerCondition<ConditionContext> {
    
    public enum EnvType {
        IN_WATER, IN_LAVA, ON_FIRE, UNDER_SKY, UNDERGROUND
    }
    
    // 单例缓存
    private static final EnvironmentCondition IN_WATER_INSTANCE = new EnvironmentCondition(EnvType.IN_WATER);
    private static final EnvironmentCondition IN_LAVA_INSTANCE = new EnvironmentCondition(EnvType.IN_LAVA);
    private static final EnvironmentCondition ON_FIRE_INSTANCE = new EnvironmentCondition(EnvType.ON_FIRE);
    private static final EnvironmentCondition UNDER_SKY_INSTANCE = new EnvironmentCondition(EnvType.UNDER_SKY);
    private static final EnvironmentCondition UNDERGROUND_INSTANCE = new EnvironmentCondition(EnvType.UNDERGROUND);
    
    private final EnvType type;
    
    private EnvironmentCondition(EnvType type) {
        this.type = type;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        return switch (type) {
            case IN_WATER -> player.isInWater();
            case IN_LAVA -> player.isInLava();
            case ON_FIRE -> player.isOnFire();
            case UNDER_SKY -> player.serverLevel().canSeeSky(player.blockPosition());
            case UNDERGROUND -> player.blockPosition().getY() < player.serverLevel().getSeaLevel();
        };
    }
    
    public static EnvironmentCondition inWater() { return IN_WATER_INSTANCE; }
    public static EnvironmentCondition inLava() { return IN_LAVA_INSTANCE; }
    public static EnvironmentCondition onFire() { return ON_FIRE_INSTANCE; }
    public static EnvironmentCondition underSky() { return UNDER_SKY_INSTANCE; }
    public static EnvironmentCondition underground() { return UNDERGROUND_INSTANCE; }
    
    public EnvType getType() { return type; }
}
