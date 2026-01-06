package net.shiroha233.roadweaverpg.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.network.QuestPacketHandler;
import net.shiroha233.roadweaverpg.network.packet.*;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Forge 网络处理器
 * 使用 QuestPacketHandler 处理公共逻辑
 */
public class NetworkHandlerForge {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void register() {
        // 服务端 -> 客户端：打开对话界面
        CHANNEL.registerMessage(packetId++, OpenDialogPacket.class,
                OpenDialogPacket::encode, OpenDialogPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleOpenDialog(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：同步委托数据
        CHANNEL.registerMessage(packetId++, SyncQuestsPacket.class,
                SyncQuestsPacket::encode, SyncQuestsPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncQuests(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：打开委托看板
        CHANNEL.registerMessage(packetId++, OpenQuestBoardPacket.class,
                OpenQuestBoardPacket::encode, OpenQuestBoardPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(ClientPacketHandler::handleOpenQuestBoard);
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：同步委托实例
        CHANNEL.registerMessage(packetId++, SyncQuestInstancePacket.class,
                SyncQuestInstancePacket::encode, SyncQuestInstancePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncQuestInstance(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：同步所有委托实例
        CHANNEL.registerMessage(packetId++, SyncAllQuestInstancesPacket.class,
                SyncAllQuestInstancesPacket::encode, SyncAllQuestInstancesPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncAllQuestInstances(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 服务端 -> 客户端：同步声望等级
        CHANNEL.registerMessage(packetId++, SyncReputationLevelsPacket.class,
                SyncReputationLevelsPacket::encode, SyncReputationLevelsPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncReputationLevels(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 服务端 -> 客户端：同步玩家声望
        CHANNEL.registerMessage(packetId++, SyncPlayerReputationPacket.class,
                SyncPlayerReputationPacket::encode, SyncPlayerReputationPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncPlayerReputation(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 服务端 -> 客户端：打开声望界面
        CHANNEL.registerMessage(packetId++, OpenReputationGuiPacket.class,
                OpenReputationGuiPacket::encode, OpenReputationGuiPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(ClientPacketHandler::handleOpenReputationGui);
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 客户端 -> 服务端：对话响应
        CHANNEL.registerMessage(packetId++, DialogResponsePacket.class,
                DialogResponsePacket::encode, DialogResponsePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            QuestPacketHandler.handleDialogResponse(player, packet, 
                                    () -> sendQuestsToClient(player));
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        // 客户端 -> 服务端：接受委托
        CHANNEL.registerMessage(packetId++, AcceptQuestPacket.class,
                AcceptQuestPacket::encode, AcceptQuestPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            QuestPacketHandler.handleAcceptQuest(player, packet, 
                                    NetworkHandlerForge::sendQuestInstance);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        // 客户端 -> 服务端：提交委托
        CHANNEL.registerMessage(packetId++, TurnInQuestPacket.class,
                TurnInQuestPacket::encode, TurnInQuestPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            QuestPacketHandler.handleTurnInQuest(player, packet);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        // 客户端 -> 服务端：请求委托进度
        CHANNEL.registerMessage(packetId++, RequestQuestProgressPacket.class,
                RequestQuestProgressPacket::encode, RequestQuestProgressPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            QuestPacketHandler.handleRequestQuestProgress(player, packet, 
                                    NetworkHandlerForge::sendQuestInstance);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        initializeCallbacks();
        registerShopPackets();
    }
    
    /** 初始化 PlayerQuestService 的回调 */
    private static void initializeCallbacks() {
        PlayerQuestService manager = PlayerQuestService.getInstance();
        manager.setOnQuestUpdated((player, instance) -> sendQuestInstance(player, new SyncQuestInstancePacket(instance)));
        manager.setOnQuestCompleted((player, instance) -> sendQuestInstance(player, new SyncQuestInstancePacket(instance)));
        manager.setOnSyncAllQuests(NetworkHandlerForge::sendAllQuestInstances);
        manager.setOnSyncAllDefinitions(NetworkHandlerForge::sendAllDefinitions);

        // 初始化声望同步回调
        manager.setOnSyncReputation((player, data) -> 
                sendPlayerReputation(player, data.getAllReputationXp(), data.getAllReputationLevels()));
        
        net.shiroha233.roadweaverpg.reputation.ReputationManager.setSyncCallback(
                NetworkHandlerForge::sendReputationLevels);
        net.shiroha233.roadweaverpg.network.QuestPacketHandler.setOnOpenReputationGui(
                NetworkHandlerForge::sendOpenReputationGui);
    }

    public static void sendReputationLevels(ServerPlayer player, Collection<net.shiroha233.roadweaverpg.reputation.ReputationLevel> levels) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncReputationLevelsPacket(levels));
    }

    public static void sendPlayerReputation(ServerPlayer player, java.util.Map<ResourceLocation, Integer> reputations, java.util.Map<ResourceLocation, Integer> levels) {
        // 将 ResourceLocation 转为 String 进行网络传输
        java.util.Map<String, Integer> repStrings = new java.util.HashMap<>();
        reputations.forEach((k, v) -> repStrings.put(k.toString(), v));
        java.util.Map<String, Integer> levelStrings = new java.util.HashMap<>();
        levels.forEach((k, v) -> levelStrings.put(k.toString(), v));
        
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                new SyncPlayerReputationPacket(repStrings, levelStrings));
    }

    public static void sendOpenReputationGui(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenReputationGuiPacket());
    }
    
    public static void sendAllDefinitions(ServerPlayer player, Collection<QuestDefinition> definitions) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                new SyncQuestsPacket(new java.util.ArrayList<>(definitions)));
    }
    
    public static void sendQuestsToClient(ServerPlayer player) {
        List<QuestDefinition> quests = PlayerQuestService.getInstance().getAvailableQuests(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncQuestsPacket(quests));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenQuestBoardPacket());
    }
    
    public static void sendQuestInstance(ServerPlayer player, SyncQuestInstancePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendAllQuestInstances(ServerPlayer player, Collection<QuestInstance> instances) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                SyncAllQuestInstancesPacket.fromCollection(instances));
    }
    
    public static void sendOpenDialog(ServerPlayer player, int entityId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenDialogPacket(entityId));
    }
    
    // ==================== 商店系统网络方法 ====================
    
    public static void sendOpenShopDialog(ServerPlayer player, int entityId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenDialogPacket(entityId));
    }
    
    public static void sendOpenShop(ServerPlayer player, int entityId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                net.shiroha233.roadweaverpg.network.ShopPacketHandler.createOpenShopPacket(player, entityId));
    }
    
    public static void sendSyncCoins(ServerPlayer player, int coins) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCoinsPacket(coins));
    }
    
    /** 注册商店相关的网络包 */
    public static void registerShopPackets() {
        // 服务端 -> 客户端：打开商店对话
        CHANNEL.registerMessage(packetId++, OpenDialogPacket.class,
                OpenDialogPacket::encode, OpenDialogPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleOpenShopDialog(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：打开商店界面
        CHANNEL.registerMessage(packetId++, OpenShopPacket.class,
                OpenShopPacket::encode, OpenShopPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleOpenShop(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：同步金币
        CHANNEL.registerMessage(packetId++, SyncCoinsPacket.class,
                SyncCoinsPacket::encode, SyncCoinsPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncCoins(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 客户端 -> 服务端：商店对话响应
        CHANNEL.registerMessage(packetId++, ShopDialogResponsePacket.class,
                ShopDialogResponsePacket::encode, ShopDialogResponsePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            net.shiroha233.roadweaverpg.network.ShopPacketHandler.handleShopDialogResponse(
                                    player, packet, p -> sendOpenShop(p, packet.entityId()));
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        // 客户端 -> 服务端：商店购买
        CHANNEL.registerMessage(packetId++, ShopPurchasePacket.class,
                ShopPurchasePacket::encode, ShopPurchasePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            net.shiroha233.roadweaverpg.network.ShopPacketHandler.handlePurchase(
                                    player, packet, (p, syncPacket) -> sendSyncCoins(p, syncPacket.coins()));
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
