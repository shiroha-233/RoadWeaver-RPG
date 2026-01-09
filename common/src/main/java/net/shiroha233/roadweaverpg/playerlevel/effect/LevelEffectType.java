package net.shiroha233.roadweaverpg.playerlevel.effect;

/**
 * 等级效果类型枚举
 * 支持原版属性和魔法模组属性
 */
public enum LevelEffectType {
    // 原版属性
    MAX_HEALTH("max_health"),           // 最大生命值
    ATTACK_DAMAGE("attack_damage"),     // 攻击伤害
    ARMOR("armor"),                     // 护甲值
    ARMOR_TOUGHNESS("armor_toughness"), // 护甲韧性（魔法防御）
    MOVEMENT_SPEED("movement_speed"),   // 移动速度
    ATTACK_SPEED("attack_speed"),       // 攻击速度
    KNOCKBACK_RESIST("knockback_resist"), // 击退抗性
    LUCK("luck"),                       // 幸运值
    
    // 魔法属性（兼容魔法模组）
    SPELL_POWER("spell_power"),         // 法术威力
    MAX_MANA("max_mana"),               // 最大魔力
    MANA_REGEN("mana_regen"),           // 魔力回复
    COOLDOWN_REDUCTION("cooldown_reduction"), // 冷却缩减
    SPELL_RESIST("spell_resist"),       // 法术抗性
    
    // 特殊效果
    POTION("potion"),                   // 药水效果
    COMMAND("command");                 // 执行命令
    
    private final String id;
    
    LevelEffectType(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    
    /**
     * 判断是否为魔法属性类型
     */
    public boolean isMagicAttribute() {
        return this == SPELL_POWER || this == MAX_MANA || 
               this == MANA_REGEN || this == COOLDOWN_REDUCTION || 
               this == SPELL_RESIST;
    }
    
    /**
     * 判断是否为原版属性类型
     */
    public boolean isVanillaAttribute() {
        return this == MAX_HEALTH || this == ATTACK_DAMAGE || 
               this == ARMOR || this == ARMOR_TOUGHNESS ||
               this == MOVEMENT_SPEED || this == ATTACK_SPEED ||
               this == KNOCKBACK_RESIST || this == LUCK;
    }
    
    public static LevelEffectType fromString(String name) {
        for (LevelEffectType type : values()) {
            if (type.id.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
