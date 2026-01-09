package net.shiroha233.roadweaverpg.stats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;

import java.util.function.BiConsumer;

/**
 * 属性分配服务
 * 处理技能点分配、属性应用和数据同步
 * 遵循单一职责原则，委托StatEffectService应用属性
 */
public class StatAllocationService {
    
    private static volatile StatAllocationService instance;
    private static final Object LOCK = new Object();
    
    private final QuestDataAccessor dataAccessor;
    
    // 同步回调
    private BiConsumer<ServerPlayer, StatAllocationData> onSyncCallback;
    
    private StatAllocationService() {
        this.dataAccessor = QuestDataAccessor.getInstance();
    }
    
    public static StatAllocationService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new StatAllocationService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置同步回调
     */
    public void setOnSyncCallback(BiConsumer<ServerPlayer, StatAllocationData> callback) {
        this.onSyncCallback = callback;
    }
    
    /**
     * 分配技能点到指定属性
     */
    public boolean allocatePoint(ServerPlayer player, StatType type) {
        if (!type.isAllocatable()) {
            player.sendSystemMessage(Component.translatable("message.roadweaver_rpg.stat_not_allocatable"));
            return false;
        }
        
        try {
            PlayerQuestData questData = dataAccessor.getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            
            if (allocData.getAvailablePoints() <= 0) {
                player.sendSystemMessage(Component.translatable("message.roadweaver_rpg.no_skill_points"));
                return false;
            }
            
            if (allocData.allocatePoint(type)) {
                applyStatBonus(player, type, allocData.getAllocatedPoints(type));
                dataAccessor.markDirty(player);
                syncToClient(player);
                
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.stat_allocated", 
                        Component.translatable(type.getTranslationKey())));
                return true;
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to allocate stat point for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
        return false;
    }
    
    /**
     * 从指定属性减少技能点
     */
    public boolean deallocatePoint(ServerPlayer player, StatType type) {
        if (!type.isAllocatable()) {
            return false;
        }
        
        try {
            PlayerQuestData questData = dataAccessor.getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            
            if (allocData.getAllocatedPoints(type) <= 0) {
                return false;
            }
            
            if (allocData.deallocatePoint(type)) {
                applyStatBonus(player, type, allocData.getAllocatedPoints(type));
                dataAccessor.markDirty(player);
                syncToClient(player);
                
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.stat_deallocated", 
                        Component.translatable(type.getTranslationKey())));
                return true;
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to deallocate stat point for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
        return false;
    }
    
    /**
     * 重置所有属性分配
     */
    public void resetAllocation(ServerPlayer player) {
        try {
            PlayerQuestData questData = dataAccessor.getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            
            // 移除所有属性效果
            StatEffectService.removeAllModifiers(player);
            
            // 重置分配数据
            allocData.resetAllocation();
            
            dataAccessor.markDirty(player);
            syncToClient(player);
            
            player.sendSystemMessage(Component.translatable("message.roadweaver_rpg.stats_reset"));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to reset stat allocation for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 添加可用技能点
     */
    public void addSkillPoints(ServerPlayer player, int points) {
        if (points <= 0) return;
        
        try {
            PlayerQuestData questData = dataAccessor.getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            
            allocData.addAvailablePoints(points);
            dataAccessor.markDirty(player);
            syncToClient(player);
            
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.skill_points_gained", points));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to add skill points for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 刷新玩家所有属性效果（登录或重生时调用）
     */
    public void refreshAllStats(ServerPlayer player) {
        try {
            PlayerQuestData questData = dataAccessor.getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            
            for (StatType type : StatType.values()) {
                if (type.isAllocatable()) {
                    int points = allocData.getAllocatedPoints(type);
                    if (points > 0) {
                        applyStatBonus(player, type, points);
                    }
                }
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to refresh stats for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 应用属性加成（委托给StatEffectService）
     */
    private void applyStatBonus(ServerPlayer player, StatType type, int totalPoints) {
        if (!StatAllocationConfig.isInitialized()) return;
        
        double bonus = StatAllocationConfig.getInstance().calculateBonus(type, totalPoints);
        
        switch (type) {
            case MAX_HEALTH -> StatEffectService.applyMaxHealth(player, bonus);
            case ATTACK -> StatEffectService.applyAttackDamage(player, bonus);
            case DEFENSE -> StatEffectService.applyArmor(player, bonus);
            case MAGIC_DEFENSE -> StatEffectService.applyArmorToughness(player, bonus);
            case MAX_MANA -> StatEffectService.applyMaxMana(player, bonus);
            case MAGIC_ATTACK -> StatEffectService.applySpellPower(player, bonus);
            case MOVE_SPEED -> StatEffectService.applyMovementSpeed(player, bonus);
            case ATTACK_SPEED -> StatEffectService.applyAttackSpeed(player, bonus);
            // 战斗属性暂存到PlayerStats缓存（客户端显示用）
            case CRIT_RATE, CRIT_DAMAGE, HIT_RATE, DODGE_RATE, HEALTH_REGEN, MANA_REGEN -> {
                // 这些属性通过RPG系统计算，不直接修改原版属性
            }
            default -> {}
        }
    }
    
    /**
     * 同步数据到客户端
     */
    public void syncToClient(ServerPlayer player) {
        if (onSyncCallback != null) {
            try {
                PlayerQuestData questData = dataAccessor.getPlayerData(player);
                onSyncCallback.accept(player, questData.getStatAllocationData());
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to sync stat allocation to client: {}", e.getMessage());
            }
        }
    }
}
