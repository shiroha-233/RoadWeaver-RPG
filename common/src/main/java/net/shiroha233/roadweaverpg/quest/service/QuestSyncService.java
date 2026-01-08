package net.shiroha233.roadweaverpg.quest.service;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.item.ModItems;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * 委托同步服务
 * 负责客户端-服务端数据同步
 */
public class QuestSyncService {
    
    private final QuestDataAccessor dataAccessor;
    
    private BiConsumer<ServerPlayer, Collection<QuestInstance>> onSyncAllQuests;
    private BiConsumer<ServerPlayer, Collection<QuestDefinition>> onSyncAllDefinitions;
    private BiConsumer<ServerPlayer, QuestInstance> onSyncQuest;
    private BiConsumer<ServerPlayer, List<ResourceLocation>> onSyncDailyQuests;
    
    public QuestSyncService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }
    
    public void setOnSyncAllQuests(BiConsumer<ServerPlayer, Collection<QuestInstance>> callback) {
        this.onSyncAllQuests = callback;
    }
    
    public void setOnSyncAllDefinitions(BiConsumer<ServerPlayer, Collection<QuestDefinition>> callback) {
        this.onSyncAllDefinitions = callback;
    }
    
    public void setOnSyncQuest(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onSyncQuest = callback;
    }
    
    public void setOnSyncDailyQuests(BiConsumer<ServerPlayer, List<ResourceLocation>> callback) {
        this.onSyncDailyQuests = callback;
    }
    
    // region 同步方法
    public void syncAllQuestsToClient(ServerPlayer player) {
        if (onSyncAllQuests != null) {
            Collection<QuestInstance> quests = getAllActiveQuests(player);
            onSyncAllQuests.accept(player, quests);
        }
    }
    
    public void syncAllDefinitionsToClient(ServerPlayer player) {
        if (onSyncAllDefinitions != null) {
            Collection<QuestDefinition> definitions = QuestDefinitionLoader.getInstance().getAllDefinitions();
            onSyncAllDefinitions.accept(player, definitions);
        }
    }
    
    public void syncQuestToClient(ServerPlayer player, ResourceLocation questId) {
        getQuestInstance(player, questId).ifPresent(instance -> {
            if (onSyncQuest != null) {
                onSyncQuest.accept(player, instance);
            }
        });
    }
    
    public void syncDailyQuestsToClient(ServerPlayer player) {
        if (onSyncDailyQuests != null) {
            PlayerQuestData data = dataAccessor.getPlayerData(player);
            onSyncDailyQuests.accept(player, new java.util.ArrayList<>(data.getDailyQuests()));
        }
    }
    // endregion
    
    // region 查询方法
    public Collection<QuestInstance> getAllActiveQuests(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getActiveQuests();
    }
    
    public Optional<QuestInstance> getQuestInstance(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        return Optional.ofNullable(data.getActiveQuest(questId));
    }
    
    /** 通过instanceId精确查询委托实例 */
    public Optional<QuestInstance> getQuestInstanceByUUID(ServerPlayer player, java.util.UUID instanceId) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        return data.getActiveQuests().stream()
                .filter(inst -> inst.getInstanceId().equals(instanceId))
                .findFirst();
    }
    
    public List<QuestDefinition> getAvailableQuests(ServerPlayer player) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        return QuestDefinitionLoader.getInstance().getAvailableQuests(
                data.getCompletedQuests(),
                data.getActiveQuestIds()
        );
    }
    
    public Optional<QuestInstance> getQuestByScroll(ServerPlayer player, ItemStack scroll) {
        if (!QuestScrollItem.hasQuest(scroll)) return Optional.empty();
        
        Optional<ResourceLocation> questId = QuestScrollItem.getQuestId(scroll);
        if (questId.isEmpty()) return Optional.empty();
        
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        return Optional.ofNullable(playerData.getActiveQuest(questId.get()));
    }
    // endregion

    
    // region 委托书验证
    public void validateInventoryScrolls(ServerPlayer player) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem && QuestScrollItem.hasQuest(stack)) {
                validateSingleScroll(stack, playerData, player);
            }
        }
    }
    
    private void validateSingleScroll(ItemStack stack, PlayerQuestData playerData, ServerPlayer player) {
        Optional<ResourceLocation> questIdOpt = QuestScrollItem.getQuestId(stack);
        if (questIdOpt.isEmpty()) return;
        
        ResourceLocation questId = questIdOpt.get();
        QuestInstance instance = playerData.getActiveQuest(questId);
        
        if (instance != null) {
            QuestState scrollState = QuestScrollItem.getQuestState(stack);
            if (scrollState != null && !instance.getState().getSerializedName().equals(scrollState.getSerializedName())) {
                QuestScrollItem.updateState(stack, instance.getState());
                RoadWeaverRPG.LOGGER.debug("Healed quest scroll state for player {}: {} -> {}", 
                        player.getName().getString(), questId, instance.getState());
            }
        } else if (playerData.hasCompletedQuest(questId)) {
            QuestScrollItem.updateState(stack, QuestState.TURNED_IN);
        }
    }
    
    public void retrieveLostScrolls(ServerPlayer player, com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        int issuedCount = 0;
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            if (instance.getState() == QuestState.TURNED_IN) continue;
            
            if (!hasScrollForQuest(player, instance.getQuestId())) {
                if (issueReplacementScroll(player, instance)) {
                    issuedCount++;
                }
            }
        }
        
        // 通过气泡显示结果，而不是聊天栏
        if (issuedCount > 0) {
            showBubbleMessage(maid, Component.translatable(
                    "gui.roadweaver_rpg.dialog.response.retrieve_scrolls_success", issuedCount));
        } else {
            showBubbleMessage(maid, Component.translatable(
                    "gui.roadweaver_rpg.dialog.response.no_scrolls_to_retrieve"));
        }
    }
    
    private void showBubbleMessage(com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid, Component message) {
        com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData bubble = 
            com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData.create(
                60, message, 
                com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData.TYPE_2,
                com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData.DEFAULT_PRIORITY
            );
        maid.getChatBubbleManager().addChatBubble(bubble);
    }
    
    private boolean hasScrollForQuest(ServerPlayer player, ResourceLocation questId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem) {
                Optional<ResourceLocation> qId = QuestScrollItem.getQuestId(stack);
                if (qId.isPresent() && qId.get().equals(questId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean issueReplacementScroll(ServerPlayer player, QuestInstance instance) {
        QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(instance.getQuestId());
        if (def == null || ModItems.QUEST_SCROLL == null) return false;
        
        ItemStack scroll = QuestScrollItem.createWithQuest(ModItems.QUEST_SCROLL.get(), def, instance);
        if (!player.getInventory().add(scroll)) {
            player.drop(scroll, false);
        }
        return true;
    }
    // endregion
}
