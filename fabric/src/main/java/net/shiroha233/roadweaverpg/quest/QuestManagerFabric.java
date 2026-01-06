package net.shiroha233.roadweaverpg.quest;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;

/**
 * Fabric 平台的委托定义管理器
 */
public class QuestManagerFabric extends QuestDefinitionLoader implements IdentifiableResourceReloadListener {
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "quests");
    }
}
