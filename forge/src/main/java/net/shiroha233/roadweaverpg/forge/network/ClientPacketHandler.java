package net.shiroha233.roadweaverpg.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shiroha233.roadweaverpg.client.ClientQuestCache;
import net.shiroha233.roadweaverpg.client.ClientReputationCache;
import net.shiroha233.roadweaverpg.client.gui.ReputationOverviewScreen;
import net.shiroha233.roadweaverpg.client.gui.NPCDialogScreen;
import net.shiroha233.roadweaverpg.client.gui.QuestBoardScreen;
import net.shiroha233.roadweaverpg.client.gui.ShopDialogScreen;
import net.shiroha233.roadweaverpg.client.gui.ShopScreen;
import net.shiroha233.roadweaverpg.network.packet.*;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

/**
 * Forge 客户端数据包处理器
 */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {
    
    public static void handleOpenDialog(OpenDialogPacket packet) {
        Minecraft.getInstance().setScreen(new NPCDialogScreen(
                packet.entityId(),
                ClientPacketHandler::sendDialogResponse
        ));
    }
    
    public static void handleSyncQuests(SyncQuestsPacket packet) {
        ClientQuestCache.setQuests(packet.quests());
    }
    
    public static void handleOpenQuestBoard() {
        Minecraft.getInstance().setScreen(new QuestBoardScreen(
                ClientQuestCache.getQuests(),
                ClientPacketHandler::sendAcceptQuest
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
    
    // ==================== 商店系统客户端处理 ====================
    
    private static int currentShopCoins = 0;
    
    public static int getCurrentShopCoins() {
        return currentShopCoins;
    }
    
    public static void handleOpenShopDialog(OpenDialogPacket packet) {
        Minecraft.getInstance().setScreen(new ShopDialogScreen(
                packet.entityId(),
                ClientPacketHandler::sendShopDialogResponse
        ));
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
