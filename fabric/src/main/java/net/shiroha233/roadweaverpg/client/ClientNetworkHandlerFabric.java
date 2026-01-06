package net.shiroha233.roadweaverpg.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.client.gui.NPCDialogScreen;
import net.shiroha233.roadweaverpg.client.gui.QuestBoardScreen;
import net.shiroha233.roadweaverpg.client.gui.ShopDialogScreen;
import net.shiroha233.roadweaverpg.client.gui.ShopScreen;
import net.shiroha233.roadweaverpg.network.NetworkHandler;
import net.shiroha233.roadweaverpg.network.packet.*;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

/**
 * Fabric 客户端网络处理器
 */
@Environment(EnvType.CLIENT)
public class ClientNetworkHandlerFabric {
    
    private static int currentShopCoins = 0;
    
    public static int getCurrentShopCoins() {
        return currentShopCoins;
    }
    
    public static void registerClientReceivers() {
        // 处理打开对话界面
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.OPEN_DIALOG, (client, handler, buf, responseSender) -> {
            OpenDialogPacket packet = OpenDialogPacket.decode(buf);
            client.execute(() -> openDialogScreen(packet.entityId()));
        });
        
        // 处理同步委托数据
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_QUESTS, (client, handler, buf, responseSender) -> {
            SyncQuestsPacket packet = SyncQuestsPacket.decode(buf);
            client.execute(() -> ClientQuestCache.setQuests(packet.quests()));
        });
        
        // 处理打开委托看板
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.OPEN_QUEST_BOARD, (client, handler, buf, responseSender) -> {
            client.execute(ClientNetworkHandlerFabric::openQuestBoardScreen);
        });
        
        // 处理同步委托实例
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_QUEST_INSTANCE, (client, handler, buf, responseSender) -> {
            SyncQuestInstancePacket packet = SyncQuestInstancePacket.decode(buf);
            client.execute(() -> handleSyncQuestInstance(packet.instance()));
        });
        
        // 处理同步所有委托实例
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_ALL_QUEST_INSTANCES, (client, handler, buf, responseSender) -> {
            SyncAllQuestInstancesPacket packet = SyncAllQuestInstancesPacket.decode(buf);
            client.execute(() -> handleSyncAllQuestInstances(packet));
        });

        // 处理同步声望等级
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_REPUTATION_LEVELS, (client, handler, buf, responseSender) -> {
            SyncReputationLevelsPacket packet = SyncReputationLevelsPacket.decode(buf);
            client.execute(() -> ClientReputationCache.setLevelDefinitions(packet.levels()));
        });

        // 处理同步玩家声望
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_PLAYER_REPUTATION, (client, handler, buf, responseSender) -> {
            SyncPlayerReputationPacket packet = SyncPlayerReputationPacket.decode(buf);
            client.execute(() -> ClientReputationCache.setPlayerData(packet.reputations(), packet.levels()));
        });

        // 处理打开声望界面
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.OPEN_REPUTATION_GUI, (client, handler, buf, responseSender) -> {
            client.execute(() -> Minecraft.getInstance().setScreen(new net.shiroha233.roadweaverpg.client.gui.ReputationOverviewScreen()));
        });
        
        // 注册商店相关接收器
        registerShopReceivers();
    }
    
    // ==================== 商店系统客户端处理 ====================
    
    private static void registerShopReceivers() {
        // 处理打开商店对话界面
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.OPEN_SHOP_DIALOG, (client, handler, buf, responseSender) -> {
            OpenDialogPacket packet = OpenDialogPacket.decode(buf);
            client.execute(() -> openShopDialogScreen(packet.entityId()));
        });
        
        // 处理打开商店界面
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.OPEN_SHOP, (client, handler, buf, responseSender) -> {
            OpenShopPacket packet = OpenShopPacket.decode(buf);
            client.execute(() -> handleOpenShop(packet));
        });
        
        // 处理同步金币
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_COINS, (client, handler, buf, responseSender) -> {
            SyncCoinsPacket packet = SyncCoinsPacket.decode(buf);
            client.execute(() -> handleSyncCoins(packet));
        });
    }
    
    private static void openShopDialogScreen(int entityId) {
        Minecraft.getInstance().setScreen(new ShopDialogScreen(
                entityId, 
                ClientNetworkHandlerFabric::sendShopDialogResponse
        ));
    }
    
    private static void handleOpenShop(OpenShopPacket packet) {
        currentShopCoins = packet.playerCoins();
        Minecraft.getInstance().setScreen(new ShopScreen(
                packet.entityId(),
                packet.items(),
                packet.playerCoins(),
                (itemId, quantity) -> sendShopPurchase(packet.entityId(), itemId, quantity)
        ));
    }
    
    private static void handleSyncCoins(SyncCoinsPacket packet) {
        currentShopCoins = packet.coins();
        if (Minecraft.getInstance().screen instanceof ShopScreen shopScreen) {
            shopScreen.updateCoins(packet.coins());
        }
    }
    
    public static void sendShopDialogResponse(int entityId, ShopDialogResponsePacket.ShopDialogOption option) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new ShopDialogResponsePacket(entityId, option).encode(buf);
        ClientPlayNetworking.send(NetworkHandler.SHOP_DIALOG_RESPONSE, buf);
    }
    
    public static void sendShopPurchase(int entityId, ResourceLocation itemId, int quantity) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new ShopPurchasePacket(entityId, itemId, quantity).encode(buf);
        ClientPlayNetworking.send(NetworkHandler.SHOP_PURCHASE, buf);
    }
    
    // ==================== 原有方法 ====================
    
    private static void openDialogScreen(int entityId) {
        Minecraft.getInstance().setScreen(new NPCDialogScreen(entityId, ClientNetworkHandlerFabric::sendDialogResponse));
    }
    
    private static void openQuestBoardScreen() {
        Minecraft.getInstance().setScreen(new QuestBoardScreen(
                ClientQuestCache.getQuests(),
                ClientNetworkHandlerFabric::sendAcceptQuest
        ));
    }
    
    private static void handleSyncQuestInstance(QuestInstance instance) {
        if (instance == null) {
            ClientQuestCache.setCurrentInstance(null);
            return;
        }
        ClientQuestCache.setCurrentInstance(instance);
        ClientQuestCache.cacheInstance(instance);
    }
    
    private static void handleSyncAllQuestInstances(SyncAllQuestInstancesPacket packet) {
        for (QuestInstance instance : packet.instances()) {
            ClientQuestCache.cacheInstance(instance);
        }
    }
    
    public static void sendDialogResponse(int entityId, DialogResponsePacket.DialogOption option) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new DialogResponsePacket(entityId, option).encode(buf);
        ClientPlayNetworking.send(NetworkHandler.DIALOG_RESPONSE, buf);
    }
    
    public static void sendAcceptQuest(ResourceLocation questId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new AcceptQuestPacket(questId).encode(buf);
        ClientPlayNetworking.send(NetworkHandler.ACCEPT_QUEST, buf);
    }
    
    public static void sendTurnInQuest(ResourceLocation questId, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new TurnInQuestPacket(questId, entityId).encode(buf);
        ClientPlayNetworking.send(NetworkHandler.TURN_IN_QUEST, buf);
    }
    
    public static void sendRequestQuestProgress(ResourceLocation questId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new RequestQuestProgressPacket(questId).encode(buf);
        ClientPlayNetworking.send(NetworkHandler.REQUEST_QUEST_PROGRESS, buf);
    }
}
