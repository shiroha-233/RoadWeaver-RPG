package net.shiroha233.roadweaverpg.worlddifficulty;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 怪物稀有度/品质枚举
 * 
 * 设计原理：
 * - 影响属性倍率与掉落
 * - 每种稀有度有独特的颜色标识
 * - 支持Jade等模组显示
 */
public enum MonsterRarity {
    NORMAL("normal", 1.0f, 1.0f, ChatFormatting.WHITE),
    ELITE("elite", 1.5f, 1.5f, ChatFormatting.BLUE),
    BOSS("boss", 3.0f, 2.5f, ChatFormatting.GOLD),
    LEGENDARY("legendary", 5.0f, 4.0f, ChatFormatting.LIGHT_PURPLE);
    
    private final String id;
    private final float statMultiplier;    // 属性倍率
    private final float dropMultiplier;    // 掉落倍率
    private final ChatFormatting color;
    
    MonsterRarity(String id, float statMultiplier, float dropMultiplier, ChatFormatting color) {
        this.id = id;
        this.statMultiplier = statMultiplier;
        this.dropMultiplier = dropMultiplier;
        this.color = color;
    }
    
    public String getId() { return id; }
    public float getStatMultiplier() { return statMultiplier; }
    public float getDropMultiplier() { return dropMultiplier; }
    public ChatFormatting getColor() { return color; }
    
    public MutableComponent getDisplayName() {
        return Component.translatable("monster.roadweaver_rpg.rarity." + id)
                .withStyle(color);
    }
    
    public static MonsterRarity fromId(String id) {
        for (MonsterRarity rarity : values()) {
            if (rarity.id.equalsIgnoreCase(id)) {
                return rarity;
            }
        }
        return NORMAL;
    }
    
    public static MonsterRarity fromOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return NORMAL;
    }
}
