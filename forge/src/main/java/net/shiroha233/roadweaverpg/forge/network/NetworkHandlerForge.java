package net.shiroha233.roadweaverpg.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.DialogManager;
import net.shiroha233.roadweaverpg.init.QuestSystemInitializer;
import net.shiroha233.roadweaverpg.network.QuestPacketHandler;
import net.shiroha233.roadweaverpg.network.ShopPacketHandler;
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
        
        // 服务端 -> 客户端：同步每日委托
        CHANNEL.registerMessage(packetId++, SyncDailyQuestsPacket.class,
                SyncDailyQuestsPacket::encode, SyncDailyQuestsPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncDailyQuests(packet));
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
        registerWalletPackets();
        registerShopPackets();
        registerInteractionPackets();
        registerDialogPackets();
        registerAdventurePackets();
    }
    
    /**
     * 注册冒险等级系统相关的网络包
     */
    public static void registerAdventurePackets() {
        // 服务端 -> 客户端：同步冒险等级定义
        CHANNEL.registerMessage(packetId++, SyncAdventureLevelsPacket.class,
                SyncAdventureLevelsPacket::encode, SyncAdventureLevelsPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncAdventureLevels(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：同步玩家冒险数据
        CHANNEL.registerMessage(packetId++, SyncPlayerAdventurePacket.class,
                SyncPlayerAdventurePacket::encode, SyncPlayerAdventurePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncPlayerAdventure(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：打开冒险等级界面
        CHANNEL.registerMessage(packetId++, OpenAdventureLevelGuiPacket.class,
                OpenAdventureLevelGuiPacket::encode, OpenAdventureLevelGuiPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleOpenAdventureLevelGui());
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    
    /**
     * 注册对话系统相关的网络包
     */
    public static void registerDialogPackets() {
        // 服务端 -> 客户端：对话数据
        CHANNEL.registerMessage(packetId++, DialogDataPacket.class,
                DialogDataPacket::encode, DialogDataPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleDialogData(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：单行对话
        CHANNEL.registerMessage(packetId++, DialogLinePacket.class,
                DialogLinePacket::encode, DialogLinePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleDialogLine(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：对话选项
        CHANNEL.registerMessage(packetId++, DialogChoicesPacket.class,
                DialogChoicesPacket::encode, DialogChoicesPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleDialogChoices(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 服务端 -> 客户端：会话关闭
        CHANNEL.registerMessage(packetId++, DialogSessionClosePacket.class,
                DialogSessionClosePacket::encode, DialogSessionClosePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleDialogSessionClose(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 客户端 -> 服务端：对话选择（带版本验证）
        CHANNEL.registerMessage(packetId++, DialogChoicePacket.class,
                DialogChoicePacket::encode, DialogChoicePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            DialogManager.getInstance().handleChoice(player, packet.choiceId(), packet.syncVersion());
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        // 客户端 -> 服务端：对话推进
        CHANNEL.registerMessage(packetId++, DialogAdvancePacket.class,
                DialogAdvancePacket::encode, DialogAdvancePacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            DialogManager.getInstance().advanceDialog(player);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    
    /**
     * 注册交互菜单相关的网络包
     */
    public static void registerInteractionPackets() {
        // 服务端 -> 客户端：打开交互菜单
        CHANNEL.registerMessage(packetId++, OpenInteractionMenuPacket.class,
                OpenInteractionMenuPacket::encode, OpenInteractionMenuPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleOpenInteractionMenu(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 客户端 -> 服务端：交互选择
        CHANNEL.registerMessage(packetId++, InteractionSelectPacket.class,
                InteractionSelectPacket::encode, InteractionSelectPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            handleInteractionSelect(player, packet);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    
    /**
     * 处理交互选择
     */
    private static void handleInteractionSelect(ServerPlayer player, InteractionSelectPacket packet) {
        var entity = player.level().getEntity(packet.npcEntityId());
        if (entity instanceof net.shiroha233.roadweaverpg.entity.npc.INPCEntity npc) {
            var entries = net.shiroha233.roadweaverpg.interaction.NPCInteractionRegistry.getEntries(npc);
            entries.stream()
                    .filter(e -> e.id().equals(packet.entryId()))
                    .findFirst()
                    .ifPresent(entry -> net.shiroha233.roadweaverpg.interaction.NPCInteractionHandler
                            .handleInteraction(player, packet.npcEntityId(), entry));
        }
    }
    
    /**
     * 发送打开交互菜单数据包
     */
    public static void sendOpenInteractionMenu(ServerPlayer player, int npcEntityId, 
            List<net.shiroha233.roadweaverpg.interaction.NPCInteractionEntry> entries) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                new OpenInteractionMenuPacket(npcEntityId, entries));
    }
    
    /** 初始化回调 */
    private static void initializeCallbacks() {
        PlayerQuestService manager = PlayerQuestService.getInstance();
        manager.setOnQuestUpdated((player, instance) -> sendQuestInstance(player, new SyncQuestInstancePacket(instance)));
        manager.setOnQuestCompleted((player, instance) -> sendQuestInstance(player, new SyncQuestInstancePacket(instance)));
        manager.setOnSyncAllQuests(NetworkHandlerForge::sendAllQuestInstances);
        manager.setOnSyncAllDefinitions(NetworkHandlerForge::sendAllDefinitions);
        manager.setOnSyncDailyQuests(NetworkHandlerForge::sendDailyQuests);

        manager.setOnSyncReputation((player, data) -> 
                sendPlayerReputation(player, data.getAllReputationXp(), data.getAllReputationLevels()));
        
        net.shiroha233.roadweaverpg.reputation.ReputationManager.setSyncCallback(
                NetworkHandlerForge::sendReputationLevels);
        QuestPacketHandler.setOnOpenReputationGui(NetworkHandlerForge::sendOpenReputationGui);
        
        // 初始化冒险等级系统回调
        net.shiroha233.roadweaverpg.adventure.AdventureLevelManager.setSyncCallback(
                NetworkHandlerForge::sendAdventureLevels);
        net.shiroha233.roadweaverpg.adventure.AdventureDataService.getInstance().setOnSyncAdventure(
                (player, data) -> sendPlayerAdventure(player, data.getAdventureExp(), data.getAdventureLevel()));
        QuestPacketHandler.setOnOpenAdventureLevelGui(NetworkHandlerForge::sendOpenAdventureLevelGui);
        
        // 初始化对话系统回调
        initializeDialogCallbacks();
        
        // 初始化交互系统回调
        QuestSystemInitializer.setOnShowQuestsBoard((player, quests) -> {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncQuestsPacket(quests));
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenQuestBoardPacket());
        });
        
        QuestSystemInitializer.setOnOpenShop(NetworkHandlerForge::sendOpenShop);
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
                NetworkHandlerForge::sendDialogSessionClose
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
        DialogManager.setSessionInvalidSender(NetworkHandlerForge::sendDialogSessionClose);
    }
    
    public static void sendDailyQuests(ServerPlayer player, List<ResourceLocation> dailyQuestIds) {
        String refreshDate = net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager.getTodayDateString();
        int timeUntilRefresh = net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager.getInstance().getTimeUntilRefresh();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                new SyncDailyQuestsPacket(dailyQuestIds, refreshDate, timeUntilRefresh));
    }

    public static void sendReputationLevels(ServerPlayer player, Collection<net.shiroha233.roadweaverpg.reputation.ReputationLevel> levels) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncReputationLevelsPacket(levels));
    }

    public static void sendPlayerReputation(ServerPlayer player, java.util.Map<ResourceLocation, Integer> reputations, java.util.Map<ResourceLocation, Integer> levels) {
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
    
    // ==================== 冒险等级系统网络方法 ====================
    
    public static void sendAdventureLevels(ServerPlayer player, Collection<net.shiroha233.roadweaverpg.adventure.AdventureLevel> levels) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAdventureLevelsPacket(levels));
    }
    
    public static void sendPlayerAdventure(ServerPlayer player, int exp, int level) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerAdventurePacket(exp, level));
    }
    
    public static void sendOpenAdventureLevelGui(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenAdventureLevelGuiPacket());
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
    
    /**
     * 发送委托地图标点到客户端
     */
    public static void sendSyncQuestMarkers(ServerPlayer player, SyncQuestMarkersPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    /**
     * 发送对话数据到客户端（带版本号）
     */
    public static void sendDialogData(ServerPlayer player, int npcEntityId, DialogData dialog, long syncVersion) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DialogDataPacket(npcEntityId, dialog, syncVersion));
    }
    
    /**
     * 发送单行对话到客户端
     */
    public static void sendDialogLine(ServerPlayer player, int npcEntityId, DialogData.DialogLine line, int lineIndex) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DialogLinePacket(npcEntityId, line, lineIndex));
    }
    
    /**
     * 发送对话选项到客户端
     */
    public static void sendDialogChoices(ServerPlayer player, ResourceLocation dialogId, List<DialogData.DialogChoice> choices) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DialogChoicesPacket(dialogId, choices));
    }
    
    /**
     * 发送会话关闭通知
     */
    public static void sendDialogSessionClose(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), DialogSessionClosePacket.normal());
    }
    
    // ==================== 商店系统网络方法 ====================
    
    public static void sendOpenShopDialog(ServerPlayer player, int entityId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenDialogPacket(entityId));
    }
    
    public static void sendOpenShop(ServerPlayer player, int entityId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                ShopPacketHandler.createOpenShopPacket(player, entityId));
    }
    
    public static void sendSyncCoins(ServerPlayer player, int coins) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCoinsPacket(coins));
    }
    
    // ==================== 钱包系统网络方法 ====================
    
    public static void sendSyncWallet(ServerPlayer player, long coins, long addedAmount) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), 
                new net.shiroha233.roadweaverpg.network.packet.wallet.SyncWalletPacket(coins, addedAmount));
    }
    
    public static void sendSyncWallet(ServerPlayer player, long coins) {
        sendSyncWallet(player, coins, 0);
    }
    
    /** 注册钱包相关的网络包 */
    public static void registerWalletPackets() {
        // 服务端 -> 客户端：同步钱包
        CHANNEL.registerMessage(packetId++, net.shiroha233.roadweaverpg.network.packet.wallet.SyncWalletPacket.class,
                net.shiroha233.roadweaverpg.network.packet.wallet.SyncWalletPacket::encode,
                net.shiroha233.roadweaverpg.network.packet.wallet.SyncWalletPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientPacketHandler.handleSyncWallet(packet));
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        // 客户端 -> 服务端：存入金币
        CHANNEL.registerMessage(packetId++, net.shiroha233.roadweaverpg.network.packet.wallet.DepositCoinsPacket.class,
                net.shiroha233.roadweaverpg.network.packet.wallet.DepositCoinsPacket::encode,
                net.shiroha233.roadweaverpg.network.packet.wallet.DepositCoinsPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer player = ctx.get().getSender();
                        if (player != null) {
                            long deposited = net.shiroha233.roadweaverpg.wallet.WalletService.depositCoinsFromInventory(player);
                            if (deposited > 0) {
                                long total = net.shiroha233.roadweaverpg.wallet.WalletService.getCoins(player);
                                sendSyncWallet(player, total, deposited);
                            }
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    
    /** 注册商店相关的网络包 */
    public static void registerShopPackets() {
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
                            ShopPacketHandler.handleShopDialogResponse(
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
                            ShopPacketHandler.handlePurchase(
                                    player, packet, (p, syncPacket) -> sendSyncCoins(p, syncPacket.coins()));
                        }
                    });
                    ctx.get().setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
