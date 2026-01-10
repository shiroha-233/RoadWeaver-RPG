package net.shiroha233.roadweaverpg.worlddifficulty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 难度配置管理器
 * 
 * 设计原理：
 * - 从数据包加载难度缩放配置
 * - 支持全局配置和指定怪物配置
 * - 线程安全的配置存储
 * 
 * 路径: data/<namespace>/difficulty_scaling/
 */
public class DifficultyConfigManager extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "difficulty_scaling";
    
    private static volatile DifficultyConfigManager instance;
    private static final Object LOCK = new Object();
    
    // 全局配置
    private volatile DifficultyScaling globalConfig = DifficultyScaling.createDefault();
    
    // 指定怪物配置 (怪物ID -> 配置)
    private final ConcurrentHashMap<ResourceLocation, DifficultyScaling> mobConfigs = new ConcurrentHashMap<>();
    
    // 同步回调
    private static BiConsumer<net.minecraft.server.level.ServerPlayer, Collection<DifficultyScaling>> syncCallback;
    
    public DifficultyConfigManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }
    
    public static DifficultyConfigManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    // 创建默认实例（用于客户端或未初始化时）
                    instance = new DifficultyConfigManager();
                }
            }
        }
        return instance;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        mobConfigs.clear();
        globalConfig = DifficultyScaling.createDefault();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                DifficultyScaling scaling = DifficultyScaling.fromJson(entry.getKey(), json);
                
                if (scaling.isGlobal()) {
                    globalConfig = scaling;
                    RoadWeaverRPG.LOGGER.info("加载全局难度配置: {}", entry.getKey());
                } else {
                    // 为每个目标怪物注册配置
                    for (ResourceLocation mob : scaling.targetMobs()) {
                        mobConfigs.put(mob, scaling);
                    }
                    RoadWeaverRPG.LOGGER.info("加载怪物难度配置: {} -> {} 种怪物", 
                            entry.getKey(), scaling.targetMobs().size());
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("加载难度配置失败: {}", entry.getKey(), e);
            }
        }
        
        RoadWeaverRPG.LOGGER.info("难度配置加载完成: 全局配置 + {} 个怪物特定配置", mobConfigs.size());
    }
    
    /**
     * 获取指定怪物的难度配置
     */
    public DifficultyScaling getConfigForMob(ResourceLocation mobId) {
        return mobConfigs.getOrDefault(mobId, globalConfig);
    }
    
    /**
     * 获取全局配置
     */
    public DifficultyScaling getGlobalConfig() {
        return globalConfig;
    }
    
    /**
     * 获取所有配置
     */
    public Collection<DifficultyScaling> getAllConfigs() {
        var list = new java.util.ArrayList<>(mobConfigs.values());
        list.add(globalConfig);
        return list;
    }
    
    /**
     * 检查怪物是否有特定配置
     */
    public boolean hasMobSpecificConfig(ResourceLocation mobId) {
        return mobConfigs.containsKey(mobId);
    }
    
    public static void setSyncCallback(BiConsumer<net.minecraft.server.level.ServerPlayer, Collection<DifficultyScaling>> callback) {
        syncCallback = callback;
    }
    
    public void syncToClient(net.minecraft.server.level.ServerPlayer player) {
        if (syncCallback != null) {
            syncCallback.accept(player, getAllConfigs());
        }
    }
}
