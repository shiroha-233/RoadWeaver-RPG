package net.shiroha233.roadweaverpg.profession;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric平台职业管理器包装类
 * 实现IdentifiableResourceReloadListener以支持Fabric资源重载
 */
public class ProfessionManagerFabric extends ProfessionManager implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(RoadWeaverRPG.MOD_ID, "professions");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
