package net.shiroha233.roadweaverpg.quest.index;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.ObjectiveRegistry;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 目标索引系统
 * 
 * 设计原理：
 * - 按事件类型建立倒排索引，将 O(n²) 复杂度降低到 O(1)
 * - 增量更新：只更新变化的部分
 * - 版本号机制：检测数据变更
 * - 多级索引：事件类型 -> 玩家 -> 目标列表
 * 
 * 性能优化：
 * - 避免每次事件都遍历所有委托的所有目标
 * - 缓存目标定义，减少重复查询
 */
public class ObjectiveIndex {
    
    private static volatile ObjectiveIndex instance;
    private static final Object LOCK = new Object();
    
    // 主索引：事件类型 -> 玩家ID -> 目标条目列表
    private final Map<String, Map<UUID, List<IndexEntry>>> eventIndex = new ConcurrentHashMap<>();
    
    // 玩家数据版本号（用于检测变更）
    private final Map<UUID, Long> playerVersions = new ConcurrentHashMap<>();
    
    // 目标定义缓存
    private final Map<String, QuestObjective> objectiveCache = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong indexHits = new AtomicLong(0);
    private final AtomicLong indexMisses = new AtomicLong(0);
    private final AtomicLong indexRebuilds = new AtomicLong(0);
    
    // 清理相关
    private volatile long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 300_000L; // 5分钟
    
    private ObjectiveIndex() {}
    
