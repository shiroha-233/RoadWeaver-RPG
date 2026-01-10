package net.shiroha233.roadweaverpg.worlddifficulty.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.worlddifficulty.DifficultyConfigManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric平台难度配置管理器包装类
 * 
 * 原理：
 * - 实现Fabric的IdentifiableResourceReloadListener接口
 * - 使用组合模式包装common模块的DifficultyConfigManager
 */
public class DifficultyConfigManagerFabric implements IdentifiableResourceReloadListener {
    
    private final DifficultyConfigManager delegate;
    
    public DifficultyConfigManagerFabric() {
        this.delegate = new DifficultyConfigManager();
    }
    
    private static final ResourceLocation ID = new ResourceLocation(RoadWeaverRPG.MOD_ID, "difficulty_scaling");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
    
    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier preparationBarrier,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            ProfilerFiller profiler2,
            Executor backgroundExecutor,
            Executor gameExecutor
    ) {
        // 委托给内部的DifficultyConfigManager实例
        return delegate.reload(preparationBarrier, resourceManager, profiler, profiler2, backgroundExecutor, gameExecutor);
    }
}
