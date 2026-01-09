package net.shiroha233.roadweaverpg.playerlevel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.adventure.AdventureDataService;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;
import net.shiroha233.roadweaverpg.stats.StatAllocationService;

import java.util.function.BiConsumer;

/**
 * 玩家等级数据服务
 * 处理经验增加、等级提升和技能点发放
 * 遵循单一职责原则，专注于玩家等级业务逻辑
 * 
 * 改动说明：
 * - 移除了旧的效果系统（effects），改为技能点系统
 * - 升级时发放技能点，由玩家自由分配属性
 */
public class PlayerLevelDataService {
    
    private static volatile PlayerLevelDataService instance;
    private static final Object LOCK = new Object();
    
    private final QuestDataAccessor dataAccessor;
    
    // 同步回调
    private BiConsumer<ServerPlayer, PlayerQuestData> onSyncPlayerLevel;
    
    private PlayerLevelDataService() {
        this.dataAccessor = QuestDataAccessor.getInstance();
    }
    
    public static PlayerLevelDataService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new PlayerLevelDataService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 增加玩家经验
     */
    public void addPlayerExp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        
        try {
            PlayerQuestData data = dataAccessor.getPlayerData(player);
            int oldLevel = data.getPlayerLevel();
            
            data.addPlayerExp(amount);
            checkPlayerLevelUp(player, data, oldLevel);
            dataAccessor.markDirty(player);
            syncToClient(player);
            
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.player_exp_gained", "+" + amount));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to add player exp for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 检查并处理等级提升
     * 玩家等级不能超过冒险等级上限
     */
    private void checkPlayerLevelUp(ServerPlayer player, PlayerQuestData data, int oldLevel) {
        if (!PlayerLevelManager.isInitialized()) return;
        
        PlayerLevelManager manager = PlayerLevelManager.getInstance();
        int currentXp = data.getPlayerExp();
        int newLevel = manager.getLevelForExperience(currentXp);
        
        // 获取冒险等级作为玩家等级上限
        int adventureLevel = AdventureDataService.getInstance().getAdventureLevel(player);
        int maxPlayerLevel = Math.max(1, adventureLevel); // 至少为1级
        
        // 限制玩家等级不超过冒险等级
        if (newLevel > maxPlayerLevel) {
            newLevel = maxPlayerLevel;
            // 提示玩家需要提升冒险等级
            if (newLevel == oldLevel) {
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.player_level_capped", maxPlayerLevel));
            }
        }
        
        if (newLevel > oldLevel) {
            // 逐级发放奖励和技能点
            for (int i = oldLevel + 1; i <= newLevel; i++) {
                grantLevelRewards(player, data, i);
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.player_level_up", i));
            }
            data.setPlayerLevel(newLevel);
            dataAccessor.markDirty(player);
        }
    }
    
    /**
     * 发放等级奖励（技能点和一次性奖励）
     */
    private void grantLevelRewards(ServerPlayer player, PlayerQuestData data, int level) {
        if (!PlayerLevelManager.isInitialized()) return;
        
        PlayerLevelManager manager = PlayerLevelManager.getInstance();
        PlayerLevel levelInfo = manager.getLevelInfo(level);
        
        if (levelInfo == null) return;
        
        // 发放技能点
        int skillPoints = levelInfo.getSkillPoints();
        if (skillPoints > 0) {
            data.getStatAllocationData().addAvailablePoints(skillPoints);
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.skill_points_gained", skillPoints));
        }
        
        // 发放一次性奖励
        for (QuestReward reward : levelInfo.getRewards()) {
            try {
                if (reward.canGrant(player)) {
                    reward.grant(player);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to grant player level {} reward: {}", 
                        level, e.getMessage());
            }
        }
    }
    
    /**
     * 刷新玩家效果（登录时调用）
     * 重新应用技能点分配的属性加成
     */
    public void refreshEffects(ServerPlayer player) {
        try {
            StatAllocationService.getInstance().refreshAllStats(player);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to refresh effects for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 获取玩家当前等级
     */
    public int getPlayerLevel(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getPlayerLevel();
    }
    
    /**
     * 获取玩家当前经验
     */
    public int getPlayerExp(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getPlayerExp();
    }
    
    /**
     * 同步玩家等级数据到客户端
     */
    public void syncToClient(ServerPlayer player) {
        if (onSyncPlayerLevel != null) {
            onSyncPlayerLevel.accept(player, dataAccessor.getPlayerData(player));
        }
    }
    
    public void setOnSyncPlayerLevel(BiConsumer<ServerPlayer, PlayerQuestData> callback) {
        this.onSyncPlayerLevel = callback;
    }
}
