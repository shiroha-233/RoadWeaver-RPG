package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 天气条件判定 - 使用单例缓存优化
 */
public final class WeatherCondition implements PlayerCondition<ConditionContext> {
    
    public enum WeatherType {
        CLEAR, RAIN, THUNDER, RAIN_AT
    }
    
    // 单例缓存
    private static final WeatherCondition CLEAR_INSTANCE = new WeatherCondition(WeatherType.CLEAR);
    private static final WeatherCondition RAIN_INSTANCE = new WeatherCondition(WeatherType.RAIN);
    private static final WeatherCondition THUNDER_INSTANCE = new WeatherCondition(WeatherType.THUNDER);
    private static final WeatherCondition RAIN_AT_INSTANCE = new WeatherCondition(WeatherType.RAIN_AT);
    
    private final WeatherType type;
    
    private WeatherCondition(WeatherType type) {
        this.type = type;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        ServerLevel level = player.serverLevel();
        
        return switch (type) {
            case CLEAR -> !level.isRaining();
            case RAIN -> level.isRaining();
            case THUNDER -> level.isThundering();
            case RAIN_AT -> level.isRaining() && level.isRainingAt(player.blockPosition());
        };
    }
    
    public static WeatherCondition clear() { return CLEAR_INSTANCE; }
    public static WeatherCondition rain() { return RAIN_INSTANCE; }
    public static WeatherCondition thunder() { return THUNDER_INSTANCE; }
    public static WeatherCondition rainAt() { return RAIN_AT_INSTANCE; }
    
    public WeatherType getType() { return type; }
}
