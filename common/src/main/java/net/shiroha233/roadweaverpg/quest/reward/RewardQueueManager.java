package net.shiroha233.roadweaverpg.quest.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 奖励队列管理器（增强版）
 * 
 * 改进点：
 * 1. 持久化支持：奖励队列可保存到 NBT，服务器重启不丢失
 * 2. 批量处理：支持批量发放奖励，提高性能
 * 3. 内存优化：定期清理过期数据，防止内存泄漏
 * 4. 性能监控：统计奖励发放情况，便于调试
 * 5. 线程安全：使用并发集合，支持多线程访问
 * 
 * 设计原则：
 * - 确保奖励不丢失：失败的奖励进入重试队列并持久化
 * - 去重机制：使用 UUID 防止重复发放
 * - 指数退避：避免频繁重试造成性能问题
 */
public class RewardQueueManager {
    
    private static volatile RewardQueueManager instance;
    private static final Object LOCK = new Object();
    
    // 待发放奖励队列（按玩家分组）
    private final Map<UUID, Queue<PendingReward>> pendingQueues = new ConcurrentHashMap<>();
    
    // 已发放奖励历史（用于去重，key为奖励UUID）
    private final Map<UUID, Long> rewardHistory = new ConcurrentHashMap<>();
    
    // 失败奖励队列（等待重试）
    private final Queue<PendingReward> retryQueue = new ConcurrentLinkedQueue<>();
    
    // 统计信息
    private final AtomicLong totalGranted = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalRetried = new AtomicLong(0);
    private final AtomicLong totalEnqueued = new AtomicLong(0);
    
    // 历史记录保留时间（1小时）
    private static final long HISTORY_RETENTION_MS = 3600_000L;
    
    // 批量处理大小
    private static final int BATCH_SIZE = 50;
    
    // 上次清理时间
    private volatile long lastCleanupTime = System.currentTimeMillis();
    
    // 脏标记（用于持久化）
    private volatile boolean dirty = false;
    
    private RewardQueueManager() {}
    
    public static RewardQueueManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RewardQueueManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 添加待发放奖励到队列（线程安全）
     * 
     * 原理：使用 putIfAbsent 实现原子的检查-操作，避免竞态条件
     * 
     * @return 是否成功添加（false表示重复）
     */
    public boolean enqueue(UUID playerId, ResourceLocation questId, QuestReward reward) {
        PendingReward pending = PendingReward.create(playerId, questId, reward);
        
        // 原子操作：检查并添加到历史记录
        Long existingTime = rewardHistory.putIfAbsent(pending.rewardId(), System.currentTimeMillis());
        if (existingTime != null && (System.currentTimeMillis() - existingTime) < HISTORY_RETENTION_MS) {
            RoadWeaverRPG.LOGGER.debug("Duplicate reward detected: {}", pending.rewardId());
            return false;
        }
        
        // 添加到队列
        pendingQueues.computeIfAbsent(playerId, k -> new ConcurrentLinkedQueue<>())
                     .offer(pending);
        totalEnqueued.incrementAndGet();
        markDirty();
        return true;
    }

    /**
     * 处理玩家的待发放奖励（批量处理）
     * @return 成功发放的奖励数量
     */
    public int processPlayerRewards(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Queue<PendingReward> queue = pendingQueues.get(playerId);
        if (queue == null || queue.isEmpty()) return 0;
        
        int granted = 0;
        int processed = 0;
        List<PendingReward> batch = new ArrayList<>(BATCH_SIZE);
        
        // 批量处理
        PendingReward pending;
        while ((pending = queue.poll()) != null && processed < BATCH_SIZE) {
            batch.add(pending);
            processed++;
        }
        
        // 发放奖励
        for (PendingReward reward : batch) {
            if (tryGrantReward(player, reward)) {
                granted++;
            }
        }
        
        // 定期清理历史记录
        cleanupIfNeeded();
        
        if (granted > 0) {
            markDirty();
        }
        
        return granted;
    }
    
