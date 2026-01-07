package net.shiroha233.roadweaverpg.reputation;

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

/**
 * 声望管理器
 * 负责从数据包加载声望等级配置
 * 路径: data/<namespace>/reputation/levels.json 或 levels/<level>.json
 */
public class ReputationManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "reputation_levels";
    
    private static volatile ReputationManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<Integer, ReputationLevel> levels = new TreeMap<>();
    private int maxLevel = 0;
    private static java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer, Collection<ReputationLevel>> syncCallback;

    public ReputationManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }

    /**
     * 获取单例实例（双重检查锁定）
     */
    public static ReputationManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException("ReputationManager not initialized");
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
                
                // 支持两种格式：
                // 1. 单个文件定义多个等级 {"levels": [...]}
                // 2. 单个文件定义一个等级 {"level": 1, ...}
                
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
                RoadWeaverRPG.LOGGER.error("Failed to load reputation config: " + entry.getKey(), e);
            }
        }

        RoadWeaverRPG.LOGGER.info("Loaded {} reputation levels. Max level: {}", levels.size(), maxLevel);
    }

    private void parseAndAddLevel(JsonObject json) {
        if (json.has("level")) {
            int level = json.get("level").getAsInt();
            ReputationLevel repLevel = ReputationLevel.fromJson(level, json);
            levels.put(level, repLevel);
            maxLevel = Math.max(maxLevel, level);
        }
    }

    public ReputationLevel getLevelInfo(int level) {
        return levels.get(level);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Map<Integer, ReputationLevel> getAllLevels() {
        return Collections.unmodifiableMap(levels);
    }

    /**
     * 计算指定经验值对应的等级
     */
    public int getLevelForExperience(int experience) {
        int currentLevel = 0;
        for (Map.Entry<Integer, ReputationLevel> entry : levels.entrySet()) {
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
        ReputationLevel nextLevel = levels.get(currentLevel + 1);
        return nextLevel != null ? nextLevel.getRequiredExperience() : -1;
    }

    public static void setSyncCallback(java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer, Collection<ReputationLevel>> callback) {
        syncCallback = callback;
    }

    public void syncToClient(net.minecraft.server.level.ServerPlayer player) {
        if (syncCallback != null) {
            syncCallback.accept(player, levels.values());
        }
    }
}
