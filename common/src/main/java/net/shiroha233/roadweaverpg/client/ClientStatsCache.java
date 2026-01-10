package net.shiroha233.roadweaverpg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.stats.StatType;

/**
 * 客户端属性缓存 - 单一数据源
 * 
 * 设计原则：
 * - 所有属性值都从玩家实体或服务端同步获取
 * - 界面只读取这里的值，不做任何计算
 * - 职业/技能点等系统负责修改玩家实体属性，这里只负责读取
 */
public final class ClientStatsCache {
    
    // 原版属性（从玩家实体读取）
    private static double maxHealth = 20.0;
    private static double attack = 1.0;
    private static double defense = 0.0;
    private static double magicDefense = 0.0;
    private static double moveSpeed = 100.0;
    private static double attackSpeed = 4.0;
    
    // RPG属性（从服务端同步）
    private static double maxMana = 100.0;
    private static double magicAttack = 1.0;
    private static double critRate = 5.0;
    private static double critDamage = 150.0;
    private static double hitRate = 100.0;
    private static double dodgeRate = 0.0;
    private static double healthRegen = 0.0;
    private static double manaRegen = 1.0;
    private static double lifeSteal = 0.0;
    private static double manaSteal = 0.0;
    private static double cooldownReduction = 0.0;
    private static double expBonus = 0.0;
    private static double dropBonus = 0.0;
    
    private ClientStatsCache() {}
    
    /**
     * 从玩家实体更新原版属性
     * 每帧调用，读取玩家实体的最新属性值
     */
    public static void updateFromPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        Player player = mc.player;
        maxHealth = player.getMaxHealth();
        attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        defense = player.getArmorValue();
        magicDefense = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        moveSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1000.0;
        attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
    }
    
    /**
     * 从服务端同步RPG属性
     * 由网络消息调用
     */
    public static void updateFromServer(double maxManaVal, double magicAttackVal, 
            double critRateVal, double critDamageVal, double hitRateVal, double dodgeRateVal,
            double healthRegenVal, double manaRegenVal, double lifeStealVal, double manaStealVal,
            double cooldownReductionVal, double expBonusVal, double dropBonusVal) {
        maxMana = maxManaVal;
        magicAttack = magicAttackVal;
        critRate = critRateVal;
        critDamage = critDamageVal;
        hitRate = hitRateVal;
        dodgeRate = dodgeRateVal;
        healthRegen = healthRegenVal;
        manaRegen = manaRegenVal;
        lifeSteal = lifeStealVal;
        manaSteal = manaStealVal;
        cooldownReduction = cooldownReductionVal;
        expBonus = expBonusVal;
        dropBonus = dropBonusVal;
    }
    
    /**
     * 获取属性值 - 界面唯一的数据来源
     */
    public static double getValue(StatType type) {
        return switch (type) {
            case MAX_HEALTH -> maxHealth;
            case ATTACK -> attack;
            case DEFENSE -> defense;
            case MAGIC_DEFENSE -> magicDefense;
            case MOVE_SPEED -> moveSpeed;
            case ATTACK_COOLDOWN -> calculateAttackCooldown();
            case MAX_MANA -> maxMana;
            case MAGIC_ATTACK -> magicAttack;
            case CRIT_RATE -> critRate;
            case CRIT_DAMAGE -> critDamage;
            case HIT_RATE -> hitRate;
            case DODGE_RATE -> dodgeRate;
            case HEALTH_REGEN -> healthRegen;
            case MANA_REGEN -> manaRegen;
            case LIFE_STEAL -> lifeSteal;
            case MANA_STEAL -> manaSteal;
            case COOLDOWN_REDUCTION -> cooldownReduction;
            case EXP_BONUS -> expBonus;
            case DROP_BONUS -> dropBonus;
        };
    }
    
    /**
     * 计算攻击冷却百分比（从攻击速度转换）
     */
    private static double calculateAttackCooldown() {
        // 基础攻击速度是4.0，转换为冷却缩减百分比
        double baseSpeed = 4.0;
        if (attackSpeed <= baseSpeed) return 0.0;
        return ((attackSpeed - baseSpeed) / attackSpeed) * 100.0;
    }
    
    /**
     * 清空缓存（断开连接时调用）
     */
    public static void clear() {
        maxHealth = 20.0;
        attack = 1.0;
        defense = 0.0;
        magicDefense = 0.0;
        moveSpeed = 100.0;
        attackSpeed = 4.0;
        maxMana = 100.0;
        magicAttack = 1.0;
        critRate = 5.0;
        critDamage = 150.0;
        hitRate = 100.0;
        dodgeRate = 0.0;
        healthRegen = 0.0;
        manaRegen = 1.0;
        lifeSteal = 0.0;
        manaSteal = 0.0;
        cooldownReduction = 0.0;
        expBonus = 0.0;
        dropBonus = 0.0;
    }
    
    // region 便捷getter（向后兼容）
    public static double getMaxHealth() { return maxHealth; }
    public static double getAttack() { return attack; }
    public static double getDefense() { return defense; }
    public static double getMagicDefense() { return magicDefense; }
    public static double getMoveSpeed() { return moveSpeed; }
    public static double getMaxMana() { return maxMana; }
    public static double getMagicAttack() { return magicAttack; }
    public static double getCritRate() { return critRate; }
    public static double getCritDamage() { return critDamage; }
    // endregion
}