    /**
     * 处理重试队列（批量处理）
     */
    public void processRetryQueue(java.util.function.Function<UUID, ServerPlayer> playerLookup) {
        List<PendingReward> toRetry = new ArrayList<>();
        List<PendingReward> notReady = new ArrayList<>();
        PendingReward pending;
        
        int processed = 0;
        // 批量收集准备重试的奖励
        while ((pending = retryQueue.poll()) != null && processed < BATCH_SIZE) {
            if (pending.isReadyForRetry()) {
                toRetry.add(pending);
            } else {
                notReady.add(pending);
            }
            processed++;
        }
        
        // 将未准备好的放回队列
        notReady.forEach(retryQueue::offer);
        
        // 批量处理重试
        for (PendingReward reward : toRetry) {
            ServerPlayer player = playerLookup.apply(reward.playerId());
            if (player != null) {
                totalRetried.incrementAndGet();
                if (!tryGrantReward(player, reward)) {
                    // 重试失败，检查是否还能继续重试
                    if (reward.canRetry()) {
                        retryQueue.offer(reward);
                    } else {
                        totalFailed.incrementAndGet();
                        RoadWeaverRPG.LOGGER.error("Reward permanently failed after {} retries: player={}, quest={}, type={}",
                                reward.retryCount(), reward.playerId(), reward.questId(), reward.reward().getType());
                    }
                }
            } else {
                // 玩家不在线，放回队列等待
                retryQueue.offer(reward);
            }
        }
        
        if (!toRetry.isEmpty()) {
            markDirty();
        }
    }
    
