package net.shiroha233.roadweaverpg.quest.chain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.*;

/**
 * 委托链管理器
 * 从数据包加载委托链定义
 * 
 * 数据包路径: data/<namespace>/quest_chains/<path>.json
 */
public class QuestChainManager extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "quest_chains";
    
    private static QuestChainManager instance;
    
    private final Map<ResourceLocation, QuestChain> chains = new HashMap<>();
    
    // 委托到链的反向索引
    private final Map<ResourceLocation, Set<ResourceLocation>> questToChains = new HashMap<>();
    
    public QuestChainManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, 
                         ResourceManager manager, ProfilerFiller profiler) {
        chains.clear();
        questToChains.clear();
        
        resources.forEach((id, element) -> {
            try {
                if (element.isJsonObject()) {
                    QuestChain chain = QuestChain.fromJson(id, element.getAsJsonObject());
                    chains.put(id, chain);
                    
                    // 构建反向索引
                    for (ResourceLocation questId : chain.getQuestSequence()) {
                        questToChains.computeIfAbsent(questId, k -> new HashSet<>()).add(id);
                    }
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load quest chain: {}", id, e);
            }
        });
        
        RoadWeaverRPG.LOGGER.info("Loaded {} quest chains", chains.size());
    }
    
    public static QuestChainManager getInstance() {
        return instance;
    }
    
    public QuestChain getChain(ResourceLocation id) {
        return chains.get(id);
    }
    
    public Collection<QuestChain> getAllChains() {
        return Collections.unmodifiableCollection(chains.values());
    }
    
    /** 获取包含指定委托的所有链 */
    public Set<ResourceLocation> getChainsContaining(ResourceLocation questId) {
        return questToChains.getOrDefault(questId, Collections.emptySet());
    }
    
    /** 计算玩家在链中的进度 */
    public ChainProgress getChainProgress(ResourceLocation chainId, Set<ResourceLocation> completedQuests) {
        QuestChain chain = chains.get(chainId);
        if (chain == null) return null;
        
        int completed = 0;
        int total = chain.getQuestCount();
        
        for (ResourceLocation questId : chain.getQuestSequence()) {
            if (completedQuests.contains(questId)) {
                completed++;
            }
        }
        
        return new ChainProgress(chainId, completed, total, completed == total);
    }
    
    /** 链进度记录 */
    public record ChainProgress(ResourceLocation chainId, int completed, int total, boolean isComplete) {
        public float getPercent() {
            return total > 0 ? (float) completed / total : 0f;
        }
    }
}
