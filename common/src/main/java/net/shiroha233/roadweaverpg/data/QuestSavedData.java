package net.shiroha233.roadweaverpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 委托系统存档数据
 * 
 * 线程安全：使用 ConcurrentHashMap 保证并发访问安全
 */
public class QuestSavedData extends SavedData {
    
    private static final String DATA_NAME = RoadWeaverRPG.MOD_ID + "_quests";
    
    // 使用线程安全的集合
    private final Map<UUID, PlayerQuestData> playerData = new ConcurrentHashMap<>();
    
    public QuestSavedData() {}
    
    /**
     * 获取或创建玩家数据（线程安全）
     */
    public PlayerQuestData getOrCreatePlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerQuestData::new);
    }
    
    public PlayerQuestData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }
    
    /**
     * 移除玩家数据（线程安全）
     */
    public synchronized void removePlayerData(UUID playerId) {
        playerData.remove(playerId);
        setDirty();
    }
    
    /**
     * 检查玩家数据是否存在
     */
    public boolean hasPlayerData(UUID playerId) {
        return playerData.containsKey(playerId);
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playerList = new ListTag();
        for (PlayerQuestData data : playerData.values()) {
            playerList.add(data.toNbt());
        }
        tag.put("players", playerList);
        return tag;
    }
    
    public static QuestSavedData load(CompoundTag tag) {
        QuestSavedData data = new QuestSavedData();
        ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            PlayerQuestData playerData = PlayerQuestData.fromNbt(playerList.getCompound(i));
            data.playerData.put(playerData.getPlayerId(), playerData);
        }
        return data;
    }
    
    public static QuestSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) overworld = level;
        
        return overworld.getDataStorage().computeIfAbsent(
                QuestSavedData::load,
                QuestSavedData::new,
                DATA_NAME
        );
    }
}
