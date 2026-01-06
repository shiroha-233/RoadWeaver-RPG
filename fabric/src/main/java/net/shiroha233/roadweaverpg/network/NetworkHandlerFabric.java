package net.shiroha233.roadweaverpg.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.network.packet.*;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;

import java.util.Collection;
import java.util.List;

/**
 * Fabric 网络处理器
 * 使用 QuestPacketHandler 处理公共逻辑
 */
public class NetworkHandlerFabric {
    
    public static void registerServerReceivers() {
        // 注册对话响应处理器
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.DIALOG_RESPONSE, (server, player, handler, buf, responseSender) -> {
            DialogResponsePacket packet = DialogResponsePacket.decode(buf);
            server.execute(() -> {
                Runnable sendQuests = () -> sendQuestsToClient(player);
                QuestPacketHandler.handleDialogResponse(player, packet, sendQuests);
            });
        });
        
        // 注册接受委托处理器
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.ACCEPT_QUEST, (server, player, handler, buf, responseSender) -> {
            AcceptQuestPacket packet = AcceptQuestPacket.decode(buf);
            server.execute(() -> QuestPacketHandler.handleAcceptQuest(player, packet, 
                    (p, syncPacket) -> sendQuestInstance(p, syncPacket.instance())));
        });
        
        // 注册提交委托处理器
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.TURN_IN_QUEST, (server, player, handler, buf, responseSender) -> {
            TurnInQuestPacket packet = TurnInQuestPacket.decode(buf);
            server.execute(() -> QuestPacketHandler.handleTurnInQuest(player, packet));
        });
        
        // 注册请求委托进度处理器
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.REQUEST_QUEST_PROGRESS, (server, player, handler, buf, responseSender) -> {
            RequestQuestProgressPacket packet = RequestQuestProgressPacket.decode(buf);
            server.execute(() -> QuestPacketHandler.handleRequestQuestProgress(player, packet, 
                    (p, syncPacket) -> {
                        FriendlyByteBuf syncBuf = PacketByteBufs.create();
                        syncPacket.encode(syncBuf);
                        ServerPlayNetworking.send(p, NetworkHandler.SYNC_QUEST_INSTANCE, syncBuf);
                    }));
        });
        
        initializeCallbacks();
        registerShopReceivers();
    }
    
    public static void sendReputationLevels(ServerPlayer player, Collection<net.shiroha233.roadweaverpg.reputation.ReputationLevel> levels) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncReputationLevelsPacket(levels).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_REPUTATION_LEVELS, buf);
    }

    public static void sendPlayerReputation(ServerPlayer player, java.util.Map<ResourceLocation, Integer> reputations, java.util.Map<ResourceLocation, Integer> levels) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        // 将 ResourceLocation 转为 String 进行网络传输，因为 SyncPlayerReputationPacket 接收 Map<String, Integer>
        java.util.Map<String, Integer> repStrings = new java.util.HashMap<>();
        reputations.forEach((k, v) -> repStrings.put(k.toString(), v));
        java.util.Map<String, Integer> levelStrings = new java.util.HashMap<>();
        levels.forEach((k, v) -> levelStrings.put(k.toString(), v));
        
        new SyncPlayerReputationPacket(repStrings, levelStrings).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_PLAYER_REPUTATION, buf);
    }

    public static void sendOpenReputationGui(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new OpenReputationGuiPacket().encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_REPUTATION_GUI, buf);
    }
    
    /** 初始化 PlayerQuestService 的回调 */
    private static void initializeCallbacks() {
        PlayerQuestService manager = PlayerQuestService.getInstance();
        manager.setOnQuestUpdated(NetworkHandlerFabric::sendQuestInstance);
        manager.setOnQuestCompleted(NetworkHandlerFabric::sendQuestInstance);
        manager.setOnSyncAllQuests(NetworkHandlerFabric::sendAllQuestInstances);
        manager.setOnSyncAllDefinitions(NetworkHandlerFabric::sendAllDefinitions);

        // 初始化声望同步回调
        manager.setOnSyncReputation((player, data) -> 
                sendPlayerReputation(player, data.getAllReputationXp(), data.getAllReputationLevels()));
        
        net.shiroha233.roadweaverpg.reputation.ReputationManager.setSyncCallback(
                NetworkHandlerFabric::sendReputationLevels);
        net.shiroha233.roadweaverpg.network.QuestPacketHandler.setOnOpenReputationGui(
                NetworkHandlerFabric::sendOpenReputationGui);
    }
    
    public static void sendAllDefinitions(ServerPlayer player, Collection<QuestDefinition> definitions) {
        SyncQuestsPacket packet = new SyncQuestsPacket(new java.util.ArrayList<>(definitions));
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_QUESTS, buf);
    }
    
    public static void sendQuestsToClient(ServerPlayer player) {
        List<QuestDefinition> quests = PlayerQuestService.getInstance().getAvailableQuests(player);
        SyncQuestsPacket packet = new SyncQuestsPacket(quests);
        
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_QUESTS, buf);
        
        FriendlyByteBuf openBuf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_QUEST_BOARD, openBuf);
    }
    
    public static void sendQuestInstance(ServerPlayer player, QuestInstance instance) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncQuestInstancePacket(instance).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_QUEST_INSTANCE, buf);
    }
    
    public static void sendAllQuestInstances(ServerPlayer player, Collection<QuestInstance> instances) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        SyncAllQuestInstancesPacket.fromCollection(instances).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_ALL_QUEST_INSTANCES, buf);
    }
    
    public static void sendOpenDialog(ServerPlayer player, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new OpenDialogPacket(entityId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_DIALOG, buf);
    }
    
    // ==================== 商店系统网络方法 ====================
    
    public static void sendOpenShopDialog(ServerPlayer player, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new OpenDialogPacket(entityId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_SHOP_DIALOG, buf);
    }
    
    public static void sendOpenShop(ServerPlayer player, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        net.shiroha233.roadweaverpg.network.ShopPacketHandler.createOpenShopPacket(player, entityId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_SHOP, buf);
    }
    
    public static void sendSyncCoins(ServerPlayer player, int coins) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncCoinsPacket(coins).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_COINS, buf);
    }
    
    /** 注册商店相关的服务端接收器 */
    public static void registerShopReceivers() {
        // 商店对话响应
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.SHOP_DIALOG_RESPONSE, (server, player, handler, buf, responseSender) -> {
            ShopDialogResponsePacket packet = ShopDialogResponsePacket.decode(buf);
            server.execute(() -> net.shiroha233.roadweaverpg.network.ShopPacketHandler.handleShopDialogResponse(
                    player, packet, p -> sendOpenShop(p, packet.entityId())));
        });
        
        // 商店购买请求
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.SHOP_PURCHASE, (server, player, handler, buf, responseSender) -> {
            ShopPurchasePacket packet = ShopPurchasePacket.decode(buf);
            server.execute(() -> net.shiroha233.roadweaverpg.network.ShopPacketHandler.handlePurchase(
                    player, packet, (p, syncPacket) -> sendSyncCoins(p, syncPacket.coins())));
        });
    }
}
