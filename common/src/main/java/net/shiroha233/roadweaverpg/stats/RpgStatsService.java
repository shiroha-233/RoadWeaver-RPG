package net.shiroha233.roadweaverpg.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.profession.ProfessionDefinition;
import net.shiroha233.roadweaverpg.profession.ProfessionManager;

import java.util.function.Consumer;

/**
 * RPG属性服务 - 统一管理RPG属性的计算和同步
 * 
 * 设计原则：
 * - 单一职责：只负责RPG属性的计算和同步
 * - 服务端计算所有属性值，客户端只负责显示
 * 
 * 注意：ATTACK_DAMAGE 在 Minecraft 中默认不同步到客户端，
 * 所以我们需要手动同步攻击力
 */
public class RpgStatsService {
    
    private static RpgStatsService instance;
    
    // 同步回调（由平台特定代码设置）
    private static Consumer<RpgStatsSyncData> syncCallback;
    
    private RpgStatsService() {}
    
    public static RpgStatsService getInstance() {
        if (instance == null) {
            instance = new RpgStatsService();
        }
        return instance;
    }
    
    public static void setSyncCallback(Consumer<RpgStatsSyncData> callback) {
        syncCallback = callback;
    }
    
    /**
     * 计算并同步玩家的RPG属性
     */
    public void syncRpgStats(ServerPlayer player) {
        if (syncCallback == null) return;
        
        QuestDataAccessor dataAccessor = QuestDataAccessor.getInstance();
        if (dataAccessor == null) return;
        
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        ProfessionDefinition profession = getPlayerProfession(data);
        
        // 从服务端实体读取原版属性（因为ATTACK_DAMAGE不会自动同步）
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        
        // 计算所有RPG属性
        double maxMana = calculateStat(StatType.MAX_MANA, profession, data);
        double magicAttack = calculateStat(StatType.MAGIC_ATTACK, profession, data);
        double critRate = calculateStat(StatType.CRIT_RATE, profession, data);
        double critDamage = calculateStat(StatType.CRIT_DAMAGE, profession, data);
        double hitRate = calculateStat(StatType.HIT_RATE, profession, data);
        double dodgeRate = calculateStat(StatType.DODGE_RATE, profession, data);
        double healthRegen = calculateStat(StatType.HEALTH_REGEN, profession, data);
        double manaRegen = calculateStat(StatType.MANA_REGEN, profession, data);
        double lifeSteal = calculateStat(StatType.LIFE_STEAL, profession, data);
        double manaSteal = calculateStat(StatType.MANA_STEAL, profession, data);
        double cooldownReduction = calculateStat(StatType.COOLDOWN_REDUCTION, profession, data);
        double expBonus = calculateStat(StatType.EXP_BONUS, profession, data);
        double dropBonus = calculateStat(StatType.DROP_BONUS, profession, data);
        
        // 发送同步（包含攻击力）
        syncCallback.accept(new RpgStatsSyncData(player, attack, maxMana, magicAttack, critRate, critDamage,
                hitRate, dodgeRate, healthRegen, manaRegen, lifeSteal, manaSteal,
                cooldownReduction, expBonus, dropBonus));
    }
    
    /**
     * 计算单个属性值
     * 公式：职业基础值 + 成长值 * (等级-1) + 技能点加成
     */
    private double calculateStat(StatType type, ProfessionDefinition profession, PlayerQuestData data) {
        // 职业基础值
        double base = profession != null ? profession.getBaseStat(type) : getDefaultBase(type);
        
        // 成长值
        double growth = 0;
        if (profession != null) {
            int level = data.getPlayerLevel();
            growth = profession.getGrowth(type) * Math.max(0, level - 1);
        }
        
        // 技能点加成
        double skillBonus = 0;
        if (type.isAllocatable()) {
            int points = data.getStatAllocationData().getAllocatedPoints(type);
            double bonusPerPoint = StatAllocationConfig.isInitialized() 
                    ? StatAllocationConfig.getInstance().getBonusPerPoint(type) 
                    : getDefaultBonusPerPoint(type);
            
            // 职业加成倍率
            double multiplier = profession != null ? profession.getBonusMultiplier(type) : 1.0;
            skillBonus = points * bonusPerPoint * multiplier;
        }
        
        return base + growth + skillBonus;
    }
    
    private ProfessionDefinition getPlayerProfession(PlayerQuestData data) {
        if (data.getProfessionId() == null) return null;
        if (!ProfessionManager.isInitialized()) return null;
        return ProfessionManager.getInstance().getProfession(data.getProfessionId());
    }
    
    private double getDefaultBase(StatType type) {
        return switch (type) {
            case MAX_MANA -> 100.0;
            case MAGIC_ATTACK, MANA_REGEN -> 1.0;
            case CRIT_RATE -> 5.0;
            case CRIT_DAMAGE -> 150.0;
            case HIT_RATE -> 100.0;
            default -> 0.0;
        };
    }
    
    private double getDefaultBonusPerPoint(StatType type) {
        return switch (type) {
            case MAX_MANA -> 10.0;
            case MAGIC_ATTACK -> 1.0;
            case CRIT_RATE -> 0.5;
            case CRIT_DAMAGE -> 2.0;
            case HEALTH_REGEN -> 0.1;
            case MANA_REGEN -> 0.2;
            default -> 0.0;
        };
    }
    
    /**
     * RPG属性同步数据（包含攻击力，因为ATTACK_DAMAGE不会自动同步）
     */
    public record RpgStatsSyncData(
            ServerPlayer player,
            double attack, double maxMana, double magicAttack, double critRate, double critDamage,
            double hitRate, double dodgeRate, double healthRegen, double manaRegen,
            double lifeSteal, double manaSteal, double cooldownReduction,
            double expBonus, double dropBonus
    ) {}
}
