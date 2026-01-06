package net.shiroha233.roadweaverpg.quest.type;

import net.minecraft.ChatFormatting;

/**
 * 委托等级枚举
 */
public enum QuestRank {
    S("S", ChatFormatting.RED, 0xFF5555, 0),
    A("A", ChatFormatting.GOLD, 0xFFAA00, 1),
    B("B", ChatFormatting.YELLOW, 0xFFFF55, 2),
    C("C", ChatFormatting.BLUE, 0x5555FF, 3),
    D("D", ChatFormatting.GREEN, 0x55FF55, 4);
    
    private final String displayName;
    private final ChatFormatting textColor;
    private final int scrollColor;
    private final int index;
    
    QuestRank(String displayName, ChatFormatting textColor, int scrollColor, int index) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.scrollColor = scrollColor;
        this.index = index;
    }
    
    public String getDisplayName() { return displayName; }
    public ChatFormatting getColor() { return textColor; }
    public int getScrollColor() { return scrollColor; }
    public int getIndex() { return index; }
    
    public static QuestRank fromString(String name) {
        for (QuestRank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return D;
    }
    
    public static QuestRank fromIndex(int index) {
        for (QuestRank rank : values()) {
            if (rank.index == index) {
                return rank;
            }
        }
        return D;
    }
}
