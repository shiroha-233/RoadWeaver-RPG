package net.shiroha233.roadweaverpg.client;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 客户端声望数据缓存
 * 
 * 线程安全：客户端单线程，无需同步
 * 内存管理：在玩家登出或切换世界时调用 clear()
 */
public class ClientReputationCache {
    private static final Map<Integer, ReputationLevel> levelDefinitions = new TreeMap<>();
    private static final Map<String, Integer> playerReputations = new HashMap<>();
    private static final Map<String, Integer> playerLevels = new HashMap<>();

    private ClientReputationCache() {}

    public static void setLevelDefinitions(Collection<ReputationLevel> levels) {
        levelDefinitions.clear();
        for (ReputationLevel level : levels) {
            levelDefinitions.put(level.getLevel(), level);
        }
    }

    public static void setPlayerData(Map<String, Integer> reputations, Map<String, Integer> levels) {
        playerReputations.clear();
        playerReputations.putAll(reputations);
        playerLevels.clear();
        playerLevels.putAll(levels);
    }

    public static Map<Integer, ReputationLevel> getLevelDefinitions() {
        return Collections.unmodifiableMap(levelDefinitions);
    }

    public static int getPlayerExperience(String factionId) {
        return playerReputations.getOrDefault(factionId, 0);
    }
    
    public static int getPlayerExperience(ResourceLocation factionId) {
        return getPlayerExperience(factionId.toString());
    }

    public static int getPlayerLevel(String factionId) {
        return playerLevels.getOrDefault(factionId, 0);
    }
    
    public static int getPlayerLevel(ResourceLocation factionId) {
        return getPlayerLevel(factionId.toString());
    }

    public static ReputationLevel getLevelInfo(int level) {
        return levelDefinitions.get(level);
    }
    
    /**
     * 获取下一级所需经验
     */
    public static int getExpForNextLevel(String factionId) {
        int currentLevel = getPlayerLevel(factionId);
        ReputationLevel nextLevel = levelDefinitions.get(currentLevel + 1);
        return nextLevel != null ? nextLevel.getRequiredExperience() : -1;
    }
    
    /**
     * 获取当前级所需经验
     */
    public static int getExpForCurrentLevel(String factionId) {
        int currentLevel = getPlayerLevel(factionId);
        ReputationLevel levelInfo = levelDefinitions.get(currentLevel);
        return levelInfo != null ? levelInfo.getRequiredExperience() : 0;
    }
    
    /**
     * 清理所有缓存
     * 在玩家登出或切换世界时调用
     */
    public static void clear() {
        levelDefinitions.clear();
        playerReputations.clear();
        playerLevels.clear();
    }
}
