package net.shiroha233.roadweaverpg.playerlevel.effect;

/**
 * 等级效果类型枚举
 */
public enum LevelEffectType {
    MAX_HEALTH("max_health"),      // 增加最大生命值
    ATTACK_DAMAGE("attack_damage"), // 增加攻击伤害
    ARMOR("armor"),                 // 增加护甲值
    MOVEMENT_SPEED("movement_speed"), // 增加移动速度
    POTION("potion"),               // 药水效果
    COMMAND("command");             // 执行命令（用于技能解锁等）
    
    private final String id;
    
    LevelEffectType(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    
    public static LevelEffectType fromString(String name) {
        for (LevelEffectType type : values()) {
            if (type.id.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