    /**
     * 尝试发放单个奖励
     */
    private boolean tryGrantReward(ServerPlayer player, PendingReward pending) {
        RewardPerformanceMonitor monitor = RewardPerformanceMonitor.getInstance();
        long startTime = monitor.startTiming();
        
        try {
            QuestReward reward = pending.reward();
            
            if (!reward.canGrant(player)) {
                // 无法发放（如背包满），加入重试队列
                handleGrantFailure(pending, "Cannot grant: condition not met");
                return false;
            }
            
            reward.grant(player);
            
            // 记录到历史（用于去重）
            recordHistory(pending);
            totalGranted.incrementAndGet();
            
            // 记录性能指标
            monitor.endTiming(reward.getType(), startTime);
            
            RoadWeaverRPG.LOGGER.debug("Reward granted: player={}, quest={}, type={}",
                    player.getName().getString(), pending.questId(), reward.getType());
            return true;
            
        } catch (Exception e) {
            handleGrantFailure(pending, e.getMessage());
            RoadWeaverRPG.LOGGER.error("Failed to grant reward: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理发放失败
     */
    private void handleGrantFailure(PendingReward pending, String error) {
        PendingReward retry = pending.withRetry(error);
        
        if (retry.canRetry()) {
            retryQueue.offer(retry);
            RoadWeaverRPG.LOGGER.debug("Reward queued for retry {}/{}: {}",
                    retry.retryCount(), PendingReward.MAX_RETRIES, error);
        } else {
            totalFailed.incrementAndGet();
            RoadWeaverRPG.LOGGER.error("Reward failed permanently: player={}, quest={}, error={}",
                    pending.playerId(), pending.questId(), error);
        }
        markDirty();
    }
    
    /**
     * 记录发放历史（线程安全）
     * 
     * 原理：使用 ConcurrentHashMap 的原子操作确保线程安全
     */
    private void recordHistory(PendingReward pending) {
        rewardHistory.put(pending.rewardId(), System.currentTimeMillis());
    }
    
    /**
     * 定期清理过期历史
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < HISTORY_RETENTION_MS / 2) return;
        
        lastCleanupTime = now;
        long threshold = now - HISTORY_RETENTION_MS;
        
        int beforeSize = rewardHistory.size();
        rewardHistory.entrySet().removeIf(e -> e.getValue() < threshold);
        int removed = beforeSize - rewardHistory.size();
        
        if (removed > 0) {
            RoadWeaverRPG.LOGGER.debug("Cleaned up {} expired reward history entries", removed);
        }
    }
    
    /**
     * 清除玩家数据（登出时调用）
     */
    public void clearPlayerData(UUID playerId) {
        Queue<PendingReward> queue = pendingQueues.remove(playerId);
        if (queue != null && !queue.isEmpty()) {
            // 将未发放的奖励移到重试队列
            queue.forEach(retryQueue::offer);
            RoadWeaverRPG.LOGGER.debug("Moved {} pending rewards to retry queue for player {}",
                    queue.size(), playerId);
            markDirty();
        }
    }
    
    /**
     * 获取统计信息
     */
    public RewardStats getStats() {
        int totalPending = pendingQueues.values().stream()
                .mapToInt(Queue::size)
                .sum();
        
        return new RewardStats(
                totalGranted.get(),
                totalFailed.get(),
                totalRetried.get(),
                totalEnqueued.get(),
                totalPending,
                retryQueue.size(),
                rewardHistory.size()
        );
    }
    
    // ==================== 持久化支持 ====================
    
    /**
     * 序列化到 NBT
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        
        // 保存待发放队列
        ListTag pendingList = new ListTag();
        for (Map.Entry<UUID, Queue<PendingReward>> entry : pendingQueues.entrySet()) {
            for (PendingReward reward : entry.getValue()) {
                pendingList.add(reward.toNbt());
            }
        }
        tag.put("pending", pendingList);
        
        // 保存重试队列
        ListTag retryList = new ListTag();
        for (PendingReward reward : retryQueue) {
            retryList.add(reward.toNbt());
        }
        tag.put("retry", retryList);
        
        // 保存统计信息
        tag.putLong("totalGranted", totalGranted.get());
        tag.putLong("totalFailed", totalFailed.get());
        tag.putLong("totalRetried", totalRetried.get());
        tag.putLong("totalEnqueued", totalEnqueued.get());
        
        dirty = false;
        return tag;
    }
    
    /**
     * 从 NBT 反序列化
     */
    public void fromNbt(CompoundTag tag) {
        // 清空现有数据
        pendingQueues.clear();
        retryQueue.clear();
        
        // 加载待发放队列
        ListTag pendingList = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingList.size(); i++) {
            try {
                PendingReward reward = PendingReward.fromNbt(pendingList.getCompound(i));
                pendingQueues.computeIfAbsent(reward.playerId(), k -> new ConcurrentLinkedQueue<>())
                             .offer(reward);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load pending reward from NBT", e);
            }
        }
        
        // 加载重试队列
        ListTag retryList = tag.getList("retry", Tag.TAG_COMPOUND);
        for (int i = 0; i < retryList.size(); i++) {
            try {
                PendingReward reward = PendingReward.fromNbt(retryList.getCompound(i));
                retryQueue.offer(reward);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load retry reward from NBT", e);
            }
        }
        
        // 加载统计信息
        if (tag.contains("totalGranted")) {
            totalGranted.set(tag.getLong("totalGranted"));
            totalFailed.set(tag.getLong("totalFailed"));
            totalRetried.set(tag.getLong("totalRetried"));
            totalEnqueued.set(tag.getLong("totalEnqueued"));
        }
        
        RoadWeaverRPG.LOGGER.info("Loaded reward queue: {} pending, {} retry",
                pendingQueues.values().stream().mapToInt(Queue::size).sum(),
                retryQueue.size());
        
        dirty = false;
    }
    
    /**
     * 标记为脏（需要保存）
     */
    private void markDirty() {
        dirty = true;
    }
    
    /**
     * 是否需要保存
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalGranted.set(0);
        totalFailed.set(0);
        totalRetried.set(0);
        totalEnqueued.set(0);
    }
    
    public record RewardStats(
            long granted, 
            long failed, 
            long retried, 
            long enqueued,
            int pending,
            int pendingRetries, 
            int historySize
    ) {
        public String toFormattedString() {
            return String.format(
                "Reward Stats: Granted=%d, Failed=%d, Retried=%d, Enqueued=%d, Pending=%d, Retry=%d, History=%d",
                granted, failed, retried, enqueued, pending, pendingRetries, historySize
            );
        }
    }
}
