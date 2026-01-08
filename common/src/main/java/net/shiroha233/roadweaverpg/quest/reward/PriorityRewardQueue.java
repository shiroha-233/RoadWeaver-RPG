package net.shiroha233.roadweaverpg.quest.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.config.QuestSystemConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 分级优先奖励队列
 * 
 * 设计原理：
 * - 按优先级分级处理奖励（关键奖励优先）
 * - 指数退避重试机制
 * - 持久化支持（服务器重启不丢失）
 * - 去重机制（防止重复发放）
 * - 批量处理优化
 * 
 * 优先级说明：
 * - CRITICAL: 关键奖励（如主线任务奖励），立即处理
 * - HIGH: 高优先级（如稀有物品），优先处理
 * - NORMAL: 普通奖励，正常处理
 * - LOW: 低优先级（如经验值），延迟处理
 */
public class PriorityRewardQueue {
    
    private static volatile PriorityRewardQueue instance;
    private static final Object LOCK = new Object();
    
    // 分级队列
    private final Map<RewardPriority, Queue<PendingReward>> priorityQueues = new ConcurrentHashMap<>();
    
    // 重试队列（按下次重试时间排序）
    private final PriorityBlockingQueue<PendingReward> retryQueue;
    
    // 已处理奖励历史（用于去重）
    private final Map<UUID, Long> processedHistory = new ConcurrentHashMap<>();
    
    // 玩家待处理奖励计数
    private final Map<UUID, AtomicLong> playerPendingCounts = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalRetried = new AtomicLong(0);
    private final Map<RewardPriority, AtomicLong> priorityStats = new ConcurrentHashMap<>();
    
    // 配置
    private static final long HISTORY_RETENTION_MS = QuestSystemConfig.REWARD_HISTORY_RETENTION;
    private static final int BATCH_SIZE = QuestSystemConfig.REWARD_BATCH_SIZE;
    
    // 脏标记
    private volatile boolean dirty = false;
    
    private PriorityRewardQueue() {
        // 初始化分级队列
        for (RewardPriority priority : RewardPriority.values()) {
            priorityQueues.put(priority, new ConcurrentLinkedQueue<>());
            priorityStats.put(priority, new AtomicLong(0));
        }
        
        // 重试队列按下次重试时间排序
        retryQueue = new PriorityBlockingQueue<>(100, 
                Comparator.comparingLong(PendingReward::nextRetryTime));
    }
    
