package net.shiroha233.roadweaverpg.dialog;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric 平台的对话注册表包装
 * 实现 IdentifiableResourceReloadListener 接口
 */
public class DialogRegistryFabric extends DialogRegistry implements IdentifiableResourceReloadListener {
    
    private static DialogRegistryFabric instance;
    
    public static DialogRegistryFabric getInstance() {
        if (instance == null) {
            instance = new DialogRegistryFabric();
        }
        return instance;
    }
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "dialogs");
    }
}
