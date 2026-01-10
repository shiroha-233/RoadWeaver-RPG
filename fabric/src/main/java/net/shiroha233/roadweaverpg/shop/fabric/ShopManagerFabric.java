package net.shiroha233.roadweaverpg.shop.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.shop.ShopManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric平台的商店管理器包装类
 * 实现IdentifiableResourceReloadListener接口以满足Fabric API要求
 */
public class ShopManagerFabric implements IdentifiableResourceReloadListener {
    
    private final ShopManager delegate;
    
    public ShopManagerFabric() {
        this.delegate = new ShopManager();
    }
    
    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop");
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
        // 委托给内部的ShopManager实例
        return delegate.reload(preparationBarrier, resourceManager, profiler, profiler2, backgroundExecutor, gameExecutor);
    }
    
    // 代理方法
    public static ShopManagerFabric getInstance() {
        return new ShopManagerFabric();
    }
}
