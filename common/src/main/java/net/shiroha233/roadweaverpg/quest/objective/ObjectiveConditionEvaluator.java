package net.shiroha233.roadweaverpg.quest.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

import java.util.List;

/**
 * 目标条件评估器
 * 
 * 设计原理：
 * - 统一处理所有目标的条件评估逻辑
 * - 将条件系统与目标系统解耦
 * - 提供标准化的上下文构建
 * - 支持短路评估优化性能
 */
public final class ObjectiveConditionEvaluator {
    
    private ObjectiveConditionEvaluator() {}
    
    /**
     * 评估所有条件是否满足（短路评估）
     * 
     * @param player 玩家
     * @param conditions 条件列表
     * @param context 评估上下文
     * @return 所有条件是否都满足
     */
    public static boolean evaluateAll(ServerPlayer player, 
                                       List<PlayerCondition<ConditionContext>> conditions,
                                       ConditionContext context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (PlayerCondition<ConditionContext> condition : conditions) {
            if (!condition.evaluate(player, context)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 评估任一条件是否满足
     */
    public static boolean evaluateAny(ServerPlayer player,
                                       List<PlayerCondition<ConditionContext>> conditions,
                                       ConditionContext context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (PlayerCondition<ConditionContext> condition : conditions) {
            if (condition.evaluate(player, context)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== 上下文构建器 ====================
    
    /**
     * 为玩家位置构建上下文
     */
    public static ConditionContext buildPlayerContext(ServerPlayer player) {
        return ConditionContext.builder()
                .position(player.blockPosition())
                .dimension(player.level().dimension().location())
                .build();
    }
    
    /**
     * 为实体击杀事件构建上下文
     */
    public static ConditionContext buildKillContext(ServerPlayer player, Entity victim) {
        BlockPos entityPos = victim.blockPosition();
        return ConditionContext.builder()
                .position(entityPos)
                .targetEntity(victim)
                .dimension(player.level().dimension().location())
                .build();
    }
    
    /**
     * 为指定位置构建上下文
     */
    public static ConditionContext buildPositionContext(ServerPlayer player, BlockPos position) {
        return ConditionContext.builder()
                .position(position)
                .dimension(player.level().dimension().location())
                .build();
    }
    
    /**
     * 为NPC交互构建上下文
     */
    public static ConditionContext buildNpcContext(ServerPlayer player, int npcEntityId) {
        return ConditionContext.builder()
                .position(player.blockPosition())
                .dimension(player.level().dimension().location())
                .npcEntityId(npcEntityId)
                .build();
    }
    
    /**
     * 为自定义数据构建上下文
     */
    public static ConditionContext buildCustomContext(ServerPlayer player, 
                                                       BlockPos position,
                                                       Entity targetEntity,
                                                       ResourceLocation dimension) {
        ConditionContext.Builder builder = ConditionContext.builder();
        
        if (position != null) {
            builder.position(position);
        } else if (player != null) {
            builder.position(player.blockPosition());
        }
        
        if (targetEntity != null) {
            builder.targetEntity(targetEntity);
        }
        
        if (dimension != null) {
            builder.dimension(dimension);
        } else if (player != null) {
            builder.dimension(player.level().dimension().location());
        }
        
        return builder.build();
    }
}
