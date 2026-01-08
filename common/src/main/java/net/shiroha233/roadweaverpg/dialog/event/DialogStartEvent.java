package net.shiroha233.roadweaverpg.dialog.event;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;

/**
 * 对话开始事件
 * 
 * 原理：使用统一的 QuestEventBus 发布对话事件，替代独立的 DialogEvents
 */
public class DialogStartEvent extends QuestEvent {
    
    private final ServerPlayer player;
    private final int npcEntityId;
    private final DialogData dialog;
    
    public DialogStartEvent(ServerPlayer player, int npcEntityId, DialogData dialog) {
        this.player = player;
        this.npcEntityId = npcEntityId;
        this.dialog = dialog;
    }
    
    public ServerPlayer getPlayer() {
        return player;
    }
    
    public int getNpcEntityId() {
        return npcEntityId;
    }
    
    public DialogData getDialog() {
        return dialog;
    }
}
