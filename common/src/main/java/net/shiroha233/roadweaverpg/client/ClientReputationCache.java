package net.shiroha233.roadweaverpg.client;

import net.shiroha233.roadweaverpg.reputation.ReputationLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 客户端声望数据缓存
 */
public class ClientReputationCache {
    private static final Map<Integer, ReputationLevel> levelDefinitions = new TreeMap<>();
    private static final Map<String, Integer> playerReputations = new HashMap<>();
    private static final Map<String, Integer> playerLevels = new HashMap<>();

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

    public static int getPlayerLevel(String factionId) {
        return playerLevels.getOrDefault(factionId, 0);
    }

    public static ReputationLevel getLevelInfo(int level) {
        return levelDefinitions.get(level);
    }
}
