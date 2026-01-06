package net.shiroha233.roadweaverpg.network;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.network.packet.*;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;

import java.util.Optional;

/**
 * 委托数据包处理器 - 公共逻辑
 * 供 Fabric/Forge 网络处理器调用，避免代码重复
 */
public final class QuestPacketHandler {
    
    private QuestPacketHandler() {}
    
    /**
     * 处理对话响应
     * @param sendQuests 发送委托列表的回调
     */
    public static void handleDialogResponse(ServerPlayer player, DialogResponsePacket packet,
                                            Runnable sendQuests) {
        Entity entity = player.level().getEntity(packet.entityId());
        if (!(entity instanceof GuildMaidEntity)) return;
        
        switch (packet.option()) {
            case WHO_ARE_YOU -> player.displayClientMessage(
                    Component.translatable("gui.roadweaver_rpg.dialog.response.who_are_you"), false);
            case SHOW_QUESTS -> sendQuests.run();
            case COMPLETE_QUEST -> handleQuestTurnIn(player);
            case VIEW_REPUTATION -> handleViewReputation(player);
            case RETRIEVE_SCROLL -> handleRetrieveScroll(player);
        }
    }
    
    private static void handleRetrieveScroll(ServerPlayer player) {
        PlayerQuestService.getInstance().retrieveLostScrolls(player);
    }
    
    private static void handleViewReputation(ServerPlayer player) {
        // 同步最新的声望等级定义（以防万一）
        net.shiroha233.roadweaverpg.quest.event.QuestEventHandler.syncReputationDefinitions(player);
        // 同步玩家声望数据
        net.shiroha233.roadweaverpg.quest.event.QuestEventHandler.syncPlayerReputation(player);
        
        // 触发打开 GUI 的回调（由平台实现）
        if (onOpenReputationGui != null) {
            onOpenReputationGui.accept(player);
        }
    }

    private static java.util.function.Consumer<ServerPlayer> onOpenReputationGui;

    public static void setOnOpenReputationGui(java.util.function.Consumer<ServerPlayer> callback) {
        onOpenReputationGui = callback;
    }
    
    /**
     * 处理接受委托
     * @param syncPacket 同步数据包的回调
     */
    public static void handleAcceptQuest(ServerPlayer player, AcceptQuestPacket packet,
                                         java.util.function.BiConsumer<ServerPlayer, SyncQuestInstancePacket> syncPacket) {
        PlayerQuestService.getInstance().acceptQuest(player, packet.questId()).ifPresent(instance -> {
            QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(packet.questId());
            if (def != null) {
                player.displayClientMessage(
                        Component.translatable("gui.roadweaver_rpg.quest.accepted", def.getTitle()), false);
                syncPacket.accept(player, new SyncQuestInstancePacket(instance));
            }
        });
    }
    
    /**
     * 处理提交委托（通过委托书）
     */
    public static void handleTurnInQuest(ServerPlayer player, TurnInQuestPacket packet) {
        Entity entity = player.level().getEntity(packet.entityId());
        if (!(entity instanceof GuildMaidEntity)) return;
        
        ItemStack scroll = findQuestScroll(player, packet.questId());
        if (scroll.isEmpty()) return;
        
        if (PlayerQuestService.getInstance().turnInQuestByScroll(player, scroll)) {
            QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(packet.questId());
            if (def != null) {
                player.displayClientMessage(
                        Component.translatable("gui.roadweaver_rpg.quest.turned_in", def.getTitle()), false);
            }
        }
    }
    
    /**
     * 处理委托提交（通过对话选项，检查手持物品）
     */
    public static void handleQuestTurnIn(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof QuestScrollItem && QuestScrollItem.hasQuest(stack)) {
                if (QuestScrollItem.getQuestState(stack) == QuestState.COMPLETED) {
                    Optional<ResourceLocation> questId = QuestScrollItem.getQuestId(stack);
                    if (questId.isPresent() && PlayerQuestService.getInstance().turnInQuestByScroll(player, stack)) {
                        QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(questId.get());
                        if (def != null) {
                            player.displayClientMessage(
                                    Component.translatable("gui.roadweaver_rpg.quest.turned_in", def.getTitle()), false);
                        }
                        return;
                    }
                }
            }
        }
        
        player.displayClientMessage(
                Component.translatable("gui.roadweaver_rpg.dialog.response.no_completed_quest"), false);
    }
    
    /**
     * 处理请求委托进度
     * @param syncInstance 同步实例的回调
     */
    public static void handleRequestQuestProgress(ServerPlayer player, RequestQuestProgressPacket packet,
                                                   java.util.function.BiConsumer<ServerPlayer, SyncQuestInstancePacket> syncPacket) {
        PlayerQuestService manager = PlayerQuestService.getInstance();
        Optional<QuestInstance> instanceOpt = manager.getQuestInstance(player, packet.questId());
        
        if (instanceOpt.isPresent()) {
            syncPacket.accept(player, new SyncQuestInstancePacket(instanceOpt.get()));
        } else {
            // 如果服务端找不到实例，通知客户端清除该 questId 的缓存
            RoadWeaverRPG.LOGGER.warn("Player {} requested progress for non-existent quest: {}", 
                    player.getName().getString(), packet.questId());
            syncPacket.accept(player, new SyncQuestInstancePacket(packet.questId(), null));
        }
    }
    
    /**
     * 查找玩家背包中指定委托的委托书
     */
    public static ItemStack findQuestScroll(ServerPlayer player, ResourceLocation questId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem && QuestScrollItem.hasQuest(stack)) {
                Optional<ResourceLocation> scrollQuestId = QuestScrollItem.getQuestId(stack);
                if (scrollQuestId.isPresent() && scrollQuestId.get().equals(questId)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
