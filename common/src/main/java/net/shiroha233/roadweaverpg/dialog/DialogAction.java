package net.shiroha233.roadweaverpg.dialog;

import net.minecraft.server.level.ServerPlayer;

/**
 * 对话动作接口
 * 职责：定义对话选项触发的动作
 * 原理：策略模式 - 不同动作类型有不同实现
 */
@FunctionalInterface
public interface DialogAction {
    
    /**
     * 执行动作
     * @param player 玩家
     * @param session 当前会话
     * @param npcEntityId NPC实体ID
     */
    void execute(ServerPlayer player, DialogSession session, int npcEntityId);
    
    /**
     * 空动作
     */
    DialogAction NONE = (player, session, npcEntityId) -> {};
    
    /**
     * 关闭对话 - 结束会话并通知客户端关闭界面
     */
    DialogAction CLOSE = (player, session, npcEntityId) -> 
            DialogManager.getInstance().endSession(player);
}
