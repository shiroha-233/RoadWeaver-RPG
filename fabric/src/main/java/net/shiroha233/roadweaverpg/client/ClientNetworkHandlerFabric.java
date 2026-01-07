package net.shiroha233.roadweaverpg.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.client.gui.quest.QuestBoardScreen;
import net.shiroha233.roadweaverpg.client.gui.shop.ShopScreen;
import net.shiroha233.roadweaverpg.network.NetworkHandler;
import net.shiroha233.roadweaverpg.network.packet.quest.*;
import net.shiroha233.roadweaverpg.network.packet.sync.*;
import net.shiroha233.roadweaverpg.network.packet.ui.*;
import net.shiroha233.roadweaverpg.network.packet.shop.*;
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
            client.execute(() -> Minecraft.getInstance().setScreen(new net.shiroha233.roadweaverpg.client.gui.reputation.ReputationOverviewScreen()));
        });
        
        // 处理同步每日委托
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_DAILY_QUESTS, (client, handler, buf, responseSender) -> {
            SyncDailyQuestsPacket packet = SyncDailyQuestsPacket.decode(buf);
            client.execute(() -> net.shiroha233.roadweaverpg.client.data.ClientDailyQuestData.getInstance()
                    .update(packet.dailyQuestIds(), packet.refreshDate(), packet.timeUntilRefresh()));
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        var entity = mc.level.getEntity(entityId);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc)) return;
        
        // 使用Galgame对话界面
        net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen screen = 
            net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogBuilder.create(entityId)
                .npcName(npc.getDisplayName())
                .npcSaysTranslatable("npc.roadweaver_rpg.shop_maid.greeting")
                .addOptionTranslatable("gui.roadweaver_rpg.shop_dialog.open_shop", "open_shop")
                .onOptionSelected(index -> {
                    if (index == 0) {
                        sendShopDialogResponse(entityId, ShopDialogResponsePacket.ShopDialogOption.OPEN_SHOP);
                    }
                })
                .build();
        
        mc.setScreen(screen);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        var entity = mc.level.getEntity(entityId);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc)) return;
        
        // 使用Galgame对话界面
        net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen screen = 
            net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogBuilder.create(entityId)
                .npcName(npc.getDisplayName())
                .npcSaysTranslatable("npc.roadweaver_rpg.guild_maid.greeting")
                .addOptionTranslatable("gui.roadweaver_rpg.dialog.show_quests", "show_quests")
                .addOptionTranslatable("gui.roadweaver_rpg.dialog.complete_quest", "complete_quest")
                .addOptionTranslatable("gui.roadweaver_rpg.dialog.retrieve_scroll", "retrieve_scroll")
                .addOptionTranslatable("gui.roadweaver_rpg.dialog.view_reputation", "view_reputation")
                .onOptionSelected(index -> {
                    // 根据索引发送对应的响应
                    DialogResponsePacket.DialogOption option = switch (index) {
                        case 0 -> DialogResponsePacket.DialogOption.SHOW_QUESTS;
                        case 1 -> DialogResponsePacket.DialogOption.COMPLETE_QUEST;
                        case 2 -> DialogResponsePacket.DialogOption.RETRIEVE_SCROLL;
                        case 3 -> DialogResponsePacket.DialogOption.VIEW_REPUTATION;
                        default -> DialogResponsePacket.DialogOption.SHOW_QUESTS;
                    };
                    sendDialogResponse(entityId, option);
                })
                .build();
        
        mc.setScreen(screen);
    }
    
    private static void openQuestBoardScreen() {
        int playerRepLevel = ClientReputationCache.getPlayerLevel(
                new ResourceLocation("roadweaver_rpg", "guild"));
        Minecraft.getInstance().setScreen(new QuestBoardScreen(
                ClientQuestCache.getQuests(),
                ClientNetworkHandlerFabric::sendAcceptQuest,
                playerRepLevel
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
