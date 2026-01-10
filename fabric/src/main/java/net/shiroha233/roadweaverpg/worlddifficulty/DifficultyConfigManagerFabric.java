package net.shiroha233.roadweaverpg.worlddifficulty;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric平台难度配置管理器包装类
 * 
 * 原理：
 * - 实现Fabric的IdentifiableResourceReloadListener接口
 * - 继承common模块的DifficultyConfigManager
 */
public class DifficultyConfigManagerFabric extends DifficultyConfigManager 
        implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(RoadWeaverRPG.MOD_ID, "difficulty_scaling");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
