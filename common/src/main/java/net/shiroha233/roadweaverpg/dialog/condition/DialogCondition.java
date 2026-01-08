package net.shiroha233.roadweaverpg.dialog.condition;

import net.minecraft.server.level.ServerPlayer;

/**
 * 对话条件接口
 * 职责：定义对话行/选项的显示条件
 * 原理：策略模式 - 不同条件类型有不同实现
 */
@FunctionalInterface
public interface DialogCondition {
    
    /**
     * 评估条件是否满足
     * @param player 玩家
     * @param npcEntityId NPC实体ID
     * @return 条件是否满足
     */
    boolean evaluate(ServerPlayer player, int npcEntityId);
    
    /**
     * 始终为真的条件
     */
    DialogCondition ALWAYS = (player, npcEntityId) -> true;
    
    /**
     * 始终为假的条件
     */
    DialogCondition NEVER = (player, npcEntityId) -> false;
    
    /**
     * 组合条件：AND
     */
    default DialogCondition and(DialogCondition other) {
        return (player, npcEntityId) -> this.evaluate(player, npcEntityId) 
                && other.evaluate(player, npcEntityId);
    }
    
    /**
     * 组合条件：OR
     */
    default DialogCondition or(DialogCondition other) {
        return (player, npcEntityId) -> this.evaluate(player, npcEntityId) 
                || other.evaluate(player, npcEntityId);
    }
    
    /**
     * 取反条件
     */
    default DialogCondition negate() {
        return (player, npcEntityId) -> !this.evaluate(player, npcEntityId);
    }
}