    public static PriorityRewardQueue getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new PriorityRewardQueue();
                }
            }
        }
        return instance;
    }
    
    /**
     * 添加奖励到队列
     * 
     * @param playerId 玩家ID
     * @param questId 委托ID
     * @param reward 奖励
     * @param priority 优先级
     * @return 是否成功添加（false表示重复）
     */
    public boolean enqueue(UUID playerId, ResourceLocation questId, QuestReward reward, RewardPriority priority) {
        PendingReward pending = PendingReward.create(playerId, questId, reward);
        
        // 去重检查
        if (isDuplicate(pending.rewardId())) {
            RoadWeaverRPG.LOGGER.debug("Duplicate reward detected: {}", pending.rewardId());
            return false;
        }
        
        priorityQueues.get(priority).offer(pending);
        playerPendingCounts.computeIfAbsent(playerId, k -> new AtomicLong(0)).incrementAndGet();
        priorityStats.get(priority).incrementAndGet();
        markDirty();
        
        RoadWeaverRPG.LOGGER.debug("Enqueued {} priority reward for player {}: quest={}, type={}",
                priority, playerId, questId, reward.getType());
        return true;
    }
    
    /**
     * 添加奖励（自动确定优先级）
     */
    public boolean enqueue(UUID playerId, ResourceLocation questId, QuestReward reward) {
        RewardPriority priority = determinePriority(reward);
        return enqueue(playerId, questId, reward, priority);
    }
    
    /**
     * 处理玩家的待发放奖励
     * 
     * @param player 玩家
     * @return 成功发放的奖励数量
     */
    public int processPlayerRewards(ServerPlayer player) {
        UUID playerId = player.getUUID();
        int processed = 0;
        
        // 按优先级顺序处理
        for (RewardPriority priority : RewardPriority.values()) {
            Queue<PendingReward> queue = priorityQueues.get(priority);
            List<PendingReward> toProcess = new ArrayList<>();
            
            // 收集该玩家的奖励
            PendingReward pending;
            while ((pending = queue.peek()) != null && toProcess.size() < BATCH_SIZE) {
                if (pending.playerId().equals(playerId)) {
                    queue.poll();
                    toProcess.add(pending);
                } else {
                    break; // 队列是按玩家分组的，遇到其他玩家就停止
                }
            }
            
            // 处理奖励
            for (PendingReward reward : toProcess) {
                if (tryGrantReward(player, reward)) {
                    processed++;
                }
            }
        }
        
        // 更新计数
        if (processed > 0) {
            AtomicLong count = playerPendingCounts.get(playerId);
            if (count != null) {
                count.addAndGet(-processed);
            }
            markDirty();
        }
        
        return processed;
    }
    
    /**
     * 处理重试队列
     */
    public void processRetryQueue(Function<UUID, ServerPlayer> playerLookup) {
        long now = System.currentTimeMillis();
        List<PendingReward> toRetry = new ArrayList<>();
        
        // 收集准备重试的奖励
        PendingReward pending;
        while ((pending = retryQueue.peek()) != null && pending.nextRetryTime() <= now) {
            retryQueue.poll();
            toRetry.add(pending);
            if (toRetry.size() >= BATCH_SIZE) break;
        }
        
        // 处理重试
        for (PendingReward reward : toRetry) {
            ServerPlayer player = playerLookup.apply(reward.playerId());
            if (player != null) {
                totalRetried.incrementAndGet();
                if (!tryGrantReward(player, reward)) {
                    handleRetryFailure(reward);
                }
            } else {
                // 玩家不在线，放回队列
                retryQueue.offer(reward);
            }
        }
        
        if (!toRetry.isEmpty()) {
            markDirty();
        }
    }
    
    /**
     * 尝试发放奖励
     */
    private boolean tryGrantReward(ServerPlayer player, PendingReward pending) {
        try {
            QuestReward reward = pending.reward();
            
            if (!reward.canGrant(player)) {
                handleGrantFailure(pending, "Condition not met");
                return false;
            }
            
            reward.grant(player);
            recordProcessed(pending);
            totalProcessed.incrementAndGet();
            
            RoadWeaverRPG.LOGGER.debug("Reward granted: player={}, quest={}, type={}",
                    player.getName().getString(), pending.questId(), reward.getType());
            return true;
            
        } catch (Exception e) {
            handleGrantFailure(pending, e.getMessage());
            RoadWeaverRPG.LOGGER.error("Failed to grant reward: {}", e.getMessage());
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
            RoadWeaverRPG.LOGGER.error("Reward permanently failed: player={}, quest={}, error={}",
                    pending.playerId(), pending.questId(), error);
        }
        markDirty();
    }
    
    /**
     * 处理重试失败
     */
    private void handleRetryFailure(PendingReward pending) {
        if (pending.canRetry()) {
            PendingReward retry = pending.withRetry("Retry failed");
            retryQueue.offer(retry);
        } else {
            totalFailed.incrementAndGet();
            RoadWeaverRPG.LOGGER.error("Reward permanently failed after {} retries: player={}, quest={}",
                    pending.retryCount(), pending.playerId(), pending.questId());
        }
    }
    
    /**
     * 根据奖励类型确定优先级
     */
    private RewardPriority determinePriority(QuestReward reward) {
        return switch (reward.getType()) {
            case ITEM -> RewardPriority.HIGH;
            case EXPERIENCE -> RewardPriority.LOW;
            case COIN -> RewardPriority.NORMAL;
            case REPUTATION -> RewardPriority.NORMAL;
            default -> RewardPriority.NORMAL;
        };
    }
    
    /**
     * 检查是否重复
     */
    private boolean isDuplicate(UUID rewardId) {
        Long lastTime = processedHistory.get(rewardId);
        return lastTime != null && (System.currentTimeMillis() - lastTime) < HISTORY_RETENTION_MS;
    }
    
    /**
     * 记录已处理
     */
    private void recordProcessed(PendingReward pending) {
        processedHistory.put(pending.rewardId(), System.currentTimeMillis());
    }
    
    /**
     * 清理过期历史
     */
    public void cleanupExpiredHistory() {
        long threshold = System.currentTimeMillis() - HISTORY_RETENTION_MS;
        int before = processedHistory.size();
        processedHistory.entrySet().removeIf(e -> e.getValue() < threshold);
        int removed = before - processedHistory.size();
        
        if (removed > 0) {
            RoadWeaverRPG.LOGGER.debug("Cleaned up {} expired reward history entries", removed);
        }
    }
    
    /**
     * 清除玩家数据
     */
    public void clearPlayerData(UUID playerId) {
        // 将未处理的奖励移到重试队列
        for (Queue<PendingReward> queue : priorityQueues.values()) {
            List<PendingReward> toMove = new ArrayList<>();
            queue.removeIf(p -> {
                if (p.playerId().equals(playerId)) {
                    toMove.add(p);
                    return true;
                }
                return false;
            });
            toMove.forEach(retryQueue::offer);
        }
        
        playerPendingCounts.remove(playerId);
        markDirty();
    }
    
    /**
     * 获取玩家待处理奖励数量
     */
    public long getPlayerPendingCount(UUID playerId) {
        AtomicLong count = playerPendingCounts.get(playerId);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 获取统计信息
     */
    public QueueStats getStats() {
        int totalPending = priorityQueues.values().stream()
                .mapToInt(Queue::size)
                .sum();
        
        Map<RewardPriority, Long> priorityCounts = new EnumMap<>(RewardPriority.class);
        priorityStats.forEach((k, v) -> priorityCounts.put(k, v.get()));
        
        return new QueueStats(
                totalProcessed.get(),
                totalFailed.get(),
                totalRetried.get(),
                totalPending,
                retryQueue.size(),
                processedHistory.size(),
                priorityCounts
        );
    }
    
    // ==================== 持久化 ====================
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        
        // 保存分级队列
        for (RewardPriority priority : RewardPriority.values()) {
            ListTag list = new ListTag();
            for (PendingReward reward : priorityQueues.get(priority)) {
                list.add(reward.toNbt());
            }
            tag.put("queue_" + priority.name(), list);
        }
        
        // 保存重试队列
        ListTag retryList = new ListTag();
        for (PendingReward reward : retryQueue) {
            retryList.add(reward.toNbt());
        }
        tag.put("retry", retryList);
        
        // 保存统计
        tag.putLong("totalProcessed", totalProcessed.get());
        tag.putLong("totalFailed", totalFailed.get());
        tag.putLong("totalRetried", totalRetried.get());
        
        dirty = false;
        return tag;
    }
    
    public void fromNbt(CompoundTag tag) {
        // 清空现有数据
        priorityQueues.values().forEach(Queue::clear);
        retryQueue.clear();
        
        // 加载分级队列
        for (RewardPriority priority : RewardPriority.values()) {
            String key = "queue_" + priority.name();
            if (tag.contains(key)) {
                ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    try {
                        PendingReward reward = PendingReward.fromNbt(list.getCompound(i));
                        priorityQueues.get(priority).offer(reward);
                    } catch (Exception e) {
                        RoadWeaverRPG.LOGGER.error("Failed to load pending reward", e);
                    }
                }
            }
        }
        
        // 加载重试队列
        if (tag.contains("retry")) {
            ListTag retryList = tag.getList("retry", Tag.TAG_COMPOUND);
            for (int i = 0; i < retryList.size(); i++) {
                try {
                    PendingReward reward = PendingReward.fromNbt(retryList.getCompound(i));
                    retryQueue.offer(reward);
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.error("Failed to load retry reward", e);
                }
            }
        }
        
        // 加载统计
        if (tag.contains("totalProcessed")) {
            totalProcessed.set(tag.getLong("totalProcessed"));
            totalFailed.set(tag.getLong("totalFailed"));
            totalRetried.set(tag.getLong("totalRetried"));
        }
        
        RoadWeaverRPG.LOGGER.info("Loaded priority reward queue: {} pending, {} retry",
                priorityQueues.values().stream().mapToInt(Queue::size).sum(),
                retryQueue.size());
        
        dirty = false;
    }
    
    private void markDirty() { dirty = true; }
    public boolean isDirty() { return dirty; }
    
    // ==================== 内部类 ====================
    
    /** 奖励优先级 */
    public enum RewardPriority {
        CRITICAL,   // 关键奖励
        HIGH,       // 高优先级
        NORMAL,     // 普通
        LOW         // 低优先级
    }
    
    /** 队列统计 */
    public record QueueStats(
            long processed,
            long failed,
            long retried,
            int pending,
            int pendingRetries,
            int historySize,
            Map<RewardPriority, Long> priorityCounts
    ) {}
}
