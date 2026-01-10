package net.shiroha233.roadweaverpg.worlddifficulty;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 怪物属性数据类
 * 
 * 设计原理：
 * - 不可变记录类，线程安全
 * - 包含基础数值和环境适应加成
 * - 支持网络序列化
 */
public record MonsterStats(
        // 基础数值类
        float bonusArmor,              // 护甲值加成
        float bonusHealth,             // 生命值加成
        float bonusResistance,         // 抗性提升
        float bonusHealthRegen,        // 生命恢复
        float physicalResistance,      // 物理攻击抗性 (0-1)
        float magicResistance,         // 魔法攻击抗性 (0-1)
        float bonusDamage,             // 伤害加成
        
        // 环境适应加成（运行时计算）
        float envArmorBonus,
        float envHealthBonus,
        float envDamageBonus,
        float envResistanceBonus
) {
    
    // 获取最终属性（基础 + 环境）
    public float getTotalArmor() { return bonusArmor + envArmorBonus; }
    public float getTotalHealth() { return bonusHealth + envHealthBonus; }
    public float getTotalDamage() { return bonusDamage + envDamageBonus; }
    public float getTotalResistance() { return bonusResistance + envResistanceBonus; }
    
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(bonusArmor);
        buf.writeFloat(bonusHealth);
        buf.writeFloat(bonusResistance);
        buf.writeFloat(bonusHealthRegen);
        buf.writeFloat(physicalResistance);
        buf.writeFloat(magicResistance);
        buf.writeFloat(bonusDamage);
        buf.writeFloat(envArmorBonus);
        buf.writeFloat(envHealthBonus);
        buf.writeFloat(envDamageBonus);
        buf.writeFloat(envResistanceBonus);
    }
    
    public static MonsterStats fromNetwork(FriendlyByteBuf buf) {
        return new MonsterStats(
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()
        );
    }
    
    public static MonsterStats empty() {
        return new MonsterStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float bonusArmor = 0;
        private float bonusHealth = 0;
        private float bonusResistance = 0;
        private float bonusHealthRegen = 0;
        private float physicalResistance = 0;
        private float magicResistance = 0;
        private float bonusDamage = 0;
        private float envArmorBonus = 0;
        private float envHealthBonus = 0;
        private float envDamageBonus = 0;
        private float envResistanceBonus = 0;
        
        public Builder armor(float value) { this.bonusArmor = value; return this; }
        public Builder health(float value) { this.bonusHealth = value; return this; }
        public Builder resistance(float value) { this.bonusResistance = value; return this; }
        public Builder healthRegen(float value) { this.bonusHealthRegen = value; return this; }
        public Builder physicalResist(float value) { this.physicalResistance = value; return this; }
        public Builder magicResist(float value) { this.magicResistance = value; return this; }
        public Builder damage(float value) { this.bonusDamage = value; return this; }
        public Builder envArmor(float value) { this.envArmorBonus = value; return this; }
        public Builder envHealth(float value) { this.envHealthBonus = value; return this; }
        public Builder envDamage(float value) { this.envDamageBonus = value; return this; }
        public Builder envResistance(float value) { this.envResistanceBonus = value; return this; }
        
        public MonsterStats build() {
            return new MonsterStats(
                    bonusArmor, bonusHealth, bonusResistance, bonusHealthRegen,
                    physicalResistance, magicResistance, bonusDamage,
                    envArmorBonus, envHealthBonus, envDamageBonus, envResistanceBonus
            );
        }
    }
}
