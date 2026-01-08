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
        }
    }
}
