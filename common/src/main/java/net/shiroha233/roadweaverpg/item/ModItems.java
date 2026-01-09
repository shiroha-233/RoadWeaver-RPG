package net.shiroha233.roadweaverpg.item;

import net.minecraft.world.item.Item;
import java.util.function.Supplier;

/**
 * 物品注册接口 - 由平台特定代码实现
 */
public class ModItems {
    
    // 委托书物品
    public static Supplier<Item> QUEST_SCROLL;
    
    // 货币物品
    public static Supplier<Item> COPPER_COIN;
    public static Supplier<Item> SILVER_COIN;
    public static Supplier<Item> GOLD_COIN;
    public static Supplier<Item> EMERALD_COIN;
    public static Supplier<Item> DIAMOND_COIN;
    
    // 经验书物品
    public static Supplier<Item> EXP_BOOK_SMALL;
    public static Supplier<Item> EXP_BOOK_MEDIUM;
    public static Supplier<Item> EXP_BOOK_LARGE;
    public static Supplier<Item> EXP_BOOK_GRAND;
    
    // 旧的金币引用（向后兼容，指向金币）
    @Deprecated
    public static Supplier<Item> COIN;
    
    /**
     * 初始化物品引用（由平台调用）
     */
    public static void init(Supplier<Item> questScroll, 
                           Supplier<Item> copperCoin,
                           Supplier<Item> silverCoin,
                           Supplier<Item> goldCoin,
                           Supplier<Item> emeraldCoin,
                           Supplier<Item> diamondCoin) {
        QUEST_SCROLL = questScroll;
        COPPER_COIN = copperCoin;
        SILVER_COIN = silverCoin;
        GOLD_COIN = goldCoin;
        EMERALD_COIN = emeraldCoin;
        DIAMOND_COIN = diamondCoin;
        
        // 向后兼容
        COIN = goldCoin;
    }
    
    /**
     * 初始化经验书物品引用（由平台调用）
     */
    public static void initExpBooks(Supplier<Item> small, 
                                    Supplier<Item> medium,
                                    Supplier<Item> large,
                                    Supplier<Item> grand) {
        EXP_BOOK_SMALL = small;
        EXP_BOOK_MEDIUM = medium;
        EXP_BOOK_LARGE = large;
        EXP_BOOK_GRAND = grand;
    }
    
    /**
     * 检查物品是否为货币
     */
    public static boolean isCurrency(Item item) {
        return item instanceof CurrencyItem;
    }
    
    /**
     * 检查物品是否为经验书
     */
    public static boolean isExpBook(Item item) {
        return item instanceof ExpBookItem;
    }
}
