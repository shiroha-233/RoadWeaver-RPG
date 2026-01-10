package net.shiroha233.roadweaverpg.entity.fabric.npc.data;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorLoader;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fabric平台NPC行为数据加载器包装类
 * 职责：为Fabric提供可识别的资源重载监听器
 * 原理：委托给common模块的NPCBehaviorLoader
 */
public class NPCBehaviorLoaderFabric implements IdentifiableResourceReloadListener {
    
    private static final ResourceLocation ID = new ResourceLocation(RoadWeaverRPG.MOD_ID, "npc_behavior_loader");
    private final NPCBehaviorLoader delegate = NPCBehaviorLoader.getInstance();
    
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
