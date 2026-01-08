package net.shiroha233.roadweaverpg.client;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 客户端委托缓存
 * 
 * 修复：
 * - 分离"可接取委托列表"和"所有委托定义缓存"
 * - 委托定义使用合并策略，不会因为打开看板而丢失已接取委托的定义
 * - 使用线程安全的集合
 */
public class ClientQuestCache {
    
    // 可接取的委托列表（用于委托看板显示）
    private static final List<QuestDefinition> availableQuests = new CopyOnWriteArrayList<>();
    
    // 所有委托定义缓存（使用 Map 便于查找，不会被清空）
    private static final Map<ResourceLocation, QuestDefinition> definitionCache = new ConcurrentHashMap<>();
    
    // 委托实例缓存
    private static final Map<UUID, QuestInstance> instancesByUUID = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<UUID>> questIdToInstances = new ConcurrentHashMap<>();
    
    private static volatile QuestInstance currentInstance = null;
    
    private ClientQuestCache() {}
    
    // region 委托定义缓存
    
    /**
     * 设置可接取的委托列表（用于委托看板）
     * 同时将定义添加到缓存中（合并策略）
     */
    public static void setQuests(List<QuestDefinition> quests) {
        availableQuests.clear();
        if (quests != null) {
            availableQuests.addAll(quests);
            // 合并到定义缓存，不清空已有的
            for (QuestDefinition def : quests) {
                definitionCache.put(def.getId(), def);
            }
        }
    }
    
    /**
     * 设置所有委托定义（登录时同步）
     * 这会替换整个定义缓存
     */
    public static void setAllDefinitions(List<QuestDefinition> definitions) {
        definitionCache.clear();
        if (definitions != null) {
            for (QuestDefinition def : definitions) {
                definitionCache.put(def.getId(), def);
            }
        }
    }
    
    /**
     * 添加或更新单个委托定义
     */
    public static void cacheDefinition(QuestDefinition definition) {
        if (definition != null) {
            definitionCache.put(definition.getId(), definition);
        }
    }
    
    /**
     * 获取可接取的委托列表（用于委托看板）
     */
    public static List<QuestDefinition> getQuests() {
        return Collections.unmodifiableList(new ArrayList<>(availableQuests));
    }
    
    /**
     * 获取委托定义（从缓存中查找）
     */
    public static Optional<QuestDefinition> getQuest(ResourceLocation id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(definitionCache.get(id));
    }
    
    /**
     * 获取所有缓存的委托定义
     */
    public static Collection<QuestDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitionCache.values());
    }
    // endregion
    
    // region 委托实例缓存
    public static void setCurrentInstance(QuestInstance instance) {
        currentInstance = instance;
        if (instance != null) {
            cacheInstance(instance);
        }
    }
    
    public static QuestInstance getCurrentInstance() {
        return currentInstance;
    }
    
    /**
     * 缓存实例（使用instanceId作为主键）
     */
    public static synchronized void cacheInstance(QuestInstance instance) {
        if (instance == null) return;
        
        UUID instanceId = instance.getInstanceId();
        ResourceLocation questId = instance.getQuestId();
        
        instancesByUUID.put(instanceId, instance);
        
        questIdToInstances.compute(questId, (key, existingList) -> {
            if (existingList == null) {
                existingList = new CopyOnWriteArrayList<>();
            }
            existingList.remove(instanceId);
            existingList.add(instanceId);
            return existingList;
        });
    }
    
    /**
     * 批量缓存实例（用于登录时同步）
     */
    public static synchronized void cacheAllInstances(Collection<QuestInstance> instances) {
        if (instances == null) return;
        for (QuestInstance instance : instances) {
            cacheInstance(instance);
        }
    }
    
    /**
     * 通过instanceId获取实例（精确匹配）
     */
    public static Optional<QuestInstance> getInstanceByUUID(UUID instanceId) {
        if (instanceId == null) return Optional.empty();
        return Optional.ofNullable(instancesByUUID.get(instanceId));
    }
    
    /**
     * 通过questId获取实例（兼容旧代码，返回第一个匹配的）
     */
    public static Optional<QuestInstance> getInstance(ResourceLocation questId) {
        if (questId == null) return Optional.empty();
        
        List<UUID> instanceIds = questIdToInstances.get(questId);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Optional.empty();
        }
        
        for (UUID id : instanceIds) {
            QuestInstance inst = instancesByUUID.get(id);
            if (inst != null) {
                return Optional.of(inst);
            }
        }
        return Optional.empty();
    }
    
    /**
     * 获取questId对应的所有实例
     */
    public static List<QuestInstance> getAllInstances(ResourceLocation questId) {
        if (questId == null) return Collections.emptyList();
        
        List<UUID> instanceIds = questIdToInstances.get(questId);
        if (instanceIds == null) return Collections.emptyList();
        
        List<QuestInstance> result = new ArrayList<>();
        for (UUID id : instanceIds) {
            QuestInstance inst = instancesByUUID.get(id);
            if (inst != null) {
                result.add(inst);
            }
        }
        return result;
    }
    
    /**
     * 获取所有缓存的实例
     */
    public static Collection<QuestInstance> getAllCachedInstances() {
        return Collections.unmodifiableCollection(instancesByUUID.values());
    }
    
    /**
     * 检查是否有缓存的实例
     */
    public static boolean hasInstance(ResourceLocation questId) {
        return getInstance(questId).isPresent();
    }
    
    /**
     * 检查是否有缓存的实例（通过UUID）
     */
    public static boolean hasInstanceByUUID(UUID instanceId) {
        return instancesByUUID.containsKey(instanceId);
    }
    
    public static synchronized void removeInstance(ResourceLocation questId) {
        if (questId == null) return;
        
        List<UUID> instanceIds = questIdToInstances.remove(questId);
        if (instanceIds != null) {
            for (UUID id : instanceIds) {
                instancesByUUID.remove(id);
            }
        }
        
        if (currentInstance != null && currentInstance.getQuestId().equals(questId)) {
            currentInstance = null;
        }
    }
    
    public static synchronized void removeInstanceByUUID(UUID instanceId) {
        if (instanceId == null) return;
        
        QuestInstance removed = instancesByUUID.remove(instanceId);
        if (removed != null) {
            List<UUID> list = questIdToInstances.get(removed.getQuestId());
            if (list != null) {
                list.remove(instanceId);
                if (list.isEmpty()) {
                    questIdToInstances.remove(removed.getQuestId());
                }
            }
        }
        
        if (currentInstance != null && currentInstance.getInstanceId().equals(instanceId)) {
            currentInstance = null;
        }
    }
    // endregion
    
    /**
     * 清理所有缓存
     */
    public static synchronized void clear() {
        availableQuests.clear();
        definitionCache.clear();
        instancesByUUID.clear();
        questIdToInstances.clear();
        currentInstance = null;
    }
    
    /**
     * 获取缓存统计信息（调试用）
     */
    public static String getStats() {
        return String.format("ClientQuestCache: %d available, %d definitions cached, %d instances", 
                availableQuests.size(), definitionCache.size(), instancesByUUID.size());
    }
}
