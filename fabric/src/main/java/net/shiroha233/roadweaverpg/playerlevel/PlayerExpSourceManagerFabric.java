package net.shiroha233.roadweaverpg.playerlevel;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric平台的玩家经验来源管理器
 */
public class PlayerExpSourceManagerFabric extends PlayerExpSourceManager 
        implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(
            RoadWeaverRPG.MOD_ID, "player_exp_sources");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
