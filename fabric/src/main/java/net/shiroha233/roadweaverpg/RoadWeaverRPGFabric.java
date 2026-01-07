package net.shiroha233.roadweaverpg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.shiroha233.roadweaverpg.command.QuestDebugCommand;
import net.shiroha233.roadweaverpg.entity.ModEntitiesFabric;
import net.shiroha233.roadweaverpg.event.QuestEventsFabric;
import net.shiroha233.roadweaverpg.item.ModItemsFabric;
import net.shiroha233.roadweaverpg.network.NetworkHandlerFabric;
import net.shiroha233.roadweaverpg.quest.QuestChainManagerFabric;
import net.shiroha233.roadweaverpg.reputation.ReputationManagerFabric;
import net.shiroha233.roadweaverpg.quest.QuestManagerFabric;
import net.shiroha233.roadweaverpg.quest.event.QuestEventHandler;
import net.shiroha233.roadweaverpg.worldgen.VillagePoolInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadWeaverRPGFabric implements ModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver_rpg");
    
    @Override
    public void onInitialize() {
        LOGGER.info("Loading RoadWeaver RPG for Fabric...");
        
        // 注册实体
        ModEntitiesFabric.register();
        
        // 注册物品
        ModItemsFabric.register();
        
        // 注册网络处理器
        NetworkHandlerFabric.registerServerReceivers();
        
        // 注册委托数据加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new QuestManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new QuestChainManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new ReputationManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.shop.ShopManagerFabric());
        
        // 注册委托事件监听
        QuestEventsFabric.register();
        registerBlockEvents();
        
        // 注册调试指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
                QuestDebugCommand.register(dispatcher));
        
        // 检查前置依赖
        if (!RoadWeaverRPG.isRoadWeaverAvailable()) {
            LOGGER.warn("RoadWeaver main mod not found! Some features will be disabled.");
            RoadWeaverRPG.initializeStandalone();
        } else {
            RoadWeaverRPG.initialize();
        }
        
        // 注册服务器启动事件
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            VillagePoolInjector.injectGuildhallToVillages(server);
        });
    }
    
    private void registerBlockEvents() {
        // 方块破坏事件
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                QuestEventHandler.onBlockBroken(serverPlayer, state);
            }
        });
    }
}
