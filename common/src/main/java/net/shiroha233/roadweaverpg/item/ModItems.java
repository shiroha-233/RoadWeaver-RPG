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
     * 检查物品是否为货币
     */
    public static boolean isCurrency(Item item) {
        return item instanceof CurrencyItem;
    }
}
