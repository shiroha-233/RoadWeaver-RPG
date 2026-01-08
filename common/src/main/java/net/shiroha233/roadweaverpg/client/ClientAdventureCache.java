package net.shiroha233.roadweaverpg.client;

import net.shiroha233.roadweaverpg.adventure.AdventureLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 客户端冒险等级数据缓存
 * 
 * 线程安全：客户端单线程，无需同步
 * 内存管理：在玩家登出或切换世界时调用 clear()
 */
public class ClientAdventureCache {
    private static final Map<Integer, AdventureLevel> levelDefinitions = new TreeMap<>();
    private static int playerExp = 0;
    private static int playerLevel = 0;

    private ClientAdventureCache() {}

    public static void setLevelDefinitions(Collection<AdventureLevel> levels) {
        levelDefinitions.clear();
        for (AdventureLevel level : levels) {
            levelDefinitions.put(level.getLevel(), level);
        }
    }

    public static void setPlayerData(int exp, int level) {
        playerExp = exp;
        playerLevel = level;
    }

    public static Map<Integer, AdventureLevel> getLevelDefinitions() {
        return Collections.unmodifiableMap(levelDefinitions);
    }

    public static int getPlayerExp() { return playerExp; }
    public static int getPlayerLevel() { return playerLevel; }

    public static AdventureLevel getLevelInfo(int level) {
        return levelDefinitions.get(level);
    }
    
    /**
     * 获取下一级所需经验
     */
    public static int getExpForNextLevel() {
        AdventureLevel nextLevel = levelDefinitions.get(playerLevel + 1);
        return nextLevel != null ? nextLevel.getRequiredExperience() : -1;
    }
    
    /**
     * 获取当前等级所需经验
     */
    public static int getExpForCurrentLevel() {
        AdventureLevel currentLevel = levelDefinitions.get(playerLevel);
        return currentLevel != null ? currentLevel.getRequiredExperience() : 0;
    }

    /**
     * 清理所有缓存
     */
    public static void clear() {
        levelDefinitions.clear();
        playerExp = 0;
        playerLevel = 0;
    }
}
