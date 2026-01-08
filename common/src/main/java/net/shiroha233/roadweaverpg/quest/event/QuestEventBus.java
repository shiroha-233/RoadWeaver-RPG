package net.shiroha233.roadweaverpg.quest.event;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 委托事件总线
 * 
 * 设计原理：
 * - 解耦事件发布者和订阅者
 * - 支持事件优先级
 * - 支持同步/异步事件处理
 * - 支持事件过滤
 * - 线程安全
 * 
 * 使用方式：
 * <pre>
 * // 订阅事件
 * QuestEventBus.getInstance().subscribe(QuestAcceptedEvent.class, event -> {
 *     // 处理事件
 * }, Priority.NORMAL);
 * 
 * // 发布事件
 * QuestEventBus.getInstance().publish(new QuestAcceptedEvent(player, quest));
 * </pre>
 */
public class QuestEventBus {
    
    private static volatile QuestEventBus instance;
    private static final Object LOCK = new Object();
    
    // 事件订阅者（按事件类型分组）
    private final Map<Class<? extends QuestEvent>, List<EventSubscriber<?>>> subscribers = new ConcurrentHashMap<>();
    
    // 异步事件执行器
    private final ExecutorService asyncExecutor;
    
    // 统计信息
    private final AtomicLong totalEventsPublished = new AtomicLong(0);
    private final AtomicLong totalEventsHandled = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    
    // 订阅者数量限制（防止内存泄漏）
    private static final int MAX_SUBSCRIBERS_PER_EVENT = 100;
    
    private QuestEventBus() {
        this.asyncExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "QuestEventBus-Async");
            t.setDaemon(true);
            return t;
        });
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
     * 订阅事件（带订阅者数量限制）
     * 
     * 原理：限制每个事件类型的订阅者数量，防止内存泄漏
     * 
     * @param eventType 事件类型
     * @param handler 事件处理器
     * @param priority 优先级
     * @return 订阅ID（用于取消订阅）
     */
    public <T extends QuestEvent> String subscribe(Class<T> eventType, Consumer<T> handler, Priority priority) {
        List<EventSubscriber<?>> eventSubscribers = subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
        
        // 检查订阅者数量限制（防止内存泄漏）
        if (eventSubscribers.size() >= MAX_SUBSCRIBERS_PER_EVENT) {
            RoadWeaverRPG.LOGGER.warn("订阅者数量已达上限 ({})，拒绝订阅: {}", MAX_SUBSCRIBERS_PER_EVENT, eventType.getSimpleName());
            return null;
        }
        
        String subscriptionId = UUID.randomUUID().toString();
        EventSubscriber<T> subscriber = new EventSubscriber<>(subscriptionId, handler, priority);
        
        eventSubscribers.add(subscriber);
        
        // 按优先级排序
        sortSubscribers(eventType);
        
        RoadWeaverRPG.LOGGER.debug("Subscribed to {} with priority {} (total: {})", 
                eventType.getSimpleName(), priority, eventSubscribers.size());
        return subscriptionId;
    }
    
    /**
     * 订阅事件（默认优先级）
     */
    public <T extends QuestEvent> String subscribe(Class<T> eventType, Consumer<T> handler) {
        return subscribe(eventType, handler, Priority.NORMAL);
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribe(String subscriptionId) {
        for (List<EventSubscriber<?>> list : subscribers.values()) {
            list.removeIf(s -> s.id().equals(subscriptionId));
        }
    }
    
    /**
     * 发布事件（同步）
     */
    @SuppressWarnings("unchecked")
    public <T extends QuestEvent> void publish(T event) {
        if (event == null) return;
        
        totalEventsPublished.incrementAndGet();
        
        List<EventSubscriber<?>> subs = subscribers.get(event.getClass());
        if (subs == null || subs.isEmpty()) return;
        
        for (EventSubscriber<?> subscriber : subs) {
            try {
                ((EventSubscriber<T>) subscriber).handler().accept(event);
                totalEventsHandled.incrementAndGet();
                
                // 检查事件是否被取消
                if (event.isCancelled()) {
                    RoadWeaverRPG.LOGGER.debug("Event {} was cancelled", event.getClass().getSimpleName());
                    break;
                }
            } catch (Exception e) {
                totalEventsFailed.incrementAndGet();
                RoadWeaverRPG.LOGGER.error("Error handling event {}: {}", 
                        event.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    /**
     * 发布事件（异步）
     */
    public <T extends QuestEvent> CompletableFuture<Void> publishAsync(T event) {
        return CompletableFuture.runAsync(() -> publish(event), asyncExecutor);
    }
    
    /**
     * 批量发布事件
     */
    public void publishBatch(Collection<? extends QuestEvent> events) {
        for (QuestEvent event : events) {
            publish(event);
        }
    }
    
    /**
     * 按优先级排序订阅者
     */
    private void sortSubscribers(Class<? extends QuestEvent> eventType) {
        List<EventSubscriber<?>> list = subscribers.get(eventType);
        if (list != null) {
            list.sort(Comparator.comparingInt(s -> s.priority().ordinal()));
        }
    }
    
    /**
     * 获取统计信息
     */
    public EventBusStats getStats() {
        return new EventBusStats(
                totalEventsPublished.get(),
                totalEventsHandled.get(),
                totalEventsFailed.get(),
                subscribers.size()
        );
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // ==================== 内部类 ====================
    
    /** 事件优先级 */
    public enum Priority {
        HIGHEST,    // 最高优先级（最先执行）
        HIGH,
        NORMAL,
        LOW,
        LOWEST      // 最低优先级（最后执行）
    }
    
    /** 事件订阅者 */
    private record EventSubscriber<T extends QuestEvent>(
            String id,
            Consumer<T> handler,
            Priority priority
    ) {}
    
    /** 事件总线统计 */
    public record EventBusStats(
            long published,
            long handled,
            long failed,
            int subscriberTypes
    ) {}
}
