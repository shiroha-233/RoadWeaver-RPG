package net.shiroha233.roadweaverpg.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.client.ClientQuestCache;
import net.shiroha233.roadweaverpg.client.gui.quest.QuestProgressScreen;
import net.shiroha233.roadweaverpg.forge.network.ClientPacketHandler;
import net.shiroha233.roadweaverpg.item.ModItems;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.Optional;
import java.util.UUID;

/**
 * Forge端委托书物品客户端交互处理
 * 
 * 修复：
 * - 增强缓存查找的可靠性
 * - 即使缓存中没有实例，也先请求服务端同步再尝试打开
 * - 添加重试机制
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT)
public class QuestScrollClientHandler {
    
    // 等待服务端响应的最大时间（毫秒）
    private static final int SYNC_WAIT_TIME = 100;
    
    // 记录上次请求的委托信息，用于延迟打开
    private static ResourceLocation pendingQuestId = null;
    private static UUID pendingInstanceId = null;
    private static long pendingRequestTime = 0;
    
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != ModItems.QUEST_SCROLL.get() || !QuestScrollItem.hasQuest(stack)) {
            return;
        }
        
        Optional<ResourceLocation> questIdOpt = QuestScrollItem.getQuestId(stack);
        Optional<UUID> instanceIdOpt = QuestScrollItem.getInstanceId(stack);
        
        if (questIdOpt.isEmpty()) return;
        
        ResourceLocation questId = questIdOpt.get();
        UUID instanceId = instanceIdOpt.orElse(null);
        
        // 先请求服务端同步最新进度
        ClientPacketHandler.sendRequestQuestProgress(questId, instanceId);
        
        // 尝试从缓存获取
        Optional<QuestDefinition> questOpt = ClientQuestCache.getQuest(questId);
        Optional<QuestInstance> cachedInstance = instanceId != null
                ? ClientQuestCache.getInstanceByUUID(instanceId)
                : ClientQuestCache.getInstance(questId);
        
        if (questOpt.isPresent() && cachedInstance.isPresent()) {
            // 缓存命中，直接打开
            openQuestProgressScreen(questOpt.get(), cachedInstance.get());
        } else {
            // 缓存未命中，记录待处理请求
            pendingQuestId = questId;
            pendingInstanceId = instanceId;
            pendingRequestTime = System.currentTimeMillis();
            
            RoadWeaverRPG.LOGGER.debug("Quest scroll cache miss, waiting for sync: questId={}, instanceId={}", 
                    questId, instanceId);
            
            // 延迟尝试打开
            Minecraft.getInstance().tell(() -> tryOpenPendingQuest());
        }
    }
    
    /**
     * 尝试打开待处理的委托
     */
    private static void tryOpenPendingQuest() {
        if (pendingQuestId == null) return;
        
        if (System.currentTimeMillis() - pendingRequestTime > SYNC_WAIT_TIME) {
            Optional<QuestDefinition> questOpt = ClientQuestCache.getQuest(pendingQuestId);
            Optional<QuestInstance> instanceOpt = pendingInstanceId != null
                    ? ClientQuestCache.getInstanceByUUID(pendingInstanceId)
                    : ClientQuestCache.getInstance(pendingQuestId);
            
            if (questOpt.isPresent() && instanceOpt.isPresent()) {
                openQuestProgressScreen(questOpt.get(), instanceOpt.get());
            } else {
                RoadWeaverRPG.LOGGER.warn("Failed to open quest scroll: cache still empty. questId={}", 
                        pendingQuestId);
            }
            
            clearPending();
        } else {
            Minecraft.getInstance().tell(() -> tryOpenPendingQuest());
        }
    }
    
    /**
     * 当收到服务端同步的实例数据时调用
     */
    public static void onInstanceSynced(QuestInstance instance) {
        if (instance == null) return;
        
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
