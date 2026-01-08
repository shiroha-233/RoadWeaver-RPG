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

import java.util.*;
import java.util.function.BiConsumer;

/**
 * 冒险等级管理器
 * 从数据包加载冒险等级配置
 * 路径: data/<namespace>/adventure_levels/
 */
public class AdventureLevelManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "adventure_levels";
    
    private static volatile AdventureLevelManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<Integer, AdventureLevel> levels = new TreeMap<>();
    private int maxLevel = 0;
    
    // 同步回调
    private static BiConsumer<net.minecraft.server.level.ServerPlayer, Collection<AdventureLevel>> syncCallback;

    public AdventureLevelManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }

    public static AdventureLevelManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException("AdventureLevelManager not initialized");
                }
            }
        }
        return instance;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        levels.clear();
        maxLevel = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                
                // 支持两种格式：批量定义或单个定义
                if (json.has("levels") && json.get("levels").isJsonArray()) {
                    JsonArray levelsArray = json.getAsJsonArray("levels");
                    for (JsonElement levelElem : levelsArray) {
                        if (levelElem.isJsonObject()) {
                            parseAndAddLevel(levelElem.getAsJsonObject());
                        }
                    }
                } else if (json.has("level")) {
                    parseAndAddLevel(json);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load adventure level config: " + entry.getKey(), e);
            }
        }

        RoadWeaverRPG.LOGGER.info("Loaded {} adventure levels. Max level: {}", levels.size(), maxLevel);
    }

    private void parseAndAddLevel(JsonObject json) {
        if (json.has("level")) {
            int level = json.get("level").getAsInt();
            AdventureLevel advLevel = AdventureLevel.fromJson(level, json);
            levels.put(level, advLevel);
            maxLevel = Math.max(maxLevel, level);
        }
    }

    public AdventureLevel getLevelInfo(int level) {
        return levels.get(level);
    }

    public int getMaxLevel() { return maxLevel; }

    public Map<Integer, AdventureLevel> getAllLevels() {
        return Collections.unmodifiableMap(levels);
    }

    /**
     * 计算指定经验值对应的等级
     */
    public int getLevelForExperience(int experience) {
        int currentLevel = 0;
        for (Map.Entry<Integer, AdventureLevel> entry : levels.entrySet()) {
            if (experience >= entry.getValue().getRequiredExperience()) {
                currentLevel = entry.getKey();
            } else {
                break;
            }
        }
        return currentLevel;
    }

    /**
     * 获取下一级所需经验
     */
    public int getExperienceForNextLevel(int currentLevel) {
        AdventureLevel nextLevel = levels.get(currentLevel + 1);
        return nextLevel != null ? nextLevel.getRequiredExperience() : -1;
    }

    public static void setSyncCallback(BiConsumer<net.minecraft.server.level.ServerPlayer, Collection<AdventureLevel>> callback) {
        syncCallback = callback;
    }

    public void syncToClient(net.minecraft.server.level.ServerPlayer player) {
        if (syncCallback != null) {
            syncCallback.accept(player, levels.values());
        }
    }
}
