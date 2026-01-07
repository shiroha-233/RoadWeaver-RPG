package net.shiroha233.roadweaverpg.quest.reward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 奖励队列管理器
 * 
 * 设计原则：
 * - 确保奖励不丢失：失败的奖励进入重试队列
 * - 去重机制：防止重复发放
 * - 指数退避：避免频繁重试造成性能问题
 * - 线程安全：使用并发集合
 */
public class RewardQueueManager {
    
    private static volatile RewardQueueManager instance;
    private static final Object LOCK = new Object();
    
    // 待发放奖励队列（按玩家分组）
    private final Map<UUID, Queue<PendingReward>> pendingQueues = new ConcurrentHashMap<>();
    
    // 已发放奖励历史（用于去重，key为去重键）
    private final Map<String, Long> rewardHistory = new ConcurrentHashMap<>();
    
    // 失败奖励队列（等待重试）
    private final Queue<PendingReward> retryQueue = new ConcurrentLinkedQueue<>();
    
    // 统计信息
    private final AtomicLong totalGranted = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalRetried = new AtomicLong(0);
    
    // 历史记录保留时间（1小时）
    private static final long HISTORY_RETENTION_MS = 3600_000L;
    
    // 上次清理时间
    private volatile long lastCleanupTime = System.currentTimeMillis();
    
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
     * 添加待发放奖励到队列
     * @return 是否成功添加（false表示重复）
     */
    public boolean enqueue(UUID playerId, ResourceLocation questId, QuestReward reward) {
        PendingReward pending = PendingReward.create(playerId, questId, reward);
        
        // 去重检查
        if (isDuplicate(pending)) {
            RoadWeaverRPG.LOGGER.debug("Duplicate reward detected: {}", pending.getDeduplicationKey());
            return false;
        }
        
        pendingQueues.computeIfAbsent(playerId, k -> new ConcurrentLinkedQueue<>())
                     .offer(pending);
        return true;
    }

    /**
     * 处理玩家的待发放奖励
     * @return 成功发放的奖励数量
     */
    public int processPlayerRewards(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Queue<PendingReward> queue = pendingQueues.get(playerId);
        if (queue == null || queue.isEmpty()) return 0;
        
        int granted = 0;
        PendingReward pending;
        
        while ((pending = queue.poll()) != null) {
            if (tryGrantReward(player, pending)) {
                granted++;
            }
        }
        
        // 定期清理历史记录
        cleanupIfNeeded();
        
        return granted;
    }
    
    /**
     * 处理重试队列
     */
    public void processRetryQueue(java.util.function.Function<UUID, ServerPlayer> playerLookup) {
        List<PendingReward> toRetry = new ArrayList<>();
        PendingReward pending;
        
        while ((pending = retryQueue.poll()) != null) {
            if (pending.isReadyForRetry()) {
                toRetry.add(pending);
            } else {
                retryQueue.offer(pending); // 放回队列
            }
        }
        
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
    }
    
    /**
     * 尝试发放单个奖励
     */
    private boolean tryGrantReward(ServerPlayer player, PendingReward pending) {
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
            
            RoadWeaverRPG.LOGGER.debug("Reward granted: player={}, quest={}, type={}",
                    player.getName().getString(), pending.questId(), reward.getType());
            return true;
            
        } catch (Exception e) {
            handleGrantFailure(pending, e.getMessage());
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
    }
    
    /**
     * 检查是否重复
     */
    private boolean isDuplicate(PendingReward pending) {
        String key = pending.getDeduplicationKey();
        Long lastTime = rewardHistory.get(key);
        return lastTime != null && (System.currentTimeMillis() - lastTime) < HISTORY_RETENTION_MS;
    }
    
    /**
     * 记录发放历史
     */
    private void recordHistory(PendingReward pending) {
        rewardHistory.put(pending.getDeduplicationKey(), System.currentTimeMillis());
    }
    
    /**
     * 定期清理过期历史
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < HISTORY_RETENTION_MS / 2) return;
        
        lastCleanupTime = now;
        long threshold = now - HISTORY_RETENTION_MS;
        rewardHistory.entrySet().removeIf(e -> e.getValue() < threshold);
    }
    
    /**
     * 清除玩家数据（登出时调用）
     */
    public void clearPlayerData(UUID playerId) {
        pendingQueues.remove(playerId);
    }
    
    /**
     * 获取统计信息
     */
    public RewardStats getStats() {
        return new RewardStats(
                totalGranted.get(),
                totalFailed.get(),
                totalRetried.get(),
                retryQueue.size(),
                rewardHistory.size()
        );
    }
    
    public record RewardStats(long granted, long failed, long retried, int pendingRetries, int historySize) {}
}
