package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 维度条件判定
 * 
 * 性能优化：使用单例缓存常用维度条件
 */
public final class DimensionCondition implements PlayerCondition<ConditionContext> {
    
    // 单例缓存
    private static final ConcurrentHashMap<ResourceLocation, DimensionCondition> CACHE = new ConcurrentHashMap<>();
    private static final DimensionCondition OVERWORLD = new DimensionCondition(Level.OVERWORLD.location());
    private static final DimensionCondition NETHER = new DimensionCondition(Level.NETHER.location());
    private static final DimensionCondition END = new DimensionCondition(Level.END.location());
    
    private final ResourceLocation dimension;
    
    private DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        return player.level().dimension().location().equals(dimension);
    }
    
    public static DimensionCondition overworld() {
        return OVERWORLD;
    }
    
    public static DimensionCondition nether() {
        return NETHER;
    }
    
    public static DimensionCondition end() {
        return END;
    }
    
    public static DimensionCondition of(String dimension) {
        ResourceLocation loc = new ResourceLocation(dimension);
        return CACHE.computeIfAbsent(loc, DimensionCondition::new);
    }
    
    public ResourceLocation getDimension() {
        return dimension;
    }
}
