package net.shiroha233.roadweaverpg.reputation;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric 平台的声望管理器
 */
public class ReputationManagerFabric extends ReputationManager implements IdentifiableResourceReloadListener {
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "reputation_levels");
    }
}
