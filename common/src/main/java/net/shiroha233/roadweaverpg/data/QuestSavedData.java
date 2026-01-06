package net.shiroha233.roadweaverpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 委托系统存档数据
 */
public class QuestSavedData extends SavedData {
    
    private static final String DATA_NAME = RoadWeaverRPG.MOD_ID + "_quests";
    private final Map<UUID, PlayerQuestData> playerData = new HashMap<>();
    
    public QuestSavedData() {}
    
    public PlayerQuestData getOrCreatePlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerQuestData::new);
    }
    
    public PlayerQuestData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }
    
    public void removePlayerData(UUID playerId) {
        playerData.remove(playerId);
        setDirty();
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
