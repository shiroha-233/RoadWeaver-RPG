package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import org.jetbrains.annotations.Nullable;

/**
 * 区域条件判定（坐标范围）
 */
public final class AreaCondition implements PlayerCondition<ConditionContext> {
    
    public enum AreaType {
        SPHERE,    // 球形
        CYLINDER,  // 圆柱形
        BOX        // 方形
    }
    
    private final AreaType type;
    private final BlockPos center;
    private final double radius;
    private final double verticalRadius;
    private final BlockPos minPos;
    private final BlockPos maxPos;
    @Nullable
    private final ResourceLocation dimension;
    
    private AreaCondition(AreaType type, BlockPos center, double radius, double verticalRadius,
                          BlockPos minPos, BlockPos maxPos, @Nullable ResourceLocation dimension) {
        this.type = type;
        this.center = center;
        this.radius = radius;
        this.verticalRadius = verticalRadius;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.dimension = dimension;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        // 检查维度
        if (dimension != null) {
            ResourceLocation playerDim = player.level().dimension().location();
            if (!playerDim.equals(dimension)) {
                return false;
            }
        }
        
        BlockPos pos = context != null && context.getPosition().isPresent()
                ? context.getPosition().get()
                : player.blockPosition();
        
        return switch (type) {
            case SPHERE -> checkSphere(pos);
            case CYLINDER -> checkCylinder(pos);
            case BOX -> checkBox(pos);
        };
    }
    
    private boolean checkSphere(BlockPos pos) {
        double distSq = pos.distSqr(center);
        return distSq <= radius * radius;
    }
    
    private boolean checkCylinder(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        int dy = Math.abs(pos.getY() - center.getY());
        return horizontalDistSq <= radius * radius && dy <= verticalRadius;
    }
    
    private boolean checkBox(BlockPos pos) {
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX()
                && pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY()
                && pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
    }
    
    // 工厂方法
    public static AreaCondition sphere(BlockPos center, double radius) {
        return sphere(center, radius, null);
    }
    
    public static AreaCondition sphere(BlockPos center, double radius, @Nullable ResourceLocation dimension) {
        return new AreaCondition(AreaType.SPHERE, center, radius, radius, null, null, dimension);
    }
    
    public static AreaCondition cylinder(BlockPos center, double horizontalRadius, double verticalRadius) {
        return cylinder(center, horizontalRadius, verticalRadius, null);
    }
    
    public static AreaCondition cylinder(BlockPos center, double horizontalRadius, double verticalRadius,
                                         @Nullable ResourceLocation dimension) {
        return new AreaCondition(AreaType.CYLINDER, center, horizontalRadius, verticalRadius, null, null, dimension);
    }
    
    public static AreaCondition box(BlockPos min, BlockPos max) {
        return box(min, max, null);
    }
    
    public static AreaCondition box(BlockPos min, BlockPos max, @Nullable ResourceLocation dimension) {
        return new AreaCondition(AreaType.BOX, BlockPos.ZERO, 0, 0, min, max, dimension);
    }
    
    public AreaType getType() {
        return type;
    }
    
    public BlockPos getCenter() {
        return center;
    }
    
    public double getRadius() {
        return radius;
    }
}
