package net.shiroha233.roadweaverpg.dialog.service;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.DialogSession;
import net.shiroha233.roadweaverpg.dialog.event.DialogChoiceEvent;
import net.shiroha233.roadweaverpg.dialog.event.DialogEndEvent;
import net.shiroha233.roadweaverpg.dialog.event.DialogStartEvent;
import net.shiroha233.roadweaverpg.quest.event.QuestEventBus;

import java.util.UUID;

/**
 * 对话事件发布器
 * 职责：发布对话相关事件到事件总线
 */
public class DialogEventPublisher {
    
    private static final DialogEventPublisher INSTANCE = new DialogEventPublisher();
    
    public static DialogEventPublisher getInstance() {
        return INSTANCE;
    }
    
    /**
     * 发布对话开始事件
     */
    public void publishStart(ServerPlayer player, int npcEntityId, DialogData dialog) {
        try {
            QuestEventBus.getInstance().publish(new DialogStartEvent(player, npcEntityId, dialog));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("对话开始事件发布失败", e);
        }
    }
    
    /**
     * 发布对话选择事件
     */
    public void publishChoice(ServerPlayer player, int npcEntityId, DialogData.DialogChoice choice) {
        try {
            QuestEventBus.getInstance().publish(new DialogChoiceEvent(player, npcEntityId, choice));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("对话选择事件发布失败", e);
        }
    }
    
    /**
     * 发布对话结束事件
     */
    public void publishEnd(UUID playerId, int npcEntityId, DialogSession.SessionState state) {
        try {
            QuestEventBus.getInstance().publish(new DialogEndEvent(playerId, npcEntityId, state));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("对话结束事件发布失败", e);
        }
    }
}
