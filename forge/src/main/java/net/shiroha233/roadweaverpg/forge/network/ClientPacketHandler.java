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
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

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
    }
    
    public static void handleSyncAllQuestInstances(SyncAllQuestInstancesPacket packet) {
        for (QuestInstance instance : packet.instances()) {
            ClientQuestCache.cacheInstance(instance);
        }
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
}
