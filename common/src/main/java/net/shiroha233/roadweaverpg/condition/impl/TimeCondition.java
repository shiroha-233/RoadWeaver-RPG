package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 时间条件判定
 * 
 * MC时间：0-24000 tick
 * 性能优化：使用单例缓存常用时间条件
 */
public final class TimeCondition implements PlayerCondition<ConditionContext> {
    
    public enum TimeType {
        DAY, NIGHT, DAWN, DUSK, RANGE
    }
    
    // 单例缓存
    private static final TimeCondition DAY_INSTANCE = new TimeCondition(TimeType.DAY, 0, 12000);
    private static final TimeCondition NIGHT_INSTANCE = new TimeCondition(TimeType.NIGHT, 12000, 24000);
    private static final TimeCondition DAWN_INSTANCE = new TimeCondition(TimeType.DAWN, 22000, 1000);
    private static final TimeCondition DUSK_INSTANCE = new TimeCondition(TimeType.DUSK, 11000, 13000);
    
    private final TimeType type;
    private final long startTick;
    private final long endTick;
    
    private TimeCondition(TimeType type, long startTick, long endTick) {
        this.type = type;
        this.startTick = startTick;
        this.endTick = endTick;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        long time = player.serverLevel().getDayTime() % 24000;
        
        return switch (type) {
            case DAY -> time < 12000;
            case NIGHT -> time >= 12000;
            case DAWN -> time >= 22000 || time < 1000;
            case DUSK -> time >= 11000 && time < 13000;
            case RANGE -> checkRange(time);
        };
    }
    
    private boolean checkRange(long time) {
        if (startTick <= endTick) {
            return time >= startTick && time < endTick;
        }
        return time >= startTick || time < endTick;
    }
    
    public static TimeCondition day() { return DAY_INSTANCE; }
    public static TimeCondition night() { return NIGHT_INSTANCE; }
    public static TimeCondition dawn() { return DAWN_INSTANCE; }
    public static TimeCondition dusk() { return DUSK_INSTANCE; }
    
    public static TimeCondition range(long start, long end) {
        return new TimeCondition(TimeType.RANGE, start, end);
    }
    
    public TimeType getType() { return type; }
}
