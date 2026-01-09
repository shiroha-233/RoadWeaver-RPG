package net.shiroha233.roadweaverpg.stats;

/**
 * 玩家属性数据类
 * 移除了元素伤害和元素抗性（参考项目不需要）
 * 
 * 属性分类：
 * - 基础属性：生命值、攻击力、防御力等（可加点）
 * - 战斗属性：暴击率、暴击伤害、命中、闪避等
 * - 特殊属性：生命偷取、冷却缩减等
 */
public class PlayerStats {
    
    // ==================== 基础属性（可加点） ====================
    private double maxHealth = 20.0;           // 最大生命值
    private double currentHealth = 20.0;       // 当前生命值
    private double maxMana = 100.0;            // 最大魔力值
    private double currentMana = 100.0;        // 当前魔力值
    private double attack = 1.0;               // 攻击力
    private double defense = 0.0;              // 防御力
    private double magicAttack = 1.0;          // 魔法攻击力
    private double magicDefense = 0.0;         // 魔法防御力
    
    // ==================== 战斗属性 ====================
    private double critRate = 5.0;             // 暴击率 (%)
    private double critDamage = 150.0;         // 暴击伤害 (%)
    private double hitRate = 100.0;            // 命中率 (%)
    private double dodgeRate = 0.0;            // 闪避率 (%)
    private double attackSpeed = 100.0;        // 攻击速度 (%)
    private double moveSpeed = 100.0;          // 移动速度 (%)
    private double healthRegen = 0.0;          // 生命回复/秒
    private double manaRegen = 1.0;            // 魔力回复/秒
    
    // ==================== 特殊属性 ====================
    private double lifeSteal = 0.0;            // 生命偷取 (%)
    private double manaSteal = 0.0;            // 魔力偷取 (%)
    private double cooldownReduction = 0.0;    // 冷却缩减 (%)
    private double expBonus = 0.0;             // 经验加成 (%)
    private double dropBonus = 0.0;            // 掉落加成 (%)
    
    public PlayerStats() {}
    
    // ==================== Getters ====================
    public double getMaxHealth() { return maxHealth; }
    public double getCurrentHealth() { return currentHealth; }
    public double getMaxMana() { return maxMana; }
    public double getCurrentMana() { return currentMana; }
    public double getAttack() { return attack; }
    public double getDefense() { return defense; }
    public double getMagicAttack() { return magicAttack; }
    public double getMagicDefense() { return magicDefense; }
    
    public double getCritRate() { return critRate; }
    public double getCritDamage() { return critDamage; }
    public double getHitRate() { return hitRate; }
    public double getDodgeRate() { return dodgeRate; }
    public double getAttackSpeed() { return attackSpeed; }
    public double getMoveSpeed() { return moveSpeed; }
    public double getHealthRegen() { return healthRegen; }
    public double getManaRegen() { return manaRegen; }
    
    public double getLifeSteal() { return lifeSteal; }
    public double getManaSteal() { return manaSteal; }
    public double getCooldownReduction() { return cooldownReduction; }
    public double getExpBonus() { return expBonus; }
    public double getDropBonus() { return dropBonus; }
    
    // ==================== Setters ====================
    public void setMaxHealth(double v) { this.maxHealth = v; }
    public void setCurrentHealth(double v) { this.currentHealth = Math.min(v, maxHealth); }
    public void setMaxMana(double v) { this.maxMana = v; }
    public void setCurrentMana(double v) { this.currentMana = Math.min(v, maxMana); }
    public void setAttack(double v) { this.attack = v; }
    public void setDefense(double v) { this.defense = v; }
    public void setMagicAttack(double v) { this.magicAttack = v; }
    public void setMagicDefense(double v) { this.magicDefense = v; }
    
    public void setCritRate(double v) { this.critRate = Math.min(100, Math.max(0, v)); }
    public void setCritDamage(double v) { this.critDamage = Math.max(0, v); }
    public void setHitRate(double v) { this.hitRate = Math.max(0, v); }
    public void setDodgeRate(double v) { this.dodgeRate = Math.min(100, Math.max(0, v)); }
    public void setAttackSpeed(double v) { this.attackSpeed = Math.max(0, v); }
    public void setMoveSpeed(double v) { this.moveSpeed = Math.max(0, v); }
    public void setHealthRegen(double v) { this.healthRegen = v; }
    public void setManaRegen(double v) { this.manaRegen = v; }
    
    public void setLifeSteal(double v) { this.lifeSteal = v; }
    public void setManaSteal(double v) { this.manaSteal = v; }
    public void setCooldownReduction(double v) { this.cooldownReduction = Math.min(80, Math.max(0, v)); }
    public void setExpBonus(double v) { this.expBonus = v; }
    public void setDropBonus(double v) { this.dropBonus = v; }
}
