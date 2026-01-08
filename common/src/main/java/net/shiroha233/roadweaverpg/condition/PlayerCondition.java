package net.shiroha233.roadweaverpg.condition;

import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家条件接口
 * 
 * 设计原理：
 * - 统一的条件判定接口，可被对话系统、任务系统、NPC交互等复用
 * - 策略模式：不同条件类型有不同实现
 * - 支持条件组合（AND/OR/NOT）
 * - 线程安全：条件评估不修改任何状态
 * 
 * @param <C> 上下文类型，用于传递额外信息
 */
@FunctionalInterface
public interface PlayerCondition<C> {
    
    /**
     * 评估条件是否满足
     * @param player 玩家
     * @param context 上下文信息（可为null）
     * @return 条件是否满足
     */
    boolean evaluate(ServerPlayer player, C context);
    
    // ==================== 常量条件 ====================
    
    /** 始终为真 */
    static <C> PlayerCondition<C> always() {
        return (player, ctx) -> true;
    }
    
    /** 始终为假 */
    static <C> PlayerCondition<C> never() {
        return (player, ctx) -> false;
    }
    
    // ==================== 条件组合 ====================
    
    /** AND组合 */
    default PlayerCondition<C> and(PlayerCondition<C> other) {
        return (player, ctx) -> this.evaluate(player, ctx) && other.evaluate(player, ctx);
    }
    
    /** OR组合 */
    default PlayerCondition<C> or(PlayerCondition<C> other) {
        return (player, ctx) -> this.evaluate(player, ctx) || other.evaluate(player, ctx);
    }
    
    /** 取反 */
    default PlayerCondition<C> negate() {
        return (player, ctx) -> !this.evaluate(player, ctx);
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 无上下文评估
     */
    default boolean evaluate(ServerPlayer player) {
        return evaluate(player, null);
    }
    
    /**
     * 转换为无上下文条件
     */
    default PlayerCondition<Void> withoutContext() {
        return (player, ctx) -> this.evaluate(player, null);
    }
}
