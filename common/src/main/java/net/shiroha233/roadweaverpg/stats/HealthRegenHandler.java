package net.shiroha233.roadweaverpg.stats;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;

/**
 * 生命回复处理器
 * 每秒定期恢复玩家生命值，基于技能点分配
 */
public class HealthRegenHandler {
    
    private static final int REGEN_INTERVAL = 20; // 每20tick（1秒）恢复一次
    
    private HealthRegenHandler() {}
    
    /**
     * 处理玩家生命回复（在玩家Tick事件中调用）
     */
    public static void onPlayerTick(ServerPlayer player) {
        if (player.tickCount % REGEN_INTERVAL != 0) return;
        if (player.isDeadOrDying()) return;
        if (player.getHealth() >= player.getMaxHealth()) return;
        
        try {
            QuestDataAccessor dataAccessor = QuestDataAccessor.getInstance();
            if (dataAccessor == null) return;
            
            PlayerQuestData data = dataAccessor.getPlayerData(player);
            StatAllocationData allocData = data.getStatAllocationData();
            
            int points = allocData.getAllocatedPoints(StatType.HEALTH_REGEN);
            if (points <= 0) return;
            
            // 计算回复量：点数 × 每点加成 × 职业倍率
            double baseRegen = StatAllocationConfig.isInitialized() 
                    ? StatAllocationConfig.getInstance().getBonusPerPoint(StatType.HEALTH_REGEN)
                    : 0.1;
            
            double multiplier = net.shiroha233.roadweaverpg.profession.ProfessionDataService
                    .getInstance().getStatBonusMultiplier(player, StatType.HEALTH_REGEN);
            
            float regenAmount = (float)(points * baseRegen * multiplier);
            
            if (regenAmount > 0) {
                player.heal(regenAmount);
            }
        } catch (Exception e) {
            // 静默处理，避免影响游戏
        }
    }
}
