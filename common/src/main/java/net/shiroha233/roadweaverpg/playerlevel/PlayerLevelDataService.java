package net.shiroha233.roadweaverpg.playerlevel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;

import java.util.function.BiConsumer;

/**
 * 玩家等级数据服务
 * 处理经验增加、等级提升、效果应用和奖励发放
 * 遵循单一职责原则，专注于玩家等级业务逻辑
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
     */
    private void checkPlayerLevelUp(ServerPlayer player, PlayerQuestData data, int oldLevel) {
        if (!PlayerLevelManager.isInitialized()) return;
        
        PlayerLevelManager manager = PlayerLevelManager.getInstance();
        int currentXp = data.getPlayerExp();
        int newLevel = manager.getLevelForExperience(currentXp);
        
        if (newLevel > oldLevel) {
            // 逐级发放奖励和应用效果
            for (int i = oldLevel + 1; i <= newLevel; i++) {
                grantLevelRewards(player, i);
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.player_level_up", i));
            }
            data.setPlayerLevel(newLevel);
            
            // 应用累积效果
            applyAllEffects(player, newLevel);
            dataAccessor.markDirty(player);
        }
    }
    
    /**
     * 发放等级奖励（一次性奖励）
     */
    private void grantLevelRewards(ServerPlayer player, int level) {
        if (!PlayerLevelManager.isInitialized()) return;
        
        PlayerLevelManager manager = PlayerLevelManager.getInstance();
        PlayerLevel levelInfo = manager.getLevelInfo(level);
        
        if (levelInfo == null) return;
        
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
        
        // 执行命令效果（仅在升级时执行一次）
        for (LevelEffect effect : levelInfo.getEffects()) {
            if ("command".equals(effect.getTypeId())) {
                try {
                    effect.apply(player, level);
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.error("Failed to apply command effect at level {}: {}", 
                            level, e.getMessage());
                }
            }
        }
    }
    
    /**
     * 应用所有累积效果（属性加成等）
     * 原理：遍历所有已达到的等级，累加效果
     */
    public void applyAllEffects(ServerPlayer player, int currentLevel) {
        if (!PlayerLevelManager.isInitialized()) return;
        
        PlayerLevelManager manager = PlayerLevelManager.getInstance();
        
        // 先移除所有旧效果
        removeAllEffects(player);
        
        // 计算累积效果值
        double totalMaxHealth = 0;
        double totalAttackDamage = 0;
        double totalArmor = 0;
        
        for (int i = 1; i <= currentLevel; i++) {
            PlayerLevel levelInfo = manager.getLevelInfo(i);
            if (levelInfo == null) continue;
            
            for (LevelEffect effect : levelInfo.getEffects()) {
                // 跳过命令效果（已在升级时执行）
                if ("command".equals(effect.getTypeId())) continue;
                
                // 累加属性效果
                switch (effect.getTypeId()) {
                    case "max_health" -> totalMaxHealth += extractAmount(effect);
                    case "attack_damage" -> totalAttackDamage += extractAmount(effect);
                    case "armor" -> totalArmor += extractAmount(effect);
                    default -> {
                        // 其他效果直接应用（如药水效果）
                        try {
                            effect.apply(player, currentLevel);
                        } catch (Exception e) {
                            RoadWeaverRPG.LOGGER.error("Failed to apply effect: {}", e.getMessage());
                        }
                    }
                }
            }
        }
        
        // 应用累积的属性效果
        if (totalMaxHealth > 0) {
            new net.shiroha233.roadweaverpg.playerlevel.effect.impl.MaxHealthEffect(
                    totalMaxHealth, 
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
            ).apply(player, currentLevel);
        }
        if (totalAttackDamage > 0) {
            new net.shiroha233.roadweaverpg.playerlevel.effect.impl.AttackDamageEffect(
                    totalAttackDamage,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
            ).apply(player, currentLevel);
        }
        if (totalArmor > 0) {
            new net.shiroha233.roadweaverpg.playerlevel.effect.impl.ArmorEffect(
                    totalArmor,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
            ).apply(player, currentLevel);
        }
        
        RoadWeaverRPG.LOGGER.debug("Applied level effects to {}: HP+{}, ATK+{}, DEF+{}", 
                player.getName().getString(), totalMaxHealth, totalAttackDamage, totalArmor);
    }
    
    /**
     * 从效果中提取数值（用于累加计算）
     */
    private double extractAmount(LevelEffect effect) {
        try {
            var json = effect.toJson();
            return json.has("amount") ? json.get("amount").getAsDouble() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 移除所有等级效果
     */
    public void removeAllEffects(ServerPlayer player) {
        // 移除属性修改器
        new net.shiroha233.roadweaverpg.playerlevel.effect.impl.MaxHealthEffect(0, 
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION).remove(player);
        new net.shiroha233.roadweaverpg.playerlevel.effect.impl.AttackDamageEffect(0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION).remove(player);
        new net.shiroha233.roadweaverpg.playerlevel.effect.impl.ArmorEffect(0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION).remove(player);
    }
    
    /**
     * 刷新玩家效果（登录时调用）
     */
    public void refreshEffects(ServerPlayer player) {
        try {
            PlayerQuestData data = dataAccessor.getPlayerData(player);
            int level = data.getPlayerLevel();
            if (level > 0) {
                applyAllEffects(player, level);
            }
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
