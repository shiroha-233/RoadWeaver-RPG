package net.shiroha233.roadweaverpg.quest.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestRank;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;

import java.util.*;

/**
 * 委托定义加载器
 * 从数据包加载委托定义
 * 
 * 数据包路径: data/<namespace>/quests/<path>.json
 */
public class QuestDefinitionLoader extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "quests";
    
    private static QuestDefinitionLoader instance;
    
    private final Map<ResourceLocation, QuestDefinition> definitions = new HashMap<>();
    private final Map<QuestRank, List<QuestDefinition>> byRank = new EnumMap<>(QuestRank.class);
    private final Map<ResourceLocation, Set<ResourceLocation>> unlockGraph = new HashMap<>();
    
    public QuestDefinitionLoader() {
        super(GSON, DIRECTORY);
        instance = this;
        for (QuestRank rank : QuestRank.values()) {
            byRank.put(rank, new ArrayList<>());
        }
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, 
                         ResourceManager manager, ProfilerFiller profiler) {
        definitions.clear();
        byRank.values().forEach(List::clear);
        unlockGraph.clear();
        
        resources.forEach((id, element) -> {
            try {
                if (element.isJsonObject()) {
                    QuestDefinition def = QuestDefinition.fromJson(id, element.getAsJsonObject());
                    definitions.put(id, def);
                    byRank.get(def.getRank()).add(def);
                    
                    for (ResourceLocation unlock : def.getUnlocks()) {
                        unlockGraph.computeIfAbsent(id, k -> new HashSet<>()).add(unlock);
                    }
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load quest definition: {}", id, e);
            }
        });
        
        validateQuestChains();
        RoadWeaverRPG.LOGGER.info("Loaded {} quest definitions", definitions.size());
    }
    
    private void validateQuestChains() {
        for (QuestDefinition def : definitions.values()) {
            for (ResourceLocation prereq : def.getPrerequisites()) {
                if (!definitions.containsKey(prereq)) {
                    RoadWeaverRPG.LOGGER.warn("Quest {} has missing prerequisite: {}", 
                            def.getId(), prereq);
                }
            }
            for (ResourceLocation unlock : def.getUnlocks()) {
                if (!definitions.containsKey(unlock)) {
                    RoadWeaverRPG.LOGGER.warn("Quest {} unlocks non-existent quest: {}", 
                            def.getId(), unlock);
                }
            }
        }
    }

    
    // region 查询方法
    public static QuestDefinitionLoader getInstance() {
        return instance;
    }
    
    public QuestDefinition getDefinition(ResourceLocation id) {
        return definitions.get(id);
    }
    
    public Collection<QuestDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }
    
    public List<QuestDefinition> getDefinitionsByRank(QuestRank rank) {
        return Collections.unmodifiableList(byRank.getOrDefault(rank, Collections.emptyList()));
    }
    
    public boolean exists(ResourceLocation id) {
        return definitions.containsKey(id);
    }
    
    public Set<ResourceLocation> getUnlockedQuests(ResourceLocation questId) {
        return unlockGraph.getOrDefault(questId, Collections.emptySet());
    }
    
    public boolean checkPrerequisites(ResourceLocation questId, Set<ResourceLocation> completedQuests) {
        QuestDefinition def = definitions.get(questId);
        if (def == null) return false;
        
        for (ResourceLocation prereq : def.getPrerequisites()) {
            if (!completedQuests.contains(prereq)) {
                return false;
            }
        }
        return true;
    }
    
    public List<QuestDefinition> getAvailableQuests(Set<ResourceLocation> completedQuests, 
                                                     Set<ResourceLocation> activeQuests) {
        List<QuestDefinition> available = new ArrayList<>();
        
        for (QuestDefinition def : definitions.values()) {
            ResourceLocation id = def.getId();
            
            if (completedQuests.contains(id) && !def.isRepeatable()) {
                continue;
            }
            if (activeQuests.contains(id)) {
                continue;
            }
            if (checkPrerequisites(id, completedQuests)) {
                available.add(def);
            }
        }
        return available;
    }
    // endregion
}
