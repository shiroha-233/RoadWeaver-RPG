package net.shiroha233.roadweaverpg.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.currency.CurrencyType;

/**
 * Fabric端物品注册
 */
public class ModItemsFabric {
    
    public static final Item QUEST_SCROLL = new QuestScrollItem(new Item.Properties());
    
    // 货币物品
    public static final Item COPPER_COIN = new CurrencyItem(CurrencyType.COPPER, new Item.Properties());
    public static final Item SILVER_COIN = new CurrencyItem(CurrencyType.SILVER, new Item.Properties());
    public static final Item GOLD_COIN = new CurrencyItem(CurrencyType.GOLD, new Item.Properties());
    public static final Item EMERALD_COIN = new CurrencyItem(CurrencyType.EMERALD, new Item.Properties());
    public static final Item DIAMOND_COIN = new CurrencyItem(CurrencyType.DIAMOND, new Item.Properties());
    
    // 经验书物品
    public static final Item EXP_BOOK_SMALL = new ExpBookItem(ExpBookItem.Tier.SMALL, new Item.Properties());
    public static final Item EXP_BOOK_MEDIUM = new ExpBookItem(ExpBookItem.Tier.MEDIUM, new Item.Properties());
    public static final Item EXP_BOOK_LARGE = new ExpBookItem(ExpBookItem.Tier.LARGE, new Item.Properties());
    public static final Item EXP_BOOK_GRAND = new ExpBookItem(ExpBookItem.Tier.GRAND, new Item.Properties());
    
    public static void register() {
        // 注册委托书物品
        Registry.register(BuiltInRegistries.ITEM, 
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "quest_scroll"), 
                QUEST_SCROLL);
        
        // 注册货币物品
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "copper_coin"),
                COPPER_COIN);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "silver_coin"),
                SILVER_COIN);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "gold_coin"),
                GOLD_COIN);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "emerald_coin"),
                EMERALD_COIN);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "diamond_coin"),
                DIAMOND_COIN);
        
        // 注册经验书物品
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "exp_book_small"),
                EXP_BOOK_SMALL);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "exp_book_medium"),
                EXP_BOOK_MEDIUM);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "exp_book_large"),
                EXP_BOOK_LARGE);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "exp_book_grand"),
                EXP_BOOK_GRAND);
        
        // 初始化通用引用
        ModItems.init(() -> QUEST_SCROLL, 
                     () -> COPPER_COIN,
                     () -> SILVER_COIN,
                     () -> GOLD_COIN,
                     () -> EMERALD_COIN,
                     () -> DIAMOND_COIN);
        
        ModItems.initExpBooks(() -> EXP_BOOK_SMALL,
                             () -> EXP_BOOK_MEDIUM,
                             () -> EXP_BOOK_LARGE,
                             () -> EXP_BOOK_GRAND);
        
        // 添加到创造模式物品栏
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(QUEST_SCROLL);
            entries.accept(COPPER_COIN);
            entries.accept(SILVER_COIN);
            entries.accept(GOLD_COIN);
            entries.accept(EMERALD_COIN);
            entries.accept(DIAMOND_COIN);
            entries.accept(EXP_BOOK_SMALL);
            entries.accept(EXP_BOOK_MEDIUM);
            entries.accept(EXP_BOOK_LARGE);
            entries.accept(EXP_BOOK_GRAND);
        });
        
        RoadWeaverRPG.LOGGER.info("Registered items for Fabric");
    }
}
