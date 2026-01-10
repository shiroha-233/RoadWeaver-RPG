package net.shiroha233.roadweaverpg.quest.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.chain.QuestChainManager;

/**
 * Fabric 平台的委托链管理器
 */
public class QuestChainManagerFabric extends QuestChainManager implements IdentifiableResourceReloadListener {
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "quest_chains");
    }
}
