package net.shiroha233.roadweaverpg.dialog.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.dialog.condition.DialogCondition;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 条件评估缓存
 * 职责：缓存条件评估结果，避免重复计算
 * 原理：使用 Guava Cache 实现 LRU 缓存，自动过期
 */
public class ConditionCache {
    
    private static final ConditionCache INSTANCE = new ConditionCache();
    
    // 缓存键：玩家UUID + 条件字符串 + NPC实体ID
    private final Cache<CacheKey, Boolean> cache;
    
    private ConditionCache() {
        cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static ConditionCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取或计算条件结果
     */
    public boolean evaluate(String condition, ServerPlayer player, int npcEntityId, 
                           DialogCondition conditionObj) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        
        CacheKey key = new CacheKey(player.getUUID(), condition, npcEntityId);
        Boolean cached = cache.getIfPresent(key);
        
        if (cached != null) {
            return cached;
        }
        
        boolean result = conditionObj.evaluate(player, npcEntityId);
        cache.put(key, result);
        return result;
    }
    
    /**
     * 清除玩家的所有缓存
     */
    public void invalidatePlayer(UUID playerId) {
        cache.asMap().keySet().removeIf(key -> key.playerId.equals(playerId));
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.invalidateAll();
    }
    
    /**
     * 缓存键
     */
    private record CacheKey(UUID playerId, String condition, int npcEntityId) {}
}
