package net.shiroha233.roadweaverpg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.shiroha233.roadweaverpg.command.QuestDebugCommand;
import net.shiroha233.roadweaverpg.entity.ModEntitiesFabric;
import net.shiroha233.roadweaverpg.entity.NPCSoundProviderFabric;
import net.shiroha233.roadweaverpg.event.CoinEventsFabric;
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
        
        // 初始化平台助手
        net.shiroha233.roadweaverpg.platform.PlatformHelper.setImplementation(
                new net.shiroha233.roadweaverpg.platform.PlatformHelperFabric());
        
        // 注册实体
        ModEntitiesFabric.register();
        
        // 注册物品
        ModItemsFabric.register();
        
        // 初始化经验书回调
        net.shiroha233.roadweaverpg.item.ExpBookItem.setUseExpBookHandler((player, expAmount) -> {
            net.shiroha233.roadweaverpg.playerlevel.PlayerLevelDataService.getInstance().addPlayerExp(player, expAmount);
        });
        
        // 注册网络处理器
        NetworkHandlerFabric.registerServerReceivers();
        
        // 初始化NPC声音事件提供者
        NPCSoundProviderFabric.init();
        
        // 注册委托数据加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new QuestManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new QuestChainManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new ReputationManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.shop.ShopManagerFabric());
        
        // 注册冒险等级数据加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.adventure.AdventureLevelManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.adventure.AdventureExpSourceManagerFabric());
        
        // 注册玩家等级数据加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.playerlevel.PlayerLevelManagerFabric());
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.playerlevel.PlayerExpSourceManagerFabric());
        
        // 注册属性分配配置加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.stats.StatAllocationConfigFabric());
        
        // 注册对话数据加载器（使用Fabric包装类）
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(net.shiroha233.roadweaverpg.dialog.DialogRegistryFabric.getInstance());
        
        // 注册NPC行为数据加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorLoaderFabric());
        
        // 注册货币战利品配置加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.loot.CoinLootConfigManagerFabric());
        
        // 注册世界难度配置加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.worlddifficulty.DifficultyConfigManagerFabric());
        
        // 注册职业系统数据加载器
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new net.shiroha233.roadweaverpg.profession.ProfessionManagerFabric());
        
        // 初始化魔法模组兼容层
        net.shiroha233.roadweaverpg.compat.magic.MagicCompatInitFabric.init();
        
        // 注册委托事件监听
        QuestEventsFabric.register();
        CoinEventsFabric.register();
        net.shiroha233.roadweaverpg.event.WorldDifficultyEventsFabric.register();
        registerBlockEvents();
        registerServerTickEvents();
        
        // 初始化战斗系统回调
        NetworkHandlerFabric.initializeCombatCallbacks();
        
        // 注册调试指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            QuestDebugCommand.register(dispatcher);
            net.shiroha233.roadweaverpg.command.StatEffectCommand.register(dispatcher);
            net.shiroha233.roadweaverpg.command.StatPointCommand.register(dispatcher);
            net.shiroha233.roadweaverpg.command.WorldDifficultyCommand.register(dispatcher);
            net.shiroha233.roadweaverpg.command.AdventureCommand.register(dispatcher);
        });
        
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
        
        // 注册战利品表修改器（向所有宝箱注入金币）
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            // 匹配所有宝箱类型的战利品表（包括第三方结构）
            String path = id.getPath();
            if (path.contains("chest") || path.contains("treasure") || path.contains("reward")) {
                net.shiroha233.roadweaverpg.loot.CoinLootInjector.injectCoinPool(tableBuilder);
            }
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
    
    private void registerServerTickEvents() {
        // 服务端Level Tick事件
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                QuestEventHandler.onServerLevelTick(level);
            }
        });
        
        // 玩家登录事件 - 职业系统
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            net.shiroha233.roadweaverpg.profession.ProfessionEventHandler.onPlayerLogin(player);
        });
    }
}
