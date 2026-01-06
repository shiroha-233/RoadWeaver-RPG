package net.shiroha233.roadweaverpg.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 委托数据访问器
 */
public class QuestDataAccessor {
    
    private static QuestDataAccessor instance;
    
    private QuestDataAccessor() {}
    
    public static QuestDataAccessor getInstance() {
        if (instance == null) {
            instance = new QuestDataAccessor();
        }
        return instance;
    }
    
    public PlayerQuestData getPlayerData(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        QuestSavedData savedData = QuestSavedData.get(level);
        return savedData.getOrCreatePlayerData(player.getUUID());
    }
    
    public void markDirty(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        QuestSavedData.get(level).setDirty();
    }
    
    public QuestSavedData getSavedData(ServerLevel level) {
        return QuestSavedData.get(level);
    }
}
