package net.shiroha233.roadweaverpg.network;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity;
import net.shiroha233.roadweaverpg.entity.npc.NPCDialogBubbleHandler;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.network.packet.quest.*;
import net.shiroha233.roadweaverpg.network.packet.sync.*;
import net.shiroha233.roadweaverpg.network.packet.ui.*;
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
        if (!(entity instanceof GuildMaidEntity maid)) return;
        
        switch (packet.option()) {
            case SHOW_QUESTS -> {
                NPCDialogBubbleHandler.handleGuildMaidDialog(player, packet.entityId(), "SHOW_QUESTS");
                sendQuests.run();
            }
            case COMPLETE_QUEST -> {
                NPCDialogBubbleHandler.handleGuildMaidDialog(player, packet.entityId(), "COMPLETE_QUEST");
                handleQuestTurnIn(player, maid);
            }
            case VIEW_REPUTATION -> {
                NPCDialogBubbleHandler.handleGuildMaidDialog(player, packet.entityId(), "VIEW_REPUTATION");
                handleViewReputation(player);
            }
            case RETRIEVE_SCROLL -> {
                NPCDialogBubbleHandler.handleGuildMaidDialog(player, packet.entityId(), "RETRIEVE_SCROLL");
                handleRetrieveScroll(player, maid);
            }
        }
    }
    
    private static void handleRetrieveScroll(ServerPlayer player, GuildMaidEntity maid) {
        PlayerQuestService.getInstance().retrieveLostScrolls(player, maid);
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
     * 打开声望界面（供外部调用）
     */
    public static void openReputationGui(ServerPlayer player) {
        if (onOpenReputationGui != null) {
            onOpenReputationGui.accept(player);
        }
    }
    
    // 冒险等级GUI回调
    private static java.util.function.Consumer<ServerPlayer> onOpenAdventureLevelGui;
    
    public static void setOnOpenAdventureLevelGui(java.util.function.Consumer<ServerPlayer> callback) {
        onOpenAdventureLevelGui = callback;
    }
    
    /**
     * 打开冒险等级界面（供外部调用）
     */
    public static void openAdventureLevelGui(ServerPlayer player) {
        if (onOpenAdventureLevelGui != null) {
            onOpenAdventureLevelGui.accept(player);
        }
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
    public static void handleQuestTurnIn(ServerPlayer player, GuildMaidEntity maid) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof QuestScrollItem && QuestScrollItem.hasQuest(stack)) {
                if (QuestScrollItem.getQuestState(stack) == QuestState.COMPLETED) {
                    Optional<ResourceLocation> questId = QuestScrollItem.getQuestId(stack);
                    if (questId.isPresent() && PlayerQuestService.getInstance().turnInQuestByScroll(player, stack)) {
                        QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(questId.get());
                        if (def != null) {
                            // 通过气泡显示提交成功
                            NPCDialogBubbleHandler.showBubbleMessage(maid,
                                Component.translatable("gui.roadweaver_rpg.quest.turned_in", def.getTitle()));
                        }
                        return;
                    }
                }
            }
        }
        
        // 没有已完成的委托书，通过气泡显示
        NPCDialogBubbleHandler.showBubbleMessage(maid, 
            Component.translatable("gui.roadweaver_rpg.dialog.response.no_completed_quest"));
    }
    
    /**
     * 处理请求委托进度
     * 修复：支持通过instanceId精确查询
     */
    public static void handleRequestQuestProgress(ServerPlayer player, RequestQuestProgressPacket packet,
                                                   java.util.function.BiConsumer<ServerPlayer, SyncQuestInstancePacket> syncPacket) {
        PlayerQuestService manager = PlayerQuestService.getInstance();
        Optional<QuestInstance> instanceOpt;
        
        // 优先使用instanceId精确查询
        if (packet.instanceId() != null) {
            instanceOpt = manager.getQuestInstanceByUUID(player, packet.instanceId());
        } else {
            instanceOpt = manager.getQuestInstance(player, packet.questId());
        }
        
        if (instanceOpt.isPresent()) {
            syncPacket.accept(player, new SyncQuestInstancePacket(instanceOpt.get()));
        } else {
            RoadWeaverRPG.LOGGER.warn("Player {} requested progress for non-existent quest: {} (instanceId: {})", 
                    player.getName().getString(), packet.questId(), packet.instanceId());
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
