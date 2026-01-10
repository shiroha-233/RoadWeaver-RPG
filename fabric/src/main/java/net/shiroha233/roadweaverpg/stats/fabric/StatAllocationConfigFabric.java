package net.shiroha233.roadweaverpg.stats.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.stats.StatAllocationConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric平台的属性分配配置加载器
 * 实现IdentifiableResourceReloadListener以支持Fabric的资源重载系统
 */
public class StatAllocationConfigFabric implements IdentifiableResourceReloadListener {
    
    private final StatAllocationConfig delegate;
    
    public StatAllocationConfigFabric() {
        this.delegate = new StatAllocationConfig();
    }
    
    private static final ResourceLocation ID = new ResourceLocation(
            RoadWeaverRPG.MOD_ID, "stat_allocation");
    
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
        // 委托给内部的StatAllocationConfig实例
        return delegate.reload(preparationBarrier, resourceManager, profiler, profiler2, backgroundExecutor, gameExecutor);
    }
}
