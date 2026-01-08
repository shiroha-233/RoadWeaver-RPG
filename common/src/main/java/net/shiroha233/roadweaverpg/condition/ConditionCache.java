package net.shiroha233.roadweaverpg.condition;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 条件评估缓存 - 避免重复计算，使用Guava Cache自动过期
 */
public final class ConditionCache {
    
    private ConditionCache() {}
    
    // 条件解析缓存（JSON字符串 -> 条件对象）
    private static final Cache<String, PlayerCondition<ConditionContext>> PARSED_CONDITIONS = 
            CacheBuilder.newBuilder()
                    .maximumSize(500)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build();
    
    // 条件评估结果缓存（玩家UUID + 条件哈希 -> 结果），约1tick过期
    private static final Cache<String, Boolean> EVALUATION_CACHE = 
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(50, TimeUnit.MILLISECONDS)
                    .build();
    
    /**
     * 获取或解析条件
     */
    public static PlayerCondition<ConditionContext> getOrParse(JsonObject json) {
        if (json == null) return PlayerCondition.always();
        
        String key = json.toString();
        PlayerCondition<ConditionContext> cached = PARSED_CONDITIONS.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        
        PlayerCondition<ConditionContext> parsed = ConditionRegistry.fromJson(json);
        PARSED_CONDITIONS.put(key, parsed);
        return parsed;
    }
    
    /**
     * 带缓存的条件评估
     */
    public static boolean evaluateWithCache(ServerPlayer player, 
                                            PlayerCondition<ConditionContext> condition,
                                            String conditionId) {
        String cacheKey = player.getUUID() + ":" + conditionId;
        Boolean cached = EVALUATION_CACHE.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        boolean result = condition.evaluate(player, ConditionContext.empty());
        EVALUATION_CACHE.put(cacheKey, result);
        return result;
    }
    
    /**
     * 清除玩家的评估缓存（Guava Cache不支持前缀删除，但过期时间很短）
     */
    public static void clearPlayerCache(UUID playerId) {
        // 由于过期时间很短（50ms），不需要主动清理
    }
    
    /**
     * 清除所有缓存
     */
    public static void clearAll() {
        PARSED_CONDITIONS.invalidateAll();
        EVALUATION_CACHE.invalidateAll();
    }
    
    /**
     * 获取缓存统计
     */
    public static String getStats() {
        return String.format("解析缓存: %d, 评估缓存: %d", 
                PARSED_CONDITIONS.size(), EVALUATION_CACHE.size());
    }
}
