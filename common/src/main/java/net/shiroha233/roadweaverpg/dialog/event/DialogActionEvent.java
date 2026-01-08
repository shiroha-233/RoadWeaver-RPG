package net.shiroha233.roadweaverpg.dialog.event;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;

/**
 * 对话动作事件
 * 
 * 用于触发各种对话相关的动作（显示任务、打开商店等）
 */
public class DialogActionEvent extends QuestEvent {
    
    private final ActionType actionType;
    private final ServerPlayer player;
    private final int npcEntityId;
    
    public DialogActionEvent(ActionType actionType, ServerPlayer player, int npcEntityId) {
        this.actionType = actionType;
        this.player = player;
        this.npcEntityId = npcEntityId;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public ServerPlayer getPlayer() {
        return player;
    }
    
    public int getNpcEntityId() {
        return npcEntityId;
    }
    
    /**
     * 动作类型枚举
     */
    public enum ActionType {
        SHOW_QUESTS,        // 显示任务列表
        COMPLETE_QUEST,     // 完成任务
        RETRIEVE_SCROLL,    // 领取任务书
        VIEW_REPUTATION,    // 查看声望
        OPEN_SHOP           // 打开商店
    }
}
