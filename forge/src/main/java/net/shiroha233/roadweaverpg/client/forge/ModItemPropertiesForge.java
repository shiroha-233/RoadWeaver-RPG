package net.shiroha233.roadweaverpg.client.forge;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.item.forge.ModItemsForge;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;

/**
 * Forge端物品属性注册 - 用于模型谓词
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModItemPropertiesForge {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册委托书的has_quest谓词
            ItemProperties.register(
                    ModItemsForge.QUEST_SCROLL.get(),
                    new ResourceLocation(RoadWeaverRPG.MOD_ID, "has_quest"),
                    (stack, level, entity, seed) -> QuestScrollItem.hasQuest(stack) ? 1.0f : 0.0f
            );
        });
    }
}
