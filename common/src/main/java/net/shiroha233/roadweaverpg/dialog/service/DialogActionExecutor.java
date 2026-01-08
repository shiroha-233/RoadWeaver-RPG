package net.shiroha233.roadweaverpg.dialog.service;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogAction;
import net.shiroha233.roadweaverpg.dialog.DialogActionRegistry;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.DialogSession;

/**
 * 对话动作执行器
 * 职责：执行对话选项的动作
 * 原理：单一职责原则，只负责动作执行和错误处理
 */
public class DialogActionExecutor {
    
    private static final DialogActionExecutor INSTANCE = new DialogActionExecutor();
    
    public static DialogActionExecutor getInstance() {
        return INSTANCE;
    }
    
    /**
     * 执行选项动作
     * @return 是否执行成功
     */
    public boolean execute(ServerPlayer player, DialogSession session, DialogData.DialogChoice choice) {
        DialogAction action = DialogActionRegistry.getAction(choice.action());
        try {
            action.execute(player, session, session.npcEntityId());
            return true;
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("执行对话动作失败: {}", choice.action(), e);
            sendErrorNotification(player, choice.action());
            return false;
        }
    }
    
    /**
     * 发送错误通知给客户端
     */
    private void sendErrorNotification(ServerPlayer player, String action) {
        try {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "gui.roadweaver_rpg.dialog.action_error", action
                ), true
            );
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("发送错误通知失败", e);
        }
    }
}
