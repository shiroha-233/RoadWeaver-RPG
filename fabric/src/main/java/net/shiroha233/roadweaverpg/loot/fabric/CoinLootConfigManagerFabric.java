package net.shiroha233.roadweaverpg.loot.fabric;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.loot.CoinLootConfigManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric适配器 - 货币战利品配置管理器
 * 将Common模块的CoinLootConfigManager包装为Fabric的IdentifiableResourceReloadListener
 */
public class CoinLootConfigManagerFabric implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(RoadWeaverRPG.MOD_ID, "coin_loot_config");
    private final CoinLootConfigManager delegate = new CoinLootConfigManager();
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
    
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, 
                                          ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, 
                                          ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, 
                                          Executor gameExecutor) {
        return delegate.reload(preparationBarrier, resourceManager, 
                preparationsProfiler, reloadProfiler, 
                backgroundExecutor, gameExecutor);
    }
}
