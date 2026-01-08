package net.shiroha233.roadweaverpg.dialog.event;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;

/**
 * 对话选择事件
 */
public class DialogChoiceEvent extends QuestEvent {
    
    private final ServerPlayer player;
    private final int npcEntityId;
    private final DialogData.DialogChoice choice;
    
    public DialogChoiceEvent(ServerPlayer player, int npcEntityId, DialogData.DialogChoice choice) {
        this.player = player;
        this.npcEntityId = npcEntityId;
        this.choice = choice;
    }
    
    public ServerPlayer getPlayer() {
        return player;
    }
    
    public int getNpcEntityId() {
        return npcEntityId;
    }
    
    public DialogData.DialogChoice getChoice() {
        return choice;
    }
}
