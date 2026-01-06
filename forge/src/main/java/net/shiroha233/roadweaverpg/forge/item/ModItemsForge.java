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
import net.shiroha233.roadweaverpg.item.CoinItem;
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
    
    public static final RegistryObject<Item> COIN = ITEMS.register("coin",
            () -> new CoinItem(new Item.Properties()));
    
    /**
     * 初始化通用引用
     */
    public static void init() {
        ModItems.init(QUEST_SCROLL, COIN);
    }
    
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(QUEST_SCROLL.get());
            event.accept(COIN.get());
        }
    }
}
