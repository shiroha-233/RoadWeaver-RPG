package net.shiroha233.roadweaverpg.common.exception;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.type.QuestState;

/**
 * 委托状态异常
 */
public class QuestStateException extends QuestException {
    
    private final QuestState currentState;
    private final QuestState expectedState;
    
    public QuestStateException(ResourceLocation questId, QuestState current, QuestState expected) {
        super(ErrorCode.QUEST_NOT_COMPLETED, questId,
              String.format("Quest %s is in state %s, expected %s", questId, current, expected));
        this.currentState = current;
        this.expectedState = expected;
    }
    
    public QuestStateException(ErrorCode code, ResourceLocation questId, String message) {
        super(code, questId, message);
        this.currentState = null;
        this.expectedState = null;
    }
    
    public QuestState getCurrentState() { return currentState; }
    public QuestState getExpectedState() { return expectedState; }
}
