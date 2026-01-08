package net.shiroha233.roadweaverpg.adventure;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;

import java.util.function.BiConsumer;

/**
 * 冒险等级数据服务
 * 处理经验增加、等级提升和奖励发放
 * 遵循单一职责原则，专注于冒险等级业务逻辑
 */
public class AdventureDataService {
    private static volatile AdventureDataService instance;
    private static final Object LOCK = new Object();
    
    private final QuestDataAccessor dataAccessor;
    
    // 同步回调
    private BiConsumer<ServerPlayer, PlayerQuestData> onSyncAdventure;

    private AdventureDataService() {
        this.dataAccessor = QuestDataAccessor.getInstance();
    }

    public static AdventureDataService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new AdventureDataService();
                }
            }
        }
        return instance;
    }

    /**
     * 增加冒险经验
     */
    public void addAdventureExp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        
        try {
            PlayerQuestData data = dataAccessor.getPlayerData(player);
            data.addAdventureExp(amount);
            checkAdventureLevelUp(player, data);
            dataAccessor.markDirty(player);
            syncToClient(player);
            
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.adventure_exp_gained", "+" + amount));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to add adventure exp for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * 检查并处理等级提升
     */
    private void checkAdventureLevelUp(ServerPlayer player, PlayerQuestData data) {
        AdventureLevelManager manager = AdventureLevelManager.getInstance();
        if (manager == null) return;
        
        int currentXp = data.getAdventureExp();
        int currentLevel = data.getAdventureLevel();
        int newLevel = manager.getLevelForExperience(currentXp);
        
        if (newLevel > currentLevel) {
            // 逐级发放奖励
            for (int i = currentLevel + 1; i <= newLevel; i++) {
                grantLevelRewards(player, i);
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.adventure_level_up", i));
            }
            data.setAdventureLevel(newLevel);
            dataAccessor.markDirty(player);
        }
    }

    /**
     * 发放等级奖励
     */
    private void grantLevelRewards(ServerPlayer player, int level) {
        AdventureLevelManager manager = AdventureLevelManager.getInstance();
        AdventureLevel levelInfo = manager.getLevelInfo(level);
        
        if (levelInfo == null) return;
        
        for (QuestReward reward : levelInfo.getRewards()) {
            try {
                if (reward.canGrant(player)) {
                    reward.grant(player);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to grant adventure level {} reward: {}", 
                        level, e.getMessage());
            }
        }
    }

    /**
     * 获取玩家当前冒险等级
     */
    public int getAdventureLevel(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getAdventureLevel();
    }

    /**
     * 获取玩家当前冒险经验
     */
    public int getAdventureExp(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getAdventureExp();
    }

    /**
     * 同步冒险数据到客户端
     */
    public void syncToClient(ServerPlayer player) {
        if (onSyncAdventure != null) {
            onSyncAdventure.accept(player, dataAccessor.getPlayerData(player));
        }
    }

    public void setOnSyncAdventure(BiConsumer<ServerPlayer, PlayerQuestData> callback) {
        this.onSyncAdventure = callback;
    }
}
