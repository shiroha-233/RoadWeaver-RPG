package net.shiroha233.roadweaverpg.stats;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric平台的属性分配配置加载器
 * 实现IdentifiableResourceReloadListener以支持Fabric的资源重载系统
 */
public class StatAllocationConfigFabric extends StatAllocationConfig 
        implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(
            RoadWeaverRPG.MOD_ID, "stat_allocation");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
