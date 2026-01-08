package net.shiroha233.roadweaverpg.dialog.event;

import net.shiroha233.roadweaverpg.dialog.DialogSession;
import net.shiroha233.roadweaverpg.quest.event.QuestEvent;

import java.util.UUID;

/**
 * 对话结束事件
 * 
 * 原理：使用 UUID 而非 ServerPlayer，因为玩家可能已断线
 */
public class DialogEndEvent extends QuestEvent {
    
    private final UUID playerId;
    private final int npcEntityId;
    private final DialogSession.SessionState endState;
    
    public DialogEndEvent(UUID playerId, int npcEntityId, DialogSession.SessionState endState) {
        this.playerId = playerId;
        this.npcEntityId = npcEntityId;
        this.endState = endState;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getNpcEntityId() {
        return npcEntityId;
    }
    
    public DialogSession.SessionState getEndState() {
        return endState;
    }
}
