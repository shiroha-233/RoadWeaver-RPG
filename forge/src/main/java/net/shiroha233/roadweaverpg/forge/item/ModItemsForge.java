package net.shiroha233.roadweaverpg.forge.item;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.currency.CurrencyType;
import net.shiroha233.roadweaverpg.item.CurrencyItem;
import net.shiroha233.roadweaverpg.item.ExpBookItem;
import net.shiroha233.roadweaverpg.item.ModItems;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;

/**
 * Forge端物品注册
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItemsForge {
    
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, RoadWeaverRPG.MOD_ID);
    
    public static final RegistryObject<Item> QUEST_SCROLL = ITEMS.register("quest_scroll",
            () -> new QuestScrollItem(new Item.Properties()));
    
    // 货币物品
    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin",
            () -> new CurrencyItem(CurrencyType.COPPER, new Item.Properties()));
    
    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin",
            () -> new CurrencyItem(CurrencyType.SILVER, new Item.Properties()));
    
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            () -> new CurrencyItem(CurrencyType.GOLD, new Item.Properties()));
    
    public static final RegistryObject<Item> EMERALD_COIN = ITEMS.register("emerald_coin",
            () -> new CurrencyItem(CurrencyType.EMERALD, new Item.Properties()));
    
    public static final RegistryObject<Item> DIAMOND_COIN = ITEMS.register("diamond_coin",
            () -> new CurrencyItem(CurrencyType.DIAMOND, new Item.Properties()));
    
    // 经验书物品
    public static final RegistryObject<Item> EXP_BOOK_SMALL = ITEMS.register("exp_book_small",
            () -> new ExpBookItem(ExpBookItem.Tier.SMALL, new Item.Properties()));
    
    public static final RegistryObject<Item> EXP_BOOK_MEDIUM = ITEMS.register("exp_book_medium",
            () -> new ExpBookItem(ExpBookItem.Tier.MEDIUM, new Item.Properties()));
    
    public static final RegistryObject<Item> EXP_BOOK_LARGE = ITEMS.register("exp_book_large",
            () -> new ExpBookItem(ExpBookItem.Tier.LARGE, new Item.Properties()));
    
    public static final RegistryObject<Item> EXP_BOOK_GRAND = ITEMS.register("exp_book_grand",
            () -> new ExpBookItem(ExpBookItem.Tier.GRAND, new Item.Properties()));
    
    /**
     * 初始化通用引用
     */
    public static void init() {
        ModItems.init(QUEST_SCROLL, 
                     COPPER_COIN,
                     SILVER_COIN,
                     GOLD_COIN,
                     EMERALD_COIN,
                     DIAMOND_COIN);
        
        ModItems.initExpBooks(EXP_BOOK_SMALL,
                             EXP_BOOK_MEDIUM,
                             EXP_BOOK_LARGE,
                             EXP_BOOK_GRAND);
    }
    
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(QUEST_SCROLL.get());
            event.accept(COPPER_COIN.get());
            event.accept(SILVER_COIN.get());
            event.accept(GOLD_COIN.get());
            event.accept(EMERALD_COIN.get());
            event.accept(DIAMOND_COIN.get());
            event.accept(EXP_BOOK_SMALL.get());
            event.accept(EXP_BOOK_MEDIUM.get());
            event.accept(EXP_BOOK_LARGE.get());
            event.accept(EXP_BOOK_GRAND.get());
        }
    }
}