    public static ObjectiveIndex getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ObjectiveIndex();
                }
            }
        }
        return instance;
    }
    
    /**
     * 重建玩家的目标索引（线程安全）
     * 
     * 原理：使用 synchronized 确保重建过程的原子性，避免竞态条件
     * 
     * @param playerId 玩家ID
     * @param activeQuests 活跃委托列表
     * @param dataVersion 数据版本号
     */
    public synchronized void rebuildIndex(UUID playerId, Collection<QuestInstance> activeQuests, long dataVersion) {
        // 检查版本号，避免不必要的重建
        Long cachedVersion = playerVersions.get(playerId);
        if (cachedVersion != null && cachedVersion == dataVersion) {
            return;
        }
        
        // 清除旧索引
        clearPlayerIndex(playerId);
        
        // 构建新索引
        for (QuestInstance instance : activeQuests) {
            if (instance.getState().isTerminal()) continue;
            
            QuestDefinition definition = QuestDefinitionLoader.getInstance()
                    .getDefinition(instance.getQuestId());
            if (definition == null) {
                RoadWeaverRPG.LOGGER.warn("重建索引时找不到委托定义: {}", instance.getQuestId());
                continue;
            }
            
            for (QuestObjective objective : definition.getObjectives()) {
                // 跳过已完成的目标
                var progress = instance.getObjectiveProgress(objective.getId());
                if (progress != null && progress.isCompleted()) continue;
                
                // 缓存目标定义
                String cacheKey = instance.getQuestId() + ":" + objective.getId();
                objectiveCache.put(cacheKey, objective);
                
                // 添加到索引
                String eventType = getEventTypeForObjective(objective);
                addToIndex(eventType, playerId, new IndexEntry(
                        instance.getInstanceId(),
                        instance.getQuestId(),
                        objective.getId(),
                        objective.getType(),
                        cacheKey
                ));
            }
        }
        
        playerVersions.put(playerId, dataVersion);
        indexRebuilds.incrementAndGet();
        
        RoadWeaverRPG.LOGGER.debug("Rebuilt objective index for player {}: {} event types",
                playerId, getEventTypesForPlayer(playerId).size());
    }
    
    /**
     * 增量更新索引（单个委托变更时，线程安全）
     * 
     * 原理：使用 synchronized 确保更新的原子性
     */
    public synchronized void updateIndex(UUID playerId, QuestInstance instance, boolean add) {
        QuestDefinition definition = QuestDefinitionLoader.getInstance()
                .getDefinition(instance.getQuestId());
        if (definition == null) return;
        
        for (QuestObjective objective : definition.getObjectives()) {
            String eventType = getEventTypeForObjective(objective);
            String cacheKey = instance.getQuestId() + ":" + objective.getId();
            
            if (add) {
                objectiveCache.put(cacheKey, objective);
                addToIndex(eventType, playerId, new IndexEntry(
                        instance.getInstanceId(),
                        instance.getQuestId(),
                        objective.getId(),
                        objective.getType(),
                        cacheKey
                ));
            } else {
                removeFromIndex(eventType, playerId, instance.getInstanceId());
                objectiveCache.remove(cacheKey);
            }
        }
        
        // 更新版本号，触发下次查询时的版本检查
        if (add) {
            playerVersions.compute(playerId, (k, v) -> v == null ? 1L : v + 1);
        }
    }
    
    /**
     * 标记目标完成（从索引中移除，线程安全）
     * 
     * 原理：使用 synchronized 确保移除操作的原子性
     */
    public synchronized void markObjectiveComplete(UUID playerId, UUID instanceId, String objectiveId) {
        for (Map<UUID, List<IndexEntry>> playerMap : eventIndex.values()) {
            List<IndexEntry> entries = playerMap.get(playerId);
            if (entries != null) {
                entries.removeIf(e -> e.instanceId().equals(instanceId) && e.objectiveId().equals(objectiveId));
            }
        }
    }
    
    /**
     * 查询指定事件类型的目标列表
     * 
     * @param playerId 玩家ID
     * @param eventType 事件类型
     * @return 匹配的目标条目列表
     */
    public List<IndexEntry> query(UUID playerId, String eventType) {
        Map<UUID, List<IndexEntry>> playerMap = eventIndex.get(eventType);
        if (playerMap == null) {
            indexMisses.incrementAndGet();
            return Collections.emptyList();
        }
        
        List<IndexEntry> entries = playerMap.get(playerId);
        if (entries == null || entries.isEmpty()) {
            indexMisses.incrementAndGet();
            return Collections.emptyList();
        }
        
        indexHits.incrementAndGet();
        return new ArrayList<>(entries);
    }
    
    /**
     * 获取缓存的目标定义
     */
    public Optional<QuestObjective> getCachedObjective(String cacheKey) {
        return Optional.ofNullable(objectiveCache.get(cacheKey));
    }
    
    /**
     * 清除玩家的所有索引（线程安全）
     * 
     * 原理：使用 synchronized 确保清除操作的原子性
     */
    public synchronized void clearPlayerIndex(UUID playerId) {
        for (Map<UUID, List<IndexEntry>> playerMap : eventIndex.values()) {
            playerMap.remove(playerId);
        }
        playerVersions.remove(playerId);
        
        // 清除相关的目标缓存
        objectiveCache.entrySet().removeIf(entry -> {
            // 检查缓存键是否属于该玩家的委托
            // 由于缓存键格式为 "questId:objectiveId"，这里简化处理
            // 实际应该维护一个玩家到缓存键的映射
            return false; // 暂时保留缓存，由定期清理处理
        });
    }
    
    /**
     * 获取玩家关联的所有事件类型
     */
    public Set<String> getEventTypesForPlayer(UUID playerId) {
        Set<String> types = new HashSet<>();
        for (Map.Entry<String, Map<UUID, List<IndexEntry>>> entry : eventIndex.entrySet()) {
            if (entry.getValue().containsKey(playerId)) {
                types.add(entry.getKey());
            }
        }
        return types;
    }
    
    /**
     * 获取索引统计信息
     */
    public IndexStats getStats() {
        int totalEntries = eventIndex.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToInt(List::size)
                .sum();
        
        return new IndexStats(
                indexHits.get(),
                indexMisses.get(),
                indexRebuilds.get(),
                eventIndex.size(),
                totalEntries,
                objectiveCache.size()
        );
    }
    
    /**
     * 定期清理过期数据（防止内存泄漏）
     * 
     * 原理：
     * 1. 清理空的玩家索引
     * 2. 清理过期的目标缓存
     * 3. 清理无效的版本号记录
     * 
     * 应在服务器 Tick 中定期调用
     */
    public synchronized void cleanupExpiredData() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        
        lastCleanupTime = now;
        int removedIndexes = 0;
        int removedCaches = 0;
        int removedVersions = 0;
        
        // 清理空的玩家索引
        for (Map<UUID, List<IndexEntry>> playerMap : eventIndex.values()) {
            Set<UUID> emptyPlayers = new HashSet<>();
            playerMap.forEach((playerId, entries) -> {
                if (entries.isEmpty()) {
                    emptyPlayers.add(playerId);
                }
            });
            emptyPlayers.forEach(playerMap::remove);
            removedIndexes += emptyPlayers.size();
        }
        
        // 清理孤立的版本号记录（没有对应索引的玩家）
        Set<UUID> playersWithIndex = new HashSet<>();
        for (Map<UUID, List<IndexEntry>> playerMap : eventIndex.values()) {
            playersWithIndex.addAll(playerMap.keySet());
        }
        Set<UUID> orphanedVersions = new HashSet<>(playerVersions.keySet());
        orphanedVersions.removeAll(playersWithIndex);
        orphanedVersions.forEach(playerVersions::remove);
        removedVersions = orphanedVersions.size();
        
        // 清理未使用的目标缓存（简化版：清理所有缓存，下次查询时重建）
        // 更精确的做法是维护缓存的最后访问时间
        if (objectiveCache.size() > 1000) { // 缓存过大时清理
            objectiveCache.clear();
            removedCaches = objectiveCache.size();
        }
        
        if (removedIndexes > 0 || removedCaches > 0 || removedVersions > 0) {
            RoadWeaverRPG.LOGGER.debug("Cleaned up objective index: {} empty indexes, {} caches, {} versions",
                    removedIndexes, removedCaches, removedVersions);
        }
    }
    
    // ==================== 私有方法 ====================
    
    private void addToIndex(String eventType, UUID playerId, IndexEntry entry) {
        eventIndex.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                  .computeIfAbsent(playerId, k -> new ArrayList<>())
                  .add(entry);
    }
    
    private void removeFromIndex(String eventType, UUID playerId, UUID instanceId) {
        Map<UUID, List<IndexEntry>> playerMap = eventIndex.get(eventType);
        if (playerMap == null) return;
        
        List<IndexEntry> entries = playerMap.get(playerId);
        if (entries != null) {
            entries.removeIf(e -> e.instanceId().equals(instanceId));
        }
    }
    
    /**
     * 根据目标类型确定事件类型
     * 
     * 改进：使用ObjectiveRegistry获取映射，支持扩展
     */
    private String getEventTypeForObjective(QuestObjective objective) {
        // 优先使用注册表中的映射
        String eventType = ObjectiveRegistry.getEventType(objective.getType());
        if (!"unknown".equals(eventType)) {
            return eventType;
        }
        
        // 兜底逻辑
        return switch (objective.getType()) {
            case KILL -> "entity_kill";
            case LOCATION_KILL -> "entity_kill";
            case COLLECT -> "inventory_check";
            case EXPLORE -> "player_move";
            case BUILD -> "block_place";
            case DELIVERY -> "item_delivery";
            case TALK -> "npc_talk";
            case ESCORT -> "entity_move";
            case COMPOSITE -> "composite_check";
        };
    }
    
    // ==================== 内部类 ====================
    
    /** 索引条目 */
    public record IndexEntry(
            UUID instanceId,
            ResourceLocation questId,
            String objectiveId,
            QuestType objectiveType,
            String cacheKey
    ) {}
    
    /** 索引统计 */
    public record IndexStats(
            long hits,
            long misses,
            long rebuilds,
            int eventTypes,
            int totalEntries,
            int cachedObjectives
    ) {
        public double hitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0;
        }
    }
}
