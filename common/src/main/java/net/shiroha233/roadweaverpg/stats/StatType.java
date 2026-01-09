package net.shiroha233.roadweaverpg.stats;

/**
 * 属性类型枚举
 * 定义所有可用的属性类型及其显示信息
 * 移除了元素伤害和元素抗性（参考项目不需要）
 */
public enum StatType {
    // 基础属性
    MAX_HEALTH("max_health", "stats.roadweaver_rpg.max_health", StatCategory.BASIC, 0xFF4CAF50, true),
    MAX_MANA("max_mana", "stats.roadweaver_rpg.max_mana", StatCategory.BASIC, 0xFF2196F3, true),
    ATTACK("attack", "stats.roadweaver_rpg.attack", StatCategory.BASIC, 0xFFFF5722, true),
    DEFENSE("defense", "stats.roadweaver_rpg.defense", StatCategory.BASIC, 0xFF9E9E9E, true),
    MAGIC_ATTACK("magic_attack", "stats.roadweaver_rpg.magic_attack", StatCategory.BASIC, 0xFF9C27B0, true),
    MAGIC_DEFENSE("magic_defense", "stats.roadweaver_rpg.magic_defense", StatCategory.BASIC, 0xFF7B1FA2, true),
    
    // 战斗属性（支持技能点分配）
    CRIT_RATE("crit_rate", "stats.roadweaver_rpg.crit_rate", StatCategory.COMBAT, 0xFFFFEB3B, true),
    CRIT_DAMAGE("crit_damage", "stats.roadweaver_rpg.crit_damage", StatCategory.COMBAT, 0xFFFFC107, true),
    HIT_RATE("hit_rate", "stats.roadweaver_rpg.hit_rate", StatCategory.COMBAT, 0xFF8BC34A, true),
    DODGE_RATE("dodge_rate", "stats.roadweaver_rpg.dodge_rate", StatCategory.COMBAT, 0xFF00BCD4, true),
    ATTACK_SPEED("attack_speed", "stats.roadweaver_rpg.attack_speed", StatCategory.COMBAT, 0xFFFF9800, true),
    MOVE_SPEED("move_speed", "stats.roadweaver_rpg.move_speed", StatCategory.COMBAT, 0xFF03A9F4, true),
    HEALTH_REGEN("health_regen", "stats.roadweaver_rpg.health_regen", StatCategory.COMBAT, 0xFF66BB6A, true),
    MANA_REGEN("mana_regen", "stats.roadweaver_rpg.mana_regen", StatCategory.COMBAT, 0xFF42A5F5, true),
    
    // 特殊属性
    LIFE_STEAL("life_steal", "stats.roadweaver_rpg.life_steal", StatCategory.SPECIAL, 0xFFE91E63, false),
    MANA_STEAL("mana_steal", "stats.roadweaver_rpg.mana_steal", StatCategory.SPECIAL, 0xFF3F51B5, false),
    COOLDOWN_REDUCTION("cooldown_reduction", "stats.roadweaver_rpg.cooldown_reduction", StatCategory.SPECIAL, 0xFF009688, false),
    EXP_BONUS("exp_bonus", "stats.roadweaver_rpg.exp_bonus", StatCategory.SPECIAL, 0xFFCDDC39, false),
    DROP_BONUS("drop_bonus", "stats.roadweaver_rpg.drop_bonus", StatCategory.SPECIAL, 0xFFFF9800, false);
    
    private final String id;
    private final String translationKey;
    private final StatCategory category;
    private final int color;
    private final boolean allocatable; // 是否可通过技能点分配
    
    StatType(String id, String translationKey, StatCategory category, int color, boolean allocatable) {
        this.id = id;
        this.translationKey = translationKey;
        this.category = category;
        this.color = color;
        this.allocatable = allocatable;
    }
    
    public String getId() { return id; }
    public String getTranslationKey() { return translationKey; }
    public StatCategory getCategory() { return category; }
    public int getColor() { return color; }
    public boolean isAllocatable() { return allocatable; }
    
    /**
     * 根据ID获取属性类型
     */
    public static StatType fromId(String id) {
        for (StatType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 属性分类
     */
    public enum StatCategory {
        BASIC("stats.roadweaver_rpg.category.basic"),
        COMBAT("stats.roadweaver_rpg.category.combat"),
        SPECIAL("stats.roadweaver_rpg.category.special");
        
        private final String translationKey;
        
        StatCategory(String translationKey) {
            this.translationKey = translationKey;
        }
        
        public String getTranslationKey() { return translationKey; }
    }
}
