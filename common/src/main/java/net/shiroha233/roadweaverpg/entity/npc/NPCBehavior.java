package net.shiroha233.roadweaverpg.entity.npc;

import net.minecraft.server.level.ServerPlayer;

/**
 * NPC行为接口
 * 职责：定义NPC的可选行为（如反击、对话等）
 * 原理：接口隔离原则 - 不强制所有NPC实现所有行为
 */
public interface NPCBehavior {
    
    /**
     * 反击行为接口
     */
    interface Counterable {
        /**
         * 处理反击逻辑
         * @param attacker 攻击者
         * @param hitCount 累计击打次数
         */
        void handleCounterAttack(ServerPlayer attacker, int hitCount);
        
        /**
         * 是否启用反击
         */
        boolean isCounterAttackEnabled();
    }
    
    /**
     * 对话行为接口
     */
    interface Dialogable {
        /**
         * 打开对话界面
         * @param player 玩家
         */
        void openDialog(ServerPlayer player);
        
        /**
         * 获取对话选项
         */
        java.util.List<DialogOption> getDialogOptions(ServerPlayer player);
    }
    
    /**
     * 不可移动行为接口
     */
    interface Immovable {
        /**
         * 是否完全禁止移动
         */
        boolean isCompletelyImmovable();
        
        /**
         * 是否允许重力
         */
        boolean allowGravity();
    }
    
    /**
     * 对话选项数据
     */
    record DialogOption(String id, net.minecraft.network.chat.Component text) {}
}
