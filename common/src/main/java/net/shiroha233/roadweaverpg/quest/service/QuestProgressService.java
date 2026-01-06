package net.shiroha233.roadweaverpg.quest.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.type.QuestType;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 委托进度服务
 */
public class QuestProgressService {
    
    private final QuestDataAccessor dataAccessor;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestUpdated;
    private BiConsumer<ServerPlayer, QuestInstance> onQuestCompleted;
    
    public QuestProgressService(QuestDataAccessor dataAccessor) {
        this.dataAccessor = dataAccessor;
    }
    
    public void setOnQuestUpdated(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestUpdated = callback;
    }
    
    public void setOnQuestCompleted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        this.onQuestCompleted = callback;
    }
    
    public void updateProgress(ServerPlayer player, String eventType, Object eventData) {
        PlayerQuestData playerData = dataAccessor.getPlayerData(player);
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            if (instance.getState() != QuestState.IN_PROGRESS) continue;
            if (handleExpiration(player, instance, playerData)) continue;
            
            QuestDefinition definition = QuestDefinitionLoader.getInstance()
                    .getDefinition(instance.getQuestId());
            if (definition == null) continue;
            
            boolean updated = processObjectives(player, instance, definition, eventType, eventData);
            
            if (updated) {
                dataAccessor.markDirty(player);
                handleStateChange(player, instance, definition);
            }
        }
    }
    
    public void checkCollectObjectives(ServerPlayer player) {
        updateProgress(player, "inventory_check", null);
    }
    
    private boolean handleExpiration(ServerPlayer player, QuestInstance instance, PlayerQuestData playerData) {
        if (instance.isExpired()) {
            instance.setState(QuestState.EXPIRED);
            playerData.markQuestFailed(instance.getQuestId());
            updateScrollState(player, instance);
            RoadWeaverRPG.LOGGER.info("Quest {} expired for player {}", 
                    instance.getQuestId(), player.getName().getString());
            return true;
        }
        return false;
    }
    
    private boolean processObjectives(ServerPlayer player, QuestInstance instance, 
                                       QuestDefinition definition, String eventType, Object eventData) {
        boolean updated = false;
        
        for (QuestObjective objective : definition.getObjectives()) {
            int oldProgress = instance.getObjectiveProgress(objective.getId()).getCurrentProgress();
            int progress = objective.checkProgress(player, eventType, eventData);
            
            boolean shouldUpdate = progress > 0 || 
                    (objective.getType() == QuestType.COLLECT && "inventory_check".equals(eventType));
            
            if (shouldUpdate) {
                if (objective.getType() == QuestType.COLLECT) {
                    instance.setObjectiveProgress(objective.getId(), progress);
                } else {
                    instance.updateObjectiveProgress(objective.getId(), progress);
                }
                
                int newProgress = instance.getObjectiveProgress(objective.getId()).getCurrentProgress();
                if (newProgress != oldProgress) {
                    showProgressUpdate(player, objective, newProgress);
                }
                updated = true;
            }
        }
        return updated;
    }
    
    private void handleStateChange(ServerPlayer player, QuestInstance instance, QuestDefinition definition) {
        if (instance.getState() == QuestState.COMPLETED) {
            updateScrollState(player, instance);
            showQuestCompleted(player, definition);
            if (onQuestCompleted != null) {
                onQuestCompleted.accept(player, instance);
            }
        } else if (onQuestUpdated != null) {
            onQuestUpdated.accept(player, instance);
        }
    }
    
    private void showProgressUpdate(ServerPlayer player, QuestObjective objective, int currentProgress) {
        Component message = Component.literal(objective.getDescription().getString() + " ")
                .append(Component.literal("(" + currentProgress + "/" + objective.getRequiredAmount() + ")"))
                .withStyle(style -> style.withColor(0xFFAA00));
        player.displayClientMessage(message, true);
    }
    
    private void showQuestCompleted(ServerPlayer player, QuestDefinition definition) {
        Component title = Component.literal("✓ ")
                .append(definition.getTitle())
                .withStyle(style -> style.withColor(0x55FF55).withBold(true));
        player.displayClientMessage(title, false);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
    }
    
    private void updateScrollState(ServerPlayer player, QuestInstance instance) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem) {
                Optional<UUID> scrollInstanceId = QuestScrollItem.getInstanceId(stack);
                if (scrollInstanceId.isPresent() && scrollInstanceId.get().equals(instance.getInstanceId())) {
                    QuestScrollItem.updateState(stack, instance.getState());
                    break;
                }
            }
        }
    }
}
