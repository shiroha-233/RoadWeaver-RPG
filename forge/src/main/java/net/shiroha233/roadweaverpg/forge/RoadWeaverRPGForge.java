package net.shiroha233.roadweaverpg.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
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
import net.shiroha233.roadweaverpg.forge.entity.ModEntitiesForge;
import net.shiroha233.roadweaverpg.forge.item.ModItemsForge;
import net.shiroha233.roadweaverpg.forge.network.NetworkHandlerForge;
import net.shiroha233.roadweaverpg.quest.chain.QuestChainManager;
import net.shiroha233.roadweaverpg.quest.event.QuestEventHandler;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;
import net.shiroha233.roadweaverpg.worldgen.VillagePoolInjector;
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
        
        // 注册数据包重载监听器
        MinecraftForge.EVENT_BUS.addListener(RoadWeaverRPGForge::onAddReloadListeners);
        
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
    }
    
    @Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            VillagePoolInjector.injectGuildhallToVillages(event.getServer());
        }
        
        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                QuestEventHandler.onEntityKilled(killer, event.getEntity());
            }
        }
        
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEventHandler.onPlayerLogin(player);
            }
        }
        
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
                QuestEventHandler.onPlayerTick(player);
            }
        }
        
        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (event.getPlayer() instanceof ServerPlayer player) {
                QuestEventHandler.onBlockBroken(player, event.getState());
            }
        }
    }
}
