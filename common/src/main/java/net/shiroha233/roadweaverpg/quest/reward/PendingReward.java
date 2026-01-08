package net.shiroha233.roadweaverpg.quest.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 待发放奖励记录
 * 用于奖励队列系统，确保奖励不丢失
 * 
 * 改进：
 * - 支持 NBT 序列化，可持久化到存档
 * - 优化去重键生成算法
 */
public record PendingReward(
        UUID rewardId,           // 奖励唯一ID（用于去重）
        UUID playerId,           // 玩家UUID
        ResourceLocation questId, // 委托ID
        QuestReward reward,      // 奖励对象
        long createTime,         // 创建时间
        int retryCount,          // 重试次数
        long nextRetryTime,      // 下次重试时间
        String lastError         // 最后一次错误信息
) {
    /** 最大重试次数 */
    public static final int MAX_RETRIES = 10;
    
    /** 基础重试间隔（毫秒） */
    private static final long BASE_RETRY_INTERVAL = 1000L;
    
    /**
     * 创建新的待发放奖励
     */
    public static PendingReward create(UUID playerId, ResourceLocation questId, QuestReward reward) {
        return new PendingReward(
                UUID.randomUUID(),
                playerId,
                questId,
                reward,
                System.currentTimeMillis(),
                0,
                System.currentTimeMillis(),
                null
        );
    }
    
    /**
     * 创建重试记录（指数退避）
     */
    public PendingReward withRetry(String error) {
        int newRetryCount = retryCount + 1;
        // 指数退避：1s, 2s, 4s, 8s...
        long delay = BASE_RETRY_INTERVAL * (1L << Math.min(newRetryCount - 1, 6));
        return new PendingReward(
                rewardId, playerId, questId, reward, createTime,
                newRetryCount, System.currentTimeMillis() + delay, error
        );
    }
    
    /** 是否可以重试 */
    public boolean canRetry() {
        return retryCount < MAX_RETRIES;
    }
    
    /** 是否到达重试时间 */
    public boolean isReadyForRetry() {
        return System.currentTimeMillis() >= nextRetryTime;
    }
    
    /** 生成去重键（优化版本，使用奖励ID确保唯一性） */
    public String getDeduplicationKey() {
        return rewardId.toString();
    }
    
    /**
     * 序列化到 NBT
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("rewardId", rewardId);
        tag.putUUID("playerId", playerId);
        tag.putString("questId", questId.toString());
        tag.put("reward", RewardRegistry.toNbt(reward));
        tag.putLong("createTime", createTime);
        tag.putInt("retryCount", retryCount);
        tag.putLong("nextRetryTime", nextRetryTime);
        if (lastError != null) {
            tag.putString("lastError", lastError);
        }
        return tag;
    }
    
    /**
     * 从 NBT 反序列化
     */
    public static PendingReward fromNbt(CompoundTag tag) {
        UUID rewardId = tag.getUUID("rewardId");
        UUID playerId = tag.getUUID("playerId");
        ResourceLocation questId = new ResourceLocation(tag.getString("questId"));
        QuestReward reward = RewardRegistry.fromNbt(tag.getCompound("reward"));
        long createTime = tag.getLong("createTime");
        int retryCount = tag.getInt("retryCount");
        long nextRetryTime = tag.getLong("nextRetryTime");
        String lastError = tag.contains("lastError") ? tag.getString("lastError") : null;
        
        return new PendingReward(rewardId, playerId, questId, reward, 
                createTime, retryCount, nextRetryTime, lastError);
    }
}
