package net.shiroha233.roadweaverpg.playerlevel.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.playerlevel.PlayerLevelManager;

/**
 * Fabric平台的玩家等级管理器
 * 实现IdentifiableResourceReloadListener以支持Fabric的资源重载系统
 */
public class PlayerLevelManagerFabric extends PlayerLevelManager 
        implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(
            RoadWeaverRPG.MOD_ID, "player_levels");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
