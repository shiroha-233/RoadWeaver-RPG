package net.shiroha233.roadweaverpg.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.DialogManager;
import net.shiroha233.roadweaverpg.init.QuestSystemInitializer;
import net.shiroha233.roadweaverpg.network.packet.quest.*;
import net.shiroha233.roadweaverpg.network.packet.sync.*;
import net.shiroha233.roadweaverpg.network.packet.ui.*;
import net.shiroha233.roadweaverpg.network.packet.shop.*;
import net.shiroha233.roadweaverpg.network.packet.dialog.*;
import net.shiroha233.roadweaverpg.network.packet.interaction.*;
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
        registerInteractionReceivers();
        registerDialogReceivers();
    }
    
    /**
     * 注册对话系统相关的服务端接收器
     */
    public static void registerDialogReceivers() {
        // 处理对话选择（带版本验证）
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.DIALOG_CHOICE, (server, player, handler, buf, responseSender) -> {
            DialogChoicePacket packet = DialogChoicePacket.decode(buf);
            server.execute(() -> DialogManager.getInstance().handleChoice(player, packet.choiceId(), packet.syncVersion()));
        });
        
        // 处理对话推进
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.DIALOG_ADVANCE, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> DialogManager.getInstance().advanceDialog(player));
        });
    }
    
    /**
     * 注册交互菜单相关的服务端接收器
     */
    public static void registerInteractionReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.INTERACTION_SELECT, (server, player, handler, buf, responseSender) -> {
            InteractionSelectPacket packet = InteractionSelectPacket.decode(buf);
            server.execute(() -> {
                var entries = net.shiroha233.roadweaverpg.interaction.NPCInteractionRegistry.getEntries(
                        getEntityNPCType(player, packet.npcEntityId()));
                entries.stream()
                        .filter(e -> e.id().equals(packet.entryId()))
                        .findFirst()
                        .ifPresent(entry -> net.shiroha233.roadweaverpg.interaction.NPCInteractionHandler
                                .handleInteraction(player, packet.npcEntityId(), entry));
            });
        });
    }
    
    /**
     * 获取实体的NPC类型
     */
    private static String getEntityNPCType(ServerPlayer player, int entityId) {
        var entity = player.level().getEntity(entityId);
        if (entity instanceof net.shiroha233.roadweaverpg.entity.npc.INPCEntity npc) {
            return npc.getNPCType().getId();
        }
        return "";
    }
    
    /**
     * 发送打开交互菜单数据包
     */
    public static void sendOpenInteractionMenu(ServerPlayer player, int npcEntityId, 
            List<net.shiroha233.roadweaverpg.interaction.NPCInteractionEntry> entries) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new OpenInteractionMenuPacket(npcEntityId, entries).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_INTERACTION_MENU, buf);
    }
    
    /**
     * 发送委托地图标点到客户端
     */
    public static void sendSyncQuestMarkers(ServerPlayer player, SyncQuestMarkersPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_QUEST_MARKERS, buf);
    }
    
    public static void sendReputationLevels(ServerPlayer player, Collection<net.shiroha233.roadweaverpg.reputation.ReputationLevel> levels) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncReputationLevelsPacket(levels).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_REPUTATION_LEVELS, buf);
    }

    public static void sendPlayerReputation(ServerPlayer player, java.util.Map<ResourceLocation, Integer> reputations, java.util.Map<ResourceLocation, Integer> levels) {
        FriendlyByteBuf buf = PacketByteBufs.create();
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
    
    // ==================== 冒险等级系统网络方法 ====================
    
    public static void sendAdventureLevels(ServerPlayer player, Collection<net.shiroha233.roadweaverpg.adventure.AdventureLevel> levels) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncAdventureLevelsPacket(levels).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_ADVENTURE_LEVELS, buf);
    }
    
    public static void sendPlayerAdventure(ServerPlayer player, int exp, int level) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncPlayerAdventurePacket(exp, level).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_PLAYER_ADVENTURE, buf);
    }
    
    /** 初始化回调 */
    private static void initializeCallbacks() {
        PlayerQuestService manager = PlayerQuestService.getInstance();
        manager.setOnQuestUpdated(NetworkHandlerFabric::sendQuestInstance);
        manager.setOnQuestCompleted(NetworkHandlerFabric::sendQuestInstance);
        manager.setOnSyncAllQuests(NetworkHandlerFabric::sendAllQuestInstances);
        manager.setOnSyncAllDefinitions(NetworkHandlerFabric::sendAllDefinitions);
        manager.setOnSyncDailyQuests(NetworkHandlerFabric::sendDailyQuests);

        manager.setOnSyncReputation((player, data) -> 
                sendPlayerReputation(player, data.getAllReputationXp(), data.getAllReputationLevels()));
        
        net.shiroha233.roadweaverpg.reputation.ReputationManager.setSyncCallback(
                NetworkHandlerFabric::sendReputationLevels);
        QuestPacketHandler.setOnOpenReputationGui(NetworkHandlerFabric::sendOpenReputationGui);
        
        // 初始化冒险等级系统回调
        net.shiroha233.roadweaverpg.adventure.AdventureLevelManager.setSyncCallback(
                NetworkHandlerFabric::sendAdventureLevels);
        net.shiroha233.roadweaverpg.adventure.AdventureDataService.getInstance().setOnSyncAdventure(
                (player, data) -> sendPlayerAdventure(player, data.getAdventureExp(), data.getAdventureLevel()));
        
        // 初始化对话系统回调
        initializeDialogCallbacks();
        
        // 初始化交互系统回调
        QuestSystemInitializer.setOnShowQuestsBoard((player, quests) -> {
            SyncQuestsPacket packet = new SyncQuestsPacket(quests);
            FriendlyByteBuf buf = PacketByteBufs.create();
            packet.encode(buf);
            ServerPlayNetworking.send(player, NetworkHandler.SYNC_QUESTS, buf);
            
            FriendlyByteBuf openBuf = PacketByteBufs.create();
            ServerPlayNetworking.send(player, NetworkHandler.OPEN_QUEST_BOARD, openBuf);
        });
        
        QuestSystemInitializer.setOnOpenShop(NetworkHandlerFabric::sendOpenShop);
    }
    
    /**
     * 初始化对话系统回调
     */
    private static void initializeDialogCallbacks() {
        // 初始化对话网络处理器（包含会话关闭发送器）
        QuestSystemInitializer.initializeDialogNetworkHandler(
                (player, data) -> sendDialogData(player, data.npcEntityId(), data.dialog(), data.syncVersion()),
                (player, data) -> sendDialogLine(player, data.npcEntityId(), data.line(), data.lineIndex()),
                (player, data) -> sendDialogChoices(player, data.dialogId(), data.choices()),
                NetworkHandlerFabric::sendDialogSessionClose
        );
        
        // 发送完整对话数据（带版本号）
        DialogManager.setDialogDataSender((player, npcId, dialog, visibleLines, syncVersion) -> 
                sendDialogData(player, npcId, dialog, syncVersion));
        
        // 发送单行对话
        DialogManager.setDialogLineSender((player, npcId, line, lineIndex) -> 
                sendDialogLine(player, npcId, line, lineIndex));
        
        // 发送对话选项
        DialogManager.setDialogChoicesSender((player, dialog, visibleChoices) -> 
                sendDialogChoices(player, dialog.id(), visibleChoices));
        
        // 发送会话无效通知
        DialogManager.setSessionInvalidSender(NetworkHandlerFabric::sendDialogSessionClose);
    }
    
    public static void sendDailyQuests(ServerPlayer player, List<ResourceLocation> dailyQuestIds) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        String refreshDate = net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager.getTodayDateString();
        int timeUntilRefresh = net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager.getInstance().getTimeUntilRefresh();
        new SyncDailyQuestsPacket(dailyQuestIds, refreshDate, timeUntilRefresh).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_DAILY_QUESTS, buf);
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
    
    /**
     * 发送对话数据到客户端（带版本号）
     */
    public static void sendDialogData(ServerPlayer player, int npcEntityId, DialogData dialog, long syncVersion) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new DialogDataPacket(npcEntityId, dialog, syncVersion).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.DIALOG_DATA, buf);
    }
    
    /**
     * 发送单行对话到客户端
     */
    public static void sendDialogLine(ServerPlayer player, int npcEntityId, DialogData.DialogLine line, int lineIndex) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new DialogLinePacket(npcEntityId, line, lineIndex).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.DIALOG_LINE, buf);
    }
    
    /**
     * 发送对话选项到客户端
     */
    public static void sendDialogChoices(ServerPlayer player, ResourceLocation dialogId, List<DialogData.DialogChoice> choices) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new DialogChoicesPacket(dialogId, choices).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.DIALOG_CHOICES, buf);
    }
    
    /**
     * 发送会话关闭通知
     */
    public static void sendDialogSessionClose(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        DialogSessionClosePacket.normal().encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.DIALOG_SESSION_CLOSE, buf);
    }
    
    // ==================== 商店系统网络方法 ====================
    
    public static void sendOpenShopDialog(ServerPlayer player, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new OpenDialogPacket(entityId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_SHOP_DIALOG, buf);
    }
    
    public static void sendOpenShop(ServerPlayer player, int entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ShopPacketHandler.createOpenShopPacket(player, entityId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_SHOP, buf);
    }
    
    public static void sendSyncCoins(ServerPlayer player, int coins) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncCoinsPacket(coins).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_COINS, buf);
    }
    
    /** 注册商店相关的服务端接收器 */
    public static void registerShopReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.SHOP_DIALOG_RESPONSE, (server, player, handler, buf, responseSender) -> {
            ShopDialogResponsePacket packet = ShopDialogResponsePacket.decode(buf);
            server.execute(() -> ShopPacketHandler.handleShopDialogResponse(
                    player, packet, p -> sendOpenShop(p, packet.entityId())));
        });
        
        ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.SHOP_PURCHASE, (server, player, handler, buf, responseSender) -> {
            ShopPurchasePacket packet = ShopPurchasePacket.decode(buf);
            server.execute(() -> ShopPacketHandler.handlePurchase(
                    player, packet, (p, syncPacket) -> sendSyncCoins(p, syncPacket.coins())));
        });
    }
}
