package net.shiroha233.roadweaverpg.item;

import net.minecraft.world.item.Item;
import java.util.function.Supplier;

/**
 * 物品注册接口 - 由平台特定代码实现
 */
public class ModItems {
    
    // 委托书物品
    public static Supplier<Item> QUEST_SCROLL;
    // 金币物品
    public static Supplier<Item> COIN;
    
    /**
     * 初始化物品引用（由平台调用）
     */
    public static void init(Supplier<Item> questScroll, Supplier<Item> coin) {
        QUEST_SCROLL = questScroll;
        COIN = coin;
    }
}
