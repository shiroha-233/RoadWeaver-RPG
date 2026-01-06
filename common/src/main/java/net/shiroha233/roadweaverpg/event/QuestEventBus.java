package net.shiroha233.roadweaverpg.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 委托事件总线
 * 提供事件驱动的通知机制
 */
public class QuestEventBus {
    
    private static QuestEventBus instance;
    
    private final Map<QuestEventType, List<BiConsumer<ServerPlayer, Object>>> listeners = new ConcurrentHashMap<>();
    
    private QuestEventBus() {
        for (QuestEventType type : QuestEventType.values()) {
            listeners.put(type, new ArrayList<>());
        }
    }
    
    public static QuestEventBus getInstance() {
        if (instance == null) {
            instance = new QuestEventBus();
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    public <T> void register(QuestEventType type, BiConsumer<ServerPlayer, T> listener) {
        listeners.get(type).add((BiConsumer<ServerPlayer, Object>) listener);
    }
    
    public void publish(QuestEventType type, ServerPlayer player, Object data) {
        List<BiConsumer<ServerPlayer, Object>> typeListeners = listeners.get(type);
        if (typeListeners == null || typeListeners.isEmpty()) return;
        
        for (BiConsumer<ServerPlayer, Object> listener : typeListeners) {
            try {
                listener.accept(player, data);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Error in quest event listener for {}", type, e);
            }
        }
    }
    
    public void clear() {
        listeners.values().forEach(List::clear);
    }
    
    /** 事件类型 */
    public enum QuestEventType {
        QUEST_ACCEPTED,
        QUEST_ABANDONED,
        QUEST_PROGRESS,
        QUEST_COMPLETED,
        QUEST_TURNED_IN,
        QUEST_EXPIRED,
        QUEST_FAILED,
        REPUTATION_CHANGED,
        REPUTATION_LEVEL_UP
    }
    
    /** 委托事件数据 */
    public record QuestEventData(ResourceLocation questId, QuestInstance instance) {}
    
    /** 声望事件数据 */
    public record ReputationEventData(ResourceLocation factionId, int oldValue, int newValue) {}
}
