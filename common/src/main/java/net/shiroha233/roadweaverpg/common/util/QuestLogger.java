package net.shiroha233.roadweaverpg.common.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import org.slf4j.Logger;

/**
 * 委托系统日志工具
 */
public final class QuestLogger {
    
    private static final Logger LOGGER = RoadWeaverRPG.LOGGER;
    private static boolean debugEnabled = false;
    
    private QuestLogger() {}
    
    public static void setDebugEnabled(boolean enabled) { debugEnabled = enabled; }
    
    public static void logQuestAccepted(ServerPlayer player, ResourceLocation questId) {
        LOGGER.info("[Quest] {} accepted quest: {}", playerName(player), questId);
    }
    
    public static void logQuestAbandoned(ServerPlayer player, ResourceLocation questId) {
        LOGGER.info("[Quest] {} abandoned quest: {}", playerName(player), questId);
    }
    
    public static void logQuestCompleted(ServerPlayer player, ResourceLocation questId) {
        LOGGER.info("[Quest] {} completed quest: {}", playerName(player), questId);
    }
    
    public static void logQuestTurnedIn(ServerPlayer player, ResourceLocation questId) {
        LOGGER.info("[Quest] {} turned in quest: {}", playerName(player), questId);
    }
    
    public static void logQuestExpired(ServerPlayer player, ResourceLocation questId) {
        LOGGER.info("[Quest] Quest {} expired for player: {}", questId, playerName(player));
    }
    
    public static void logQuestStateChange(ServerPlayer player, ResourceLocation questId, 
                                           QuestState oldState, QuestState newState) {
        if (debugEnabled) {
            LOGGER.debug("[Quest] {} quest {} state: {} -> {}", 
                    playerName(player), questId, oldState, newState);
        }
    }
    
    public static void logProgressUpdate(ServerPlayer player, ResourceLocation questId, 
                                         String objectiveId, int oldProgress, int newProgress) {
        if (debugEnabled) {
            LOGGER.debug("[Quest] {} progress update: {} objective {} ({} -> {})", 
                    playerName(player), questId, objectiveId, oldProgress, newProgress);
        }
    }
    
    public static void logReputationChange(ServerPlayer player, ResourceLocation factionId, 
                                           int oldXp, int newXp) {
        LOGGER.info("[Reputation] {} faction {} XP: {} -> {}", 
                playerName(player), factionId, oldXp, newXp);
    }
    
    public static void logReputationLevelUp(ServerPlayer player, ResourceLocation factionId, 
                                            int oldLevel, int newLevel) {
        LOGGER.info("[Reputation] {} faction {} level up: {} -> {}", 
                playerName(player), factionId, oldLevel, newLevel);
    }
    
    public static void logError(String message, Object... args) {
        LOGGER.error("[Quest] " + message, args);
    }
    
    public static void logError(String message, Throwable throwable) {
        LOGGER.error("[Quest] " + message, throwable);
    }
    
    public static void logWarning(String message, Object... args) {
        LOGGER.warn("[Quest] " + message, args);
    }
    
    public static void logDebug(String message, Object... args) {
        if (debugEnabled) {
            LOGGER.debug("[Quest] " + message, args);
        }
    }
    
    private static String playerName(ServerPlayer player) {
        return player != null ? player.getName().getString() : "Unknown";
    }
}
