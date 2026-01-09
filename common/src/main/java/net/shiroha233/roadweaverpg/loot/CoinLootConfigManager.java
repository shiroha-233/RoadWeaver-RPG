package net.shiroha233.roadweaverpg.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 货币战利品配置管理器
 * 从数据包加载货币掉落配置
 * 
 * 数据包路径: data/roadweaver_rpg/loot_config/coin_loot_config.json
 */
public class CoinLootConfigManager extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "loot_config";
    
    private static volatile CoinLootConfigManager instance;
    private static final Object LOCK = new Object();
    
    // 使用ConcurrentHashMap保证线程安全
    private final Map<ResourceLocation, CoinLootConfig> configs = new ConcurrentHashMap<>();
    
    // 默认配置（如果没有加载到任何配置）
    private CoinLootConfig defaultConfig;
    
    public CoinLootConfigManager() {
        super(GSON, DIRECTORY);
        instance = this;
        initializeDefaultConfig();
    }
    
    public static CoinLootConfigManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException("CoinLootConfigManager not initialized");
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化默认配置（备用）
     */
    private void initializeDefaultConfig() {
        // 这是硬编码的备用配置，如果JSON加载失败会使用
        defaultConfig = new CoinLootConfig();
        defaultConfig.getCoins().add(createDefaultRule("copper", "roadweaver_rpg:copper_coin", 0.4, 10, 40));
        defaultConfig.getCoins().add(createDefaultRule("silver", "roadweaver_rpg:silver_coin", 0.3, 5, 15));
        defaultConfig.getCoins().add(createDefaultRule("gold", "roadweaver_rpg:gold_coin", 0.15, 2, 8));
        defaultConfig.getCoins().add(createDefaultRule("emerald", "roadweaver_rpg:emerald_coin", 0.05, 1, 3));
    }
    
    private CoinLootConfig.CoinDropRule createDefaultRule(String type, String item, 
                                                           double probability, int min, int max) {
        CoinLootConfig.CoinDropRule rule = new CoinLootConfig.CoinDropRule();
        rule.type = type;
        rule.item = item;
        rule.probability = probability;
        rule.countMin = min;
        rule.countMax = max;
        return rule;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, 
                         ResourceManager manager, ProfilerFiller profiler) {
        configs.clear();
        
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) {
                    RoadWeaverRPG.LOGGER.warn("货币战利品配置必须是JSON对象: {}", entry.getKey());
                    continue;
                }
                
                CoinLootConfig config = CoinLootConfig.fromJson(entry.getValue().getAsJsonObject());
                configs.put(entry.getKey(), config);
                count++;
                
                RoadWeaverRPG.LOGGER.info("已加载货币战利品配置: {} (包含 {} 种货币)", 
                        entry.getKey(), config.getCoins().size());
                
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("加载货币战利品配置失败: {}", entry.getKey(), e);
            }
        }
        
        if (count == 0) {
            RoadWeaverRPG.LOGGER.warn("未找到任何货币战利品配置，使用默认配置");
        } else {
            RoadWeaverRPG.LOGGER.info("已加载 {} 个货币战利品配置", count);
        }
    }
    
    /**
     * 获取货币战利品配置
     * 如果没有加载到配置，返回默认配置
     */
    public CoinLootConfig getConfig() {
        if (configs.isEmpty()) {
            return defaultConfig;
        }
        // 返回第一个配置（通常只有一个）
        return configs.values().iterator().next();
    }
    
    /**
     * 获取指定ID的配置
     */
    public CoinLootConfig getConfig(ResourceLocation id) {
        return configs.getOrDefault(id, defaultConfig);
    }
}
