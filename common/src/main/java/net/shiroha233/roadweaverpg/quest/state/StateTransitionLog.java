package net.shiroha233.roadweaverpg.quest.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 状态转移日志
 * 
 * 设计原理：
 * - 记录委托的完整状态转移历史
 * - 支持回溯和审计
 * - 限制日志大小防止内存泄漏
 * - 支持持久化
 */
public class StateTransitionLog {
    
    private static volatile StateTransitionLog instance;
    private static final Object LOCK = new Object();
    
    // 每个委托实例的状态转移历史
    private final Map<UUID, Deque<TransitionEntry>> logs = new ConcurrentHashMap<>();
    
    // 每个实例最多保留的日志条数
    private static final int MAX_ENTRIES_PER_INSTANCE = 20;
    
    // 全局最大实例数（防止内存泄漏）
    private static final int MAX_INSTANCES = 10000;
    
    private StateTransitionLog() {}
    
    public static StateTransitionLog getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new StateTransitionLog();
                }
            }
        }
        return instance;
    }
    
    /**
     * 记录状态转移
     */
    public void log(UUID instanceId, ResourceLocation questId, QuestState from, QuestState to, String reason) {
        TransitionEntry entry = new TransitionEntry(
                System.currentTimeMillis(),
                questId,
                from,
                to,
                reason
        );
        
        Deque<TransitionEntry> history = logs.computeIfAbsent(instanceId, k -> new ConcurrentLinkedDeque<>());
        history.addLast(entry);
        
        // 限制每个实例的日志大小
        while (history.size() > MAX_ENTRIES_PER_INSTANCE) {
            history.removeFirst();
        }
        
        // 限制全局实例数
        if (logs.size() > MAX_INSTANCES) {
            cleanupOldestLogs();
        }
    }
    
    /**
     * 获取委托实例的状态转移历史
     */
    public List<TransitionEntry> getHistory(UUID instanceId) {
        Deque<TransitionEntry> history = logs.get(instanceId);
        if (history == null) return Collections.emptyList();
        return new ArrayList<>(history);
    }
    
    /**
     * 获取最近一次状态转移
     */
    public Optional<TransitionEntry> getLastTransition(UUID instanceId) {
        Deque<TransitionEntry> history = logs.get(instanceId);
        if (history == null || history.isEmpty()) return Optional.empty();
        return Optional.of(history.getLast());
    }
    
    /**
     * 清除委托实例的日志
     */
    public void clearLog(UUID instanceId) {
        logs.remove(instanceId);
    }
    
    /**
     * 清理最旧的日志（当超过全局限制时）
     * 
     * 原理：找出时间戳最早的日志条目并删除，防止内存无限增长
     */
    private synchronized void cleanupOldestLogs() {
        if (logs.size() <= MAX_INSTANCES) return;
        
        // 找出最旧的日志并删除
        logs.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .min(Comparator.comparingLong(e -> e.getValue().getFirst().timestamp()))
                .ifPresent(e -> {
                    logs.remove(e.getKey());
                    RoadWeaverRPG.LOGGER.debug("Removed oldest state transition log for instance {}", e.getKey());
                });
    }
    
    /**
     * 定期清理过期日志（防止内存泄漏）
     * 
     * 原理：删除超过保留时间的日志条目
     * 应在服务器 Tick 中定期调用
     * 
     * @param retentionMs 日志保留时间（毫秒）
     */
    public synchronized void cleanupExpiredLogs(long retentionMs) {
        long threshold = System.currentTimeMillis() - retentionMs;
        int removedInstances = 0;
        int removedEntries = 0;
        
        Iterator<Map.Entry<UUID, Deque<TransitionEntry>>> iterator = logs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Deque<TransitionEntry>> entry = iterator.next();
            Deque<TransitionEntry> history = entry.getValue();
            
            // 移除过期的条目
            int beforeSize = history.size();
            history.removeIf(e -> e.timestamp() < threshold);
            removedEntries += (beforeSize - history.size());
            
            // 如果队列为空，移除整个实例
            if (history.isEmpty()) {
                iterator.remove();
                removedInstances++;
            }
        }
        
        if (removedInstances > 0 || removedEntries > 0) {
            RoadWeaverRPG.LOGGER.debug("Cleaned up state transition logs: {} instances, {} entries",
                    removedInstances, removedEntries);
        }
    }
    
    /**
     * 获取日志统计信息
     */
    public LogStats getStats() {
        int totalEntries = logs.values().stream()
                .mapToInt(Deque::size)
                .sum();
        
        return new LogStats(logs.size(), totalEntries);
    }
    
    /**
     * 序列化到 NBT（用于持久化）
     */
    public CompoundTag toNbt(UUID instanceId) {
        CompoundTag tag = new CompoundTag();
        Deque<TransitionEntry> history = logs.get(instanceId);
        if (history == null) return tag;
        
        ListTag list = new ListTag();
        for (TransitionEntry entry : history) {
            list.add(entry.toNbt());
        }
        tag.put("history", list);
        return tag;
    }
    
    /**
     * 从 NBT 反序列化
     */
    public void fromNbt(UUID instanceId, CompoundTag tag) {
        if (!tag.contains("history")) return;
        
        ListTag list = tag.getList("history", Tag.TAG_COMPOUND);
        Deque<TransitionEntry> history = new ConcurrentLinkedDeque<>();
        
        for (int i = 0; i < list.size(); i++) {
            TransitionEntry entry = TransitionEntry.fromNbt(list.getCompound(i));
            if (entry != null) {
                history.addLast(entry);
            }
        }
        
        if (!history.isEmpty()) {
            logs.put(instanceId, history);
        }
    }
    
    // ==================== 内部类 ====================
    
    /** 状态转移记录 */
    public record TransitionEntry(
            long timestamp,
            ResourceLocation questId,
            QuestState from,
            QuestState to,
            String reason
    ) {
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("time", timestamp);
            tag.putString("quest", questId.toString());
            tag.putString("from", from.getSerializedName());
            tag.putString("to", to.getSerializedName());
            if (reason != null) {
                tag.putString("reason", reason);
            }
            return tag;
        }
        
        public static TransitionEntry fromNbt(CompoundTag tag) {
            try {
                return new TransitionEntry(
                        tag.getLong("time"),
                        new ResourceLocation(tag.getString("quest")),
                        QuestState.fromString(tag.getString("from")),
                        QuestState.fromString(tag.getString("to")),
                        tag.contains("reason") ? tag.getString("reason") : null
                );
            } catch (Exception e) {
                return null;
            }
        }
    }
    
    /** 日志统计信息 */
    public record LogStats(int instances, int totalEntries) {
        public String toFormattedString() {
            return String.format("State Transition Logs: %d instances, %d entries", instances, totalEntries);
        }
    }
}
