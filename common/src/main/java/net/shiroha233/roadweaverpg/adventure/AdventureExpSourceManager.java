package net.shiroha233.roadweaverpg.adventure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冒险经验来源管理器
 * 从数据包加载击杀怪物获得经验的配置
 * 路径: data/<namespace>/adventure_exp_sources/
 */
public class AdventureExpSourceManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "adventure_exp_sources";
    
    private static volatile AdventureExpSourceManager instance;
    private static final Object LOCK = new Object();
    
    // 使用线程安全的Map
    private final Map<ResourceLocation, AdventureExpSource> sources = new ConcurrentHashMap<>();
    private int defaultExp = 1;

    public AdventureExpSourceManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }

    public static AdventureExpSourceManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException("AdventureExpSourceManager not initialized");
                }
            }
        }
        return instance;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        sources.clear();
        defaultExp = 1;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                
                // 读取默认经验值
                if (json.has("default_exp")) {
                    defaultExp = json.get("default_exp").getAsInt();
                }
                
                // 批量定义
                if (json.has("sources") && json.get("sources").isJsonArray()) {
                    JsonArray sourcesArray = json.getAsJsonArray("sources");
                    for (JsonElement elem : sourcesArray) {
                        if (elem.isJsonObject()) {
                            AdventureExpSource source = AdventureExpSource.fromJson(elem.getAsJsonObject());
                            sources.put(source.getEntityType(), source);
                        }
                    }
                }
                // 单个定义
                else if (json.has("entity")) {
                    AdventureExpSource source = AdventureExpSource.fromJson(json);
                    sources.put(source.getEntityType(), source);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load adventure exp source: " + entry.getKey(), e);
            }
        }

        RoadWeaverRPG.LOGGER.info("Loaded {} adventure exp sources. Default exp: {}", sources.size(), defaultExp);
    }

    /**
     * 获取击杀指定实体类型可获得的经验值
     */
    public int getExpForEntity(ResourceLocation entityType) {
        AdventureExpSource source = sources.get(entityType);
        return source != null ? source.getBaseExp() : defaultExp;
    }

    public Map<ResourceLocation, AdventureExpSource> getAllSources() {
        return Collections.unmodifiableMap(sources);
    }

    public int getDefaultExp() { return defaultExp; }
}
