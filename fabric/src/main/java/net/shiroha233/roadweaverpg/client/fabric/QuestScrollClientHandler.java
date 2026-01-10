package net.shiroha233.roadweaverpg.client.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.client.ClientQuestCache;
import net.shiroha233.roadweaverpg.client.gui.quest.QuestProgressScreen;
import net.shiroha233.roadweaverpg.item.ModItems;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.Optional;
import java.util.UUID;

/**
 * 委托书物品客户端交互处理
 * 
 * 修复：
 * - 增强缓存查找的可靠性
 * - 即使缓存中没有实例，也先请求服务端同步再尝试打开
 * - 添加重试机制
 */
@Environment(EnvType.CLIENT)
public class QuestScrollClientHandler {
    
    // 等待服务端响应的最大时间（毫秒）
    private static final int SYNC_WAIT_TIME = 100;
    
    // 记录上次请求的委托信息，用于延迟打开
    private static ResourceLocation pendingQuestId = null;
    private static UUID pendingInstanceId = null;
    private static long pendingRequestTime = 0;
    
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() != ModItems.QUEST_SCROLL.get() || !QuestScrollItem.hasQuest(stack)) {
                return InteractionResultHolder.pass(stack);
            }
            
            Optional<ResourceLocation> questIdOpt = QuestScrollItem.getQuestId(stack);
            Optional<UUID> instanceIdOpt = QuestScrollItem.getInstanceId(stack);
            
            if (questIdOpt.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            
            ResourceLocation questId = questIdOpt.get();
            UUID instanceId = instanceIdOpt.orElse(null);
            
            // 先请求服务端同步最新进度
            ClientNetworkHandlerFabric.sendRequestQuestProgress(questId, instanceId);
            
            // 尝试从缓存获取
            Optional<QuestDefinition> questOpt = ClientQuestCache.getQuest(questId);
            Optional<QuestInstance> cachedInstance = instanceId != null
                    ? ClientQuestCache.getInstanceByUUID(instanceId)
                    : ClientQuestCache.getInstance(questId);
            
            if (questOpt.isPresent() && cachedInstance.isPresent()) {
                // 缓存命中，直接打开
                openQuestProgressScreen(questOpt.get(), cachedInstance.get());
            } else {
                // 缓存未命中，记录待处理请求，等待服务端同步后再打开
                pendingQuestId = questId;
                pendingInstanceId = instanceId;
                pendingRequestTime = System.currentTimeMillis();
                
                RoadWeaverRPG.LOGGER.debug("Quest scroll cache miss, waiting for sync: questId={}, instanceId={}", 
                        questId, instanceId);
                
                // 延迟尝试打开（给服务端响应一点时间）
                Minecraft.getInstance().tell(() -> {
                    tryOpenPendingQuest();
                });
            }
            
            return InteractionResultHolder.success(stack);
        });
    }
    
    /**
     * 尝试打开待处理的委托
     * 在服务端同步数据后调用
     */
    private static void tryOpenPendingQuest() {
        if (pendingQuestId == null) return;
        
        // 检查是否超时
        if (System.currentTimeMillis() - pendingRequestTime > SYNC_WAIT_TIME) {
            // 再次尝试从缓存获取
            Optional<QuestDefinition> questOpt = ClientQuestCache.getQuest(pendingQuestId);
            Optional<QuestInstance> instanceOpt = pendingInstanceId != null
                    ? ClientQuestCache.getInstanceByUUID(pendingInstanceId)
                    : ClientQuestCache.getInstance(pendingQuestId);
            
            if (questOpt.isPresent() && instanceOpt.isPresent()) {
                openQuestProgressScreen(questOpt.get(), instanceOpt.get());
            } else {
                RoadWeaverRPG.LOGGER.warn("Failed to open quest scroll: cache still empty after sync. questId={}", 
                        pendingQuestId);
            }
            
            // 清除待处理状态
            clearPending();
        } else {
            // 继续等待
            Minecraft.getInstance().tell(() -> {
                tryOpenPendingQuest();
            });
        }
    }
    
    /**
     * 当收到服务端同步的实例数据时调用
     */
    public static void onInstanceSynced(QuestInstance instance) {
        if (instance == null) return;
        
        // 检查是否是待处理的委托
        if (pendingQuestId != null && instance.getQuestId().equals(pendingQuestId)) {
            if (pendingInstanceId == null || instance.getInstanceId().equals(pendingInstanceId)) {
                Optional<QuestDefinition> questOpt = ClientQuestCache.getQuest(pendingQuestId);
                if (questOpt.isPresent()) {
                    openQuestProgressScreen(questOpt.get(), instance);
                    clearPending();
                }
            }
        }
    }
    
    private static void openQuestProgressScreen(QuestDefinition quest, QuestInstance instance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || !(mc.screen instanceof QuestProgressScreen)) {
            mc.setScreen(new QuestProgressScreen(quest, instance));
        }
    }
    
    private static void clearPending() {
        pendingQuestId = null;
        pendingInstanceId = null;
        pendingRequestTime = 0;
    }
}
