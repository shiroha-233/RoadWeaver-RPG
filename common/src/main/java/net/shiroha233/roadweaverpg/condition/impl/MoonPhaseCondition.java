package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 月相条件判定 - 使用缓存优化
 * 月相：0-7（0=满月，4=新月）
 */
public final class MoonPhaseCondition implements PlayerCondition<ConditionContext> {
    
    // 缓存（月相只有8种）
    private static final ConcurrentHashMap<Integer, MoonPhaseCondition> CACHE = new ConcurrentHashMap<>();
    private static final MoonPhaseCondition FULL_MOON = new MoonPhaseCondition(0);
    private static final MoonPhaseCondition NEW_MOON = new MoonPhaseCondition(4);
    
    private final int phase;
    
    private MoonPhaseCondition(int phase) {
        this.phase = phase;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        return player.serverLevel().getMoonPhase() == phase;
    }
    
    public static MoonPhaseCondition fullMoon() { return FULL_MOON; }
    public static MoonPhaseCondition newMoon() { return NEW_MOON; }
    
    public static MoonPhaseCondition phase(int phase) {
        return CACHE.computeIfAbsent(phase, MoonPhaseCondition::new);
    }
    
    public int getPhase() { return phase; }
}
