package net.shiroha233.roadweaverpg.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric端物品注册
 */
public class ModItemsFabric {
    
    public static final Item QUEST_SCROLL = new QuestScrollItem(new Item.Properties());
    public static final Item COIN = new CoinItem(new Item.Properties());
    
    public static void register() {
        // 注册委托书物品
        Registry.register(BuiltInRegistries.ITEM, 
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "quest_scroll"), 
                QUEST_SCROLL);
        
        // 注册金币物品
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "coin"),
                COIN);
        
        // 初始化通用引用
        ModItems.init(() -> QUEST_SCROLL, () -> COIN);
        
        // 添加到创造模式物品栏
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(QUEST_SCROLL);
            entries.accept(COIN);
        });
        
        RoadWeaverRPG.LOGGER.info("Registered items for Fabric");
    }
}
