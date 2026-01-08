package net.shiroha233.roadweaverpg.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.command.QuestDebugCommand;
import net.shiroha233.roadweaverpg.forge.entity.ModEntitiesForge;
import net.shiroha233.roadweaverpg.forge.entity.NPCSoundProviderForge;
import net.shiroha233.roadweaverpg.forge.item.ModItemsForge;
import net.shiroha233.roadweaverpg.forge.loot.ModLootModifiersForge;
import net.shiroha233.roadweaverpg.forge.network.NetworkHandlerForge;
import net.shiroha233.roadweaverpg.quest.chain.QuestChainManager;
import net.shiroha233.roadweaverpg.quest.event.QuestEventHandler;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;
import net.shiroha233.roadweaverpg.worldgen.VillagePoolInjector;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;
import net.shiroha233.roadweaverpg.client.config.ConfigScreenBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RoadWeaver RPG Forge 入口类
 */
@Mod(RoadWeaverRPG.MOD_ID)
public class RoadWeaverRPGForge {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver_rpg");
    
    public RoadWeaverRPGForge() {
        LOGGER.info("Loading RoadWeaver RPG for Forge...");
        
        // 注册网络处理器
        NetworkHandlerForge.register();
        
        // 注册实体
        ModEntitiesForge.ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().register(ModEntitiesForge.class);
        
        // 注册物品
        ModItemsForge.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItemsForge.init();
        
        // 注册全局战利品修改器
        ModLootModifiersForge.register(FMLJavaModLoadingContext.get().getModEventBus());
        
        // 初始化金币事件处理
        net.shiroha233.roadweaverpg.forge.event.CoinEventsForge.init();
        
        // 注册数据包重载监听器
        MinecraftForge.EVENT_BUS.addListener(RoadWeaverRPGForge::onAddReloadListeners);
        
        // 初始化NPC声音事件提供者
        NPCSoundProviderForge.init();
        
        // 注册 Config Screen
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> ConfigScreenBuilder.create(screen)));
        
        // 检查前置依赖
        if (!RoadWeaverRPG.isRoadWeaverAvailable()) {
            LOGGER.warn("RoadWeaver main mod not found! Some features will be disabled.");
            RoadWeaverRPG.initializeStandalone();
        } else {
            RoadWeaverRPG.initialize();
        }
    }
    
    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestDefinitionLoader());
        event.addListener(new QuestChainManager());
        event.addListener(new ReputationManager());
        event.addListener(new net.shiroha233.roadweaverpg.shop.ShopManager());
        // 注册对话数据加载器
        event.addListener(net.shiroha233.roadweaverpg.dialog.DialogRegistry.getInstance());
        // 注册NPC行为数据加载器
        event.addListener(net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorLoader.getInstance());
        // 注册冒险等级数据加载器
        event.addListener(new net.shiroha233.roadweaverpg.adventure.AdventureLevelManager());
        event.addListener(new net.shiroha233.roadweaverpg.adventure.AdventureExpSourceManager());
        // 注册玩家等级数据加载器
        event.addListener(new net.shiroha233.roadweaverpg.playerlevel.PlayerLevelManager());
        event.addListener(new net.shiroha233.roadweaverpg.playerlevel.PlayerExpSourceManager());
    }
    
    @Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            VillagePoolInjector.injectGuildhallToVillages(event.getServer());
        }
        
        @SubscribeEvent
        public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
            LOGGER.info("Server stopping, forcing save of all quest data...");
            try {
                // 强制保存所有世界的委托数据
                for (net.minecraft.server.level.ServerLevel level : event.getServer().getAllLevels()) {
                    net.shiroha233.roadweaverpg.data.QuestSavedData savedData = 
                            net.shiroha233.roadweaverpg.data.QuestSavedData.get(level);
                    savedData.setDirty();
                    LOGGER.info("Marked quest data dirty for level: {}, player count: {}", 
                            level.dimension().location(), savedData.getPlayerCount());
                }
                LOGGER.info("Quest data save completed");
            } catch (Exception e) {
                LOGGER.error("Failed to save quest data on server stop: {}", e.getMessage(), e);
            }
        }
        
        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                QuestEventHandler.onEntityKilled(killer, event.getEntity());
                // 冒险等级经验
                net.shiroha233.roadweaverpg.adventure.AdventureEventHandler.onEntityKilled(event.getEntity(), killer);
                // 玩家等级经验
                net.shiroha233.roadweaverpg.playerlevel.PlayerLevelEventHandler.onEntityKilled(event.getEntity(), killer);
            }
        }
        
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEventHandler.onPlayerLogin(player);
                // 同步冒险等级数据
                net.shiroha233.roadweaverpg.adventure.AdventureEventHandler.onPlayerLogin(player);
                // 同步玩家等级数据
                net.shiroha233.roadweaverpg.playerlevel.PlayerLevelEventHandler.onPlayerLogin(player);
                // 同步钱包数据
                long coins = net.shiroha233.roadweaverpg.wallet.WalletService.getCoins(player);
                NetworkHandlerForge.sendSyncWallet(player, coins);
            }
        }
        
        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                // 重新应用玩家等级效果
                net.shiroha233.roadweaverpg.playerlevel.PlayerLevelEventHandler.onPlayerRespawn(player);
            }
        }
        
        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEventHandler.onPlayerLogout(player);
                
                // 玩家登出时强制保存数据
                try {
                    net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance().forceSave(player);
                    LOGGER.debug("Forced save quest data for player: {}", player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("Failed to force save on logout for {}: {}", 
                            player.getName().getString(), e.getMessage());
                }
            }
        }
        
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
                QuestEventHandler.onPlayerTick(player);
            }
        }
        
        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.END && 
                event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                QuestEventHandler.onServerLevelTick(serverLevel);
            }
        }
        
        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (event.getPlayer() instanceof ServerPlayer player) {
                QuestEventHandler.onBlockBroken(player, event.getState());
            }
        }
        
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            QuestDebugCommand.register(event.getDispatcher());
        }
    }
}
