package net.shiroha233.roadweaverpg.client;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;

/**
 * 客户端委托缓存
 * 缓存委托定义和当前委托实例
 */
public class ClientQuestCache {
    
    private static final List<QuestDefinition> cachedQuests = new ArrayList<>();
    private static final Map<ResourceLocation, QuestInstance> cachedInstances = new HashMap<>();
    private static QuestInstance currentInstance = null;
    
    private ClientQuestCache() {}
    
    // region 委托定义缓存
    public static void setQuests(List<QuestDefinition> quests) {
        cachedQuests.clear();
        cachedQuests.addAll(quests);
    }
    
    public static List<QuestDefinition> getQuests() {
        return Collections.unmodifiableList(cachedQuests);
    }
    
    public static Optional<QuestDefinition> getQuest(ResourceLocation id) {
        return cachedQuests.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst();
    }
    // endregion
    
    // region 委托实例缓存
    public static void setCurrentInstance(QuestInstance instance) {
        currentInstance = instance;
        if (instance != null) {
            cachedInstances.put(instance.getQuestId(), instance);
        }
    }
    
    public static QuestInstance getCurrentInstance() {
        return currentInstance;
    }
    
    public static void cacheInstance(QuestInstance instance) {
        cachedInstances.put(instance.getQuestId(), instance);
    }
    
    public static Optional<QuestInstance> getInstance(ResourceLocation questId) {
        return Optional.ofNullable(cachedInstances.get(questId));
    }
    
    public static void removeInstance(ResourceLocation questId) {
        cachedInstances.remove(questId);
        if (currentInstance != null && currentInstance.getQuestId().equals(questId)) {
            currentInstance = null;
        }
    }
    // endregion
    
    public static void clear() {
        cachedQuests.clear();
        cachedInstances.clear();
        currentInstance = null;
    }
}
