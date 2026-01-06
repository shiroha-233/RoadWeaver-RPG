package net.shiroha233.roadweaverpg.data;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 委托缓存
 */
public class QuestCache {
    
    private static QuestCache instance;
    
    private final Map<ResourceLocation, QuestDefinition> definitionCache = new ConcurrentHashMap<>();
    private final Map<String, List<QuestDefinition>> rankIndex = new ConcurrentHashMap<>();
    private final Map<String, List<QuestDefinition>> typeIndex = new ConcurrentHashMap<>();
    private final Map<UUID, CachedAvailableQuests> availableQuestsCache = new ConcurrentHashMap<>();
    
    private static final long CACHE_TTL = 5000;
    
    private QuestCache() {}
    
    public static QuestCache getInstance() {
        if (instance == null) {
            instance = new QuestCache();
        }
        return instance;
    }
    
    public void updateDefinitions(Collection<QuestDefinition> definitions) {
        definitionCache.clear();
        rankIndex.clear();
        typeIndex.clear();
        
        for (QuestDefinition def : definitions) {
            definitionCache.put(def.getId(), def);
            String rankKey = def.getRank().name();
            rankIndex.computeIfAbsent(rankKey, k -> new ArrayList<>()).add(def);
            String typeKey = def.getPrimaryType().name();
            typeIndex.computeIfAbsent(typeKey, k -> new ArrayList<>()).add(def);
        }
        availableQuestsCache.clear();
    }
    
    public Optional<QuestDefinition> getDefinition(ResourceLocation id) {
        return Optional.ofNullable(definitionCache.get(id));
    }
    
    public List<QuestDefinition> getByRank(String rank) {
        return rankIndex.getOrDefault(rank, Collections.emptyList());
    }
    
    public List<QuestDefinition> getByType(String type) {
        return typeIndex.getOrDefault(type, Collections.emptyList());
    }
    
    public Collection<QuestDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionCache.values());
    }
    
    public void cacheAvailableQuests(UUID playerId, List<QuestDefinition> quests) {
        availableQuestsCache.put(playerId, new CachedAvailableQuests(quests, System.currentTimeMillis()));
    }
    
    public Optional<List<QuestDefinition>> getCachedAvailableQuests(UUID playerId) {
        CachedAvailableQuests cached = availableQuestsCache.get(playerId);
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached.quests);
        }
        availableQuestsCache.remove(playerId);
        return Optional.empty();
    }
    
    public void invalidatePlayerCache(UUID playerId) {
        availableQuestsCache.remove(playerId);
    }
    
    public void clear() {
        definitionCache.clear();
        rankIndex.clear();
        typeIndex.clear();
        availableQuestsCache.clear();
    }
    
    private record CachedAvailableQuests(List<QuestDefinition> quests, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
}
