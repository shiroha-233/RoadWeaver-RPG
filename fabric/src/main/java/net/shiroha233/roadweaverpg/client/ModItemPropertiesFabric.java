package net.shiroha233.roadweaverpg.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.item.ModItemsFabric;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;

/**
 * Fabric端物品属性注册 - 用于模型谓词
 */
@Environment(EnvType.CLIENT)
public class ModItemPropertiesFabric {
    
    public static void register() {
        // 注册委托书的has_quest谓词
        ItemProperties.register(
                ModItemsFabric.QUEST_SCROLL,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "has_quest"),
                (stack, level, entity, seed) -> QuestScrollItem.hasQuest(stack) ? 1.0f : 0.0f
        );
    }
}
