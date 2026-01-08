package net.shiroha233.roadweaverpg.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shiroha233.roadweaverpg.client.ClientQuestCache;
import net.shiroha233.roadweaverpg.client.ClientReputationCache;
import net.shiroha233.roadweaverpg.client.gui.reputation.ReputationOverviewScreen;
import net.shiroha233.roadweaverpg.client.gui.quest.QuestBoardScreen;
import net.shiroha233.roadweaverpg.client.gui.shop.ShopScreen;
import net.shiroha233.roadweaverpg.network.packet.quest.*;
import net.shiroha233.roadweaverpg.network.packet.sync.*;
import net.shiroha233.roadweaverpg.network.packet.ui.*;
import net.shiroha233.roadweaverpg.network.packet.shop.*;
import net.shiroha233.roadweaverpg.network.packet.interaction.*;

/**
 * Forge 客户端数据包处理器
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {
    
    public static void handleOpenDialog(OpenDialogPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        var entity = mc.level.getEntity(packet.entityId());
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc)) return;
        
        // 使用Galgame对话界面
        net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen screen = 
            net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogBuilder.create(packet.entityId())
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
                    sendDialogResponse(packet.entityId(), option);
                })
                .build();
        
        mc.setScreen(screen);
    }
    
    public static void handleSyncQuests(SyncQuestsPacket packet) {
        // 使用合并策略，不清空已有的定义缓存
        ClientQuestCache.setQuests(packet.quests());
    }
    
    public static void handleOpenQuestBoard() {
        int playerRepLevel = ClientReputationCache.getPlayerLevel(
                new ResourceLocation("roadweaver_rpg", "guild"));
        Minecraft.getInstance().setScreen(new QuestBoardScreen(
                ClientQuestCache.getQuests(),
                ClientPacketHandler::sendAcceptQuest,
                playerRepLevel
        ));
    }
    
    public static void handleSyncQuestInstance(SyncQuestInstancePacket packet) {
        if (packet.instance() == null) {
            ClientQuestCache.setCurrentInstance(null);
            return;
        }
        ClientQuestCache.setCurrentInstance(packet.instance());
        ClientQuestCache.cacheInstance(packet.instance());
        
        // 通知委托书处理器（用于延迟打开）
        net.shiroha233.roadweaverpg.forge.client.QuestScrollClientHandler.onInstanceSynced(packet.instance());
    }
    
    public static void handleSyncAllQuestInstances(SyncAllQuestInstancesPacket packet) {
        // 批量缓存所有实例
        ClientQuestCache.cacheAllInstances(packet.instances());
    }

    public static void handleSyncReputationLevels(SyncReputationLevelsPacket packet) {
        ClientReputationCache.setLevelDefinitions(packet.levels());
    }

    public static void handleSyncPlayerReputation(SyncPlayerReputationPacket packet) {
        ClientReputationCache.setPlayerData(packet.reputations(), packet.levels());
    }

    public static void handleOpenReputationGui() {
        Minecraft.getInstance().setScreen(new ReputationOverviewScreen());
    }
    
    public static void handleSyncDailyQuests(SyncDailyQuestsPacket packet) {
        net.shiroha233.roadweaverpg.client.data.ClientDailyQuestData.getInstance()
                .update(packet.dailyQuestIds(), packet.refreshDate(), packet.timeUntilRefresh());
    }
    
    // ==================== 冒险等级系统客户端处理 ====================
    
    public static void handleSyncAdventureLevels(SyncAdventureLevelsPacket packet) {
        net.shiroha233.roadweaverpg.client.ClientAdventureCache.setLevelDefinitions(packet.levels());
    }
    
    public static void handleSyncPlayerAdventure(SyncPlayerAdventurePacket packet) {
        net.shiroha233.roadweaverpg.client.ClientAdventureCache.setPlayerData(packet.exp(), packet.level());
    }
    
    // ==================== 商店系统客户端处理 ====================
    
    private static int currentShopCoins = 0;
    
    public static int getCurrentShopCoins() {
        return currentShopCoins;
    }
    
    public static void handleOpenShopDialog(OpenDialogPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        var entity = mc.level.getEntity(packet.entityId());
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc)) return;
        
        // 使用Galgame对话界面
        net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen screen = 
            net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogBuilder.create(packet.entityId())
                .npcName(npc.getDisplayName())
                .npcSaysTranslatable("npc.roadweaver_rpg.shop_maid.greeting")
                .addOptionTranslatable("gui.roadweaver_rpg.shop_dialog.open_shop", "open_shop")
                .onOptionSelected(index -> {
                    if (index == 0) {
                        sendShopDialogResponse(packet.entityId(), ShopDialogResponsePacket.ShopDialogOption.OPEN_SHOP);
                    }
                })
                .build();
        
        mc.setScreen(screen);
    }
    
    public static void handleOpenShop(OpenShopPacket packet) {
        currentShopCoins = packet.playerCoins();
        Minecraft.getInstance().setScreen(new ShopScreen(
                packet.entityId(),
                packet.items(),
                packet.playerCoins(),
                (itemId, quantity) -> sendShopPurchase(packet.entityId(), itemId, quantity)
        ));
    }
    
    public static void handleSyncCoins(SyncCoinsPacket packet) {
        currentShopCoins = packet.coins();
        if (Minecraft.getInstance().screen instanceof ShopScreen shopScreen) {
            shopScreen.updateCoins(packet.coins());
        }
    }
    
    public static void sendShopDialogResponse(int entityId, ShopDialogResponsePacket.ShopDialogOption option) {
        NetworkHandlerForge.CHANNEL.sendToServer(new ShopDialogResponsePacket(entityId, option));
    }
    
    public static void sendShopPurchase(int entityId, ResourceLocation itemId, int quantity) {
        NetworkHandlerForge.CHANNEL.sendToServer(new ShopPurchasePacket(entityId, itemId, quantity));
    }
    
    // ==================== 原有方法 ====================
    
    public static void sendDialogResponse(int entityId, DialogResponsePacket.DialogOption option) {
        NetworkHandlerForge.CHANNEL.sendToServer(new DialogResponsePacket(entityId, option));
    }
    
    public static void sendAcceptQuest(ResourceLocation questId) {
        NetworkHandlerForge.CHANNEL.sendToServer(new AcceptQuestPacket(questId));
    }
    
    public static void sendTurnInQuest(ResourceLocation questId, int entityId) {
        NetworkHandlerForge.CHANNEL.sendToServer(new TurnInQuestPacket(questId, entityId));
    }
    
    public static void sendRequestQuestProgress(ResourceLocation questId) {
        NetworkHandlerForge.CHANNEL.sendToServer(new RequestQuestProgressPacket(questId));
    }
    
    /** 发送请求委托进度（带instanceId精确匹配） */
    public static void sendRequestQuestProgress(ResourceLocation questId, java.util.UUID instanceId) {
        NetworkHandlerForge.CHANNEL.sendToServer(new RequestQuestProgressPacket(questId, instanceId));
    }
    
    // ==================== 交互菜单系统客户端处理 ====================
    
    public static void handleOpenInteractionMenu(OpenInteractionMenuPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        mc.setScreen(new net.shiroha233.roadweaverpg.client.gui.interaction.NPCInteractionScreen(
                packet.npcEntityId(),
                packet.entries(),
                entry -> sendInteractionSelect(packet.npcEntityId(), entry.id())
        ));
    }
    
    public static void sendInteractionSelect(int npcEntityId, String entryId) {
        NetworkHandlerForge.CHANNEL.sendToServer(new InteractionSelectPacket(npcEntityId, entryId));
    }
    
    // ==================== 对话系统客户端处理 ====================
    
    /**
     * 处理对话数据（带版本号）
     */
    public static void handleDialogData(net.shiroha233.roadweaverpg.network.packet.dialog.DialogDataPacket packet) {
        // 初始化对话回调（如果尚未初始化）
        net.shiroha233.roadweaverpg.dialog.client.ClientDialogHandler.init(
                ClientPacketHandler::sendDialogChoice,
                ClientPacketHandler::sendDialogAdvance
        );
        
        // 委托给公共处理器（包含版本号）
        net.shiroha233.roadweaverpg.dialog.client.ClientDialogHandler.handleDialogData(
                packet.npcEntityId(), packet.dialog(), packet.syncVersion());
    }
    
    /**
     * 处理单行对话
     */
    public static void handleDialogLine(net.shiroha233.roadweaverpg.network.packet.dialog.DialogLinePacket packet) {
        net.shiroha233.roadweaverpg.dialog.client.ClientDialogHandler.handleDialogLine(
                packet.npcEntityId(), packet.line(), packet.lineIndex());
    }
    
    /**
     * 处理对话选项
     */
    public static void handleDialogChoices(net.shiroha233.roadweaverpg.network.packet.dialog.DialogChoicesPacket packet) {
        net.shiroha233.roadweaverpg.dialog.client.ClientDialogHandler.handleDialogChoices(
                packet.dialogId(), packet.choices());
    }
    
    /**
     * 处理会话关闭
     */
    public static void handleDialogSessionClose(net.shiroha233.roadweaverpg.network.packet.dialog.DialogSessionClosePacket packet) {
        net.shiroha233.roadweaverpg.dialog.client.ClientDialogHandler.handleSessionClose(packet.reason());
    }
    
    /**
     * 发送对话选择到服务端（带版本号）
     */
    public static void sendDialogChoice(String choiceId) {
        long syncVersion = net.shiroha233.roadweaverpg.dialog.client.ClientDialogHandler.getCurrentSyncVersion();
        NetworkHandlerForge.CHANNEL.sendToServer(
                new net.shiroha233.roadweaverpg.network.packet.dialog.DialogChoicePacket(choiceId, syncVersion));
    }
    
    /**
     * 发送对话推进请求到服务端
     */
    public static void sendDialogAdvance() {
        NetworkHandlerForge.CHANNEL.sendToServer(
                new net.shiroha233.roadweaverpg.network.packet.dialog.DialogAdvancePacket());
    }
}
