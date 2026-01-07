package net.shiroha233.roadweaverpg.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * 委托事件总线
 * 提供事件驱动的通知机制
 * 
 * 线程安全：
 * - 使用 CopyOnWriteArrayList 保证遍历时的线程安全
 * - 支持注册和注销监听器
 */
public class QuestEventBus {
    
    private static volatile QuestEventBus instance;
    private static final Object LOCK = new Object();
    
    // 使用 CopyOnWriteArrayList 保证线程安全
    private final Map<QuestEventType, List<BiConsumer<ServerPlayer, Object>>> listeners = new ConcurrentHashMap<>();
    
    private QuestEventBus() {
        for (QuestEventType type : QuestEventType.values()) {
            listeners.put(type, new CopyOnWriteArrayList<>());
        }
    }
    
    public static QuestEventBus getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new QuestEventBus();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册事件监听器
     */
    @SuppressWarnings("unchecked")
    public <T> void register(QuestEventType type, BiConsumer<ServerPlayer, T> listener) {
        listeners.get(type).add((BiConsumer<ServerPlayer, Object>) listener);
    }
    
    /**
     * 注销事件监听器
     */
    @SuppressWarnings("unchecked")
    public <T> void unregister(QuestEventType type, BiConsumer<ServerPlayer, T> listener) {
        listeners.get(type).remove((BiConsumer<ServerPlayer, Object>) listener);
    }
    
    /**
     * 发布事件（线程安全）
     */
    public void publish(QuestEventType type, ServerPlayer player, Object data) {
        List<BiConsumer<ServerPlayer, Object>> typeListeners = listeners.get(type);
        if (typeListeners == null || typeListeners.isEmpty()) return;
        
        // CopyOnWriteArrayList 保证遍历时的线程安全
        for (BiConsumer<ServerPlayer, Object> listener : typeListeners) {
            try {
                listener.accept(player, data);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Error in quest event listener for {}", type, e);
            }
        }
    }
    
    /**
     * 清除所有监听器（同步）
     */
    public synchronized void clear() {
        listeners.values().forEach(List::clear);
    }
    
    /**
     * 清除指定类型的所有监听器
     */
    public void clear(QuestEventType type) {
        listeners.get(type).clear();
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
