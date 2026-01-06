package net.shiroha233.roadweaverpg.shop;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric平台的商店管理器包装类
 * 实现IdentifiableResourceReloadListener接口以满足Fabric API要求
 */
public class ShopManagerFabric extends ShopManager implements IdentifiableResourceReloadListener {
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop");
    }
}
