package net.shiroha233.roadweaverpg.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 委托数据访问器
 * 
 * 修复：增强 markDirty 的可靠性，添加日志
 */
public class QuestDataAccessor {
    
    private static volatile QuestDataAccessor instance;
    private static final Object LOCK = new Object();
    
    private QuestDataAccessor() {}
    
    public static QuestDataAccessor getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new QuestDataAccessor();
                }
            }
        }
        return instance;
    }
    
    public PlayerQuestData getPlayerData(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        QuestSavedData savedData = QuestSavedData.get(level);
        return savedData.getOrCreatePlayerData(player.getUUID());
    }
    
    /**
     * 标记数据需要保存
     * 修复：确保正确获取 SavedData 并标记
     */
    public void markDirty(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            QuestSavedData savedData = QuestSavedData.get(level);
            savedData.setDirty();
            RoadWeaverRPG.LOGGER.debug("Marked QuestSavedData dirty for player: {}", 
                    player.getName().getString());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to mark dirty for player {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 强制保存数据（用于关键操作后）
     */
    public void forceSave(ServerPlayer player) {
        markDirty(player);
    }
    
    public QuestSavedData getSavedData(ServerLevel level) {
        return QuestSavedData.get(level);
    }
}
