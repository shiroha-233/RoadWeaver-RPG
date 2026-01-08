package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 高度条件判定
 */
public final class HeightCondition implements PlayerCondition<ConditionContext> {
    
    private final int minY;
    private final int maxY;
    
    public HeightCondition(int minY, int maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        BlockPos pos = context != null && context.getPosition().isPresent()
                ? context.getPosition().get()
                : player.blockPosition();
        
        int y = pos.getY();
        return y >= minY && y <= maxY;
    }
    
    public static HeightCondition range(int minY, int maxY) {
        return new HeightCondition(minY, maxY);
    }
    
    public static HeightCondition minY(int minY) {
        return new HeightCondition(minY, 320);
    }
    
    public static HeightCondition maxY(int maxY) {
        return new HeightCondition(-64, maxY);
    }
    
    public static HeightCondition underground() {
        return new HeightCondition(-64, 62);
    }
    
    public static HeightCondition surface() {
        return new HeightCondition(63, 320);
    }
    
    public int getMinY() {
        return minY;
    }
    
    public int getMaxY() {
        return maxY;
    }
}
