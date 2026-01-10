package net.shiroha233.roadweaverpg.combat;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.stats.StatAllocationConfig;
import net.shiroha233.roadweaverpg.stats.StatAllocationData;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 暴击系统服务
 * 计算暴击率和暴击伤害
 */
public final class CriticalHitService {
    
    private CriticalHitService() {}
    
    // 基础暴击率 5%
    private static final double BASE_CRIT_RATE = 5.0;
    // 基础暴击伤害 150%
    private static final double BASE_CRIT_DAMAGE = 150.0;
    
    /**
     * 计算玩家的暴击率
     */
    public static double getCritRate(ServerPlayer player) {
        try {
            var questData = QuestDataAccessor.getInstance().getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            int points = allocData.getAllocatedPoints(StatType.CRIT_RATE);
            double bonusPerPoint = StatAllocationConfig.isInitialized() 
                    ? StatAllocationConfig.getInstance().getBonusPerPoint(StatType.CRIT_RATE) 
                    : 0.5;
            return Math.min(100.0, BASE_CRIT_RATE + points * bonusPerPoint);
        } catch (Exception e) {
            return BASE_CRIT_RATE;
        }
    }
    
    /**
     * 计算玩家的暴击伤害倍率
     */
    public static double getCritDamage(ServerPlayer player) {
        try {
            var questData = QuestDataAccessor.getInstance().getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            int points = allocData.getAllocatedPoints(StatType.CRIT_DAMAGE);
            double bonusPerPoint = StatAllocationConfig.isInitialized() 
                    ? StatAllocationConfig.getInstance().getBonusPerPoint(StatType.CRIT_DAMAGE) 
                    : 2.0;
            return BASE_CRIT_DAMAGE + points * bonusPerPoint;
        } catch (Exception e) {
            return BASE_CRIT_DAMAGE;
        }
    }
    
    /**
     * 判断是否触发暴击
     */
    public static boolean rollCritical(ServerPlayer player) {
        double critRate = getCritRate(player);
        return ThreadLocalRandom.current().nextDouble(100.0) < critRate;
    }
    
    /**
     * 计算暴击后的伤害
     * @param baseDamage 基础伤害
     * @param player 攻击者
     * @return 暴击结果（包含是否暴击和最终伤害）
     */
    public static CritResult calculateCriticalDamage(float baseDamage, ServerPlayer player) {
        boolean isCrit = rollCritical(player);
        if (isCrit) {
            double critMultiplier = getCritDamage(player) / 100.0;
            return new CritResult(true, (float)(baseDamage * critMultiplier));
        }
        return new CritResult(false, baseDamage);
    }
    
    /**
     * 暴击计算结果
     */
    public record CritResult(boolean isCritical, float damage) {}
}
