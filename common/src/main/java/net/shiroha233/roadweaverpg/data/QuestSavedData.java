package net.shiroha233.roadweaverpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.reward.RewardQueueManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 委托系统存档数据
 * 
 * 修复：
 * - 新建玩家数据时立即标记脏数据
 * - 添加日志便于调试持久化问题
 * - 确保所有数据修改都触发保存
 */
public class QuestSavedData extends SavedData {
    
    private static final String DATA_NAME = RoadWeaverRPG.MOD_ID + "_quests";
    
    private final Map<UUID, PlayerQuestData> playerData = new ConcurrentHashMap<>();
    
    public QuestSavedData() {
        RoadWeaverRPG.LOGGER.debug("Created new QuestSavedData instance");
    }
    
    /**
     * 获取或创建玩家数据
     * 修复：新建数据时立即标记脏数据，确保持久化
     */
    public PlayerQuestData getOrCreatePlayerData(UUID playerId) {
        PlayerQuestData existing = playerData.get(playerId);
        if (existing != null) {
            return existing;
        }
        
        // 使用 synchronized 确保原子性创建
        synchronized (this) {
            existing = playerData.get(playerId);
            if (existing != null) {
                return existing;
            }
            
            PlayerQuestData newData = new PlayerQuestData(playerId);
            playerData.put(playerId, newData);
            setDirty(); // 关键：新建数据时立即标记脏
            RoadWeaverRPG.LOGGER.info("Created new PlayerQuestData for player: {}", playerId);
            return newData;
        }
    }
    
    public PlayerQuestData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }
    
    public synchronized void removePlayerData(UUID playerId) {
        PlayerQuestData removed = playerData.remove(playerId);
        if (removed != null) {
            setDirty();
            RoadWeaverRPG.LOGGER.debug("Removed PlayerQuestData for player: {}", playerId);
        }
    }
    
    public boolean hasPlayerData(UUID playerId) {
        return playerData.containsKey(playerId);
    }
    
    /**
     * 获取所有玩家数据数量（调试用）
     */
    public int getPlayerCount() {
        return playerData.size();
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        // 保存玩家委托数据
        ListTag playerList = new ListTag();
        int savedCount = 0;
        
        for (PlayerQuestData data : playerData.values()) {
            try {
                CompoundTag playerTag = data.toNbt();
                playerList.add(playerTag);
                savedCount++;
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to save PlayerQuestData for {}: {}", 
                        data.getPlayerId(), e.getMessage());
            }
        }
        tag.put("players", playerList);
        
        // 保存奖励队列
        try {
            RewardQueueManager rewardManager = RewardQueueManager.getInstance();
            if (rewardManager.isDirty()) {
                tag.put("rewardQueue", rewardManager.toNbt());
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to save reward queue: {}", e.getMessage());
        }
        
        RoadWeaverRPG.LOGGER.info("Saved QuestSavedData: {} players", savedCount);
        return tag;
    }
    
    public static QuestSavedData load(CompoundTag tag) {
        QuestSavedData data = new QuestSavedData();
        int loadedCount = 0;
        
        // 加载玩家委托数据
        if (tag.contains("players")) {
            ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < playerList.size(); i++) {
                try {
                    PlayerQuestData playerData = PlayerQuestData.fromNbt(playerList.getCompound(i));
                    data.playerData.put(playerData.getPlayerId(), playerData);
                    loadedCount++;
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.error("Failed to load PlayerQuestData at index {}: {}", i, e.getMessage());
                }
            }
        }
        
        // 加载奖励队列
        if (tag.contains("rewardQueue")) {
            try {
                RewardQueueManager.getInstance().fromNbt(tag.getCompound("rewardQueue"));
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load reward queue: {}", e.getMessage());
            }
        }
        
        RoadWeaverRPG.LOGGER.info("Loaded QuestSavedData: {} players", loadedCount);
        return data;
    }
    
    public static QuestSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            overworld = level;
        }
        
        return overworld.getDataStorage().computeIfAbsent(
                QuestSavedData::load,
                QuestSavedData::new,
                DATA_NAME
        );
    }
}
