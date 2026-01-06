package net.shiroha233.roadweaverpg.shop;

import net.minecraft.network.chat.Component;

/**
 * 商店分类枚举
 */
public enum ShopCategory {
    WEAPONS("weapons", 0xFFCC4444),
    ARMOR("armor", 0xFF4488CC),
    TOOLS("tools", 0xFF44CC44),
    MATERIALS("materials", 0xFFCCCC44),
    CONSUMABLES("consumables", 0xFFCC44CC),
    POTIONS("potions", 0xFF8844FF),
    BLOCKS("blocks", 0xFF888888),
    DECORATIONS("decorations", 0xFFFF88AA),
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
