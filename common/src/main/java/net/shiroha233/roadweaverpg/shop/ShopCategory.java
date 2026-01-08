package net.shiroha233.roadweaverpg.shop;

import net.minecraft.network.chat.Component;

/**
 * 商店分类枚举
 */
public enum ShopCategory {
    COMBAT("combat", 0xFFCC4444),
    TOOLS("tools", 0xFF44CC44),
    FOOD("food", 0xFFCC44CC),
    MATERIALS("materials", 0xFFCCCC44),
    BUILDING("building", 0xFF886644),
    FUNCTIONAL("functional", 0xFF888888),
    REDSTONE("redstone", 0xFFFF4444),
    NATURAL("natural", 0xFF44AA44),
    COLORED("colored", 0xFFCC66FF),
    SPECIAL("special", 0xFFFFAA00);
    
    private final String id;
    private final int color;
    
    ShopCategory(String id, int color) {
        this.id = id;
        this.color = color;
    }
    
    public String getId() { return id; }
    public int getColor() { return color; }
    
    public Component getDisplayName() {
        return Component.translatable("shop.roadweaver_rpg.category." + id);
    }
    
    public static ShopCategory fromString(String name) {
        for (ShopCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(name) || cat.name().equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return MATERIALS;
    }
}
