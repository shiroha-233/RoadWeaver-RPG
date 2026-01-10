package net.shiroha233.roadweaverpg.adventure.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.adventure.AdventureExpSourceManager;

/**
 * Fabric 平台的冒险经验来源管理器
 */
public class AdventureExpSourceManagerFabric extends AdventureExpSourceManager implements IdentifiableResourceReloadListener {
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "adventure_exp_sources");
    }
}
