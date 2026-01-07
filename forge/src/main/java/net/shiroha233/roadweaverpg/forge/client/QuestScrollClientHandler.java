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

/**
 * Forge端委托书物品客户端交互处理
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT)
public class QuestScrollClientHandler {
    
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == ModItems.QUEST_SCROLL.get() && QuestScrollItem.hasQuest(stack)) {
            Optional<ResourceLocation> questId = QuestScrollItem.getQuestId(stack);
            questId.ifPresent(id -> {
                // 先请求服务端同步最新进度
                ClientPacketHandler.sendRequestQuestProgress(id);
                
                // 同时尝试用缓存显示
                // 优先使用客户端缓存，因为 QuestDefinitionManager 仅在服务端有效
                Optional<QuestDefinition> questOpt = ClientQuestCache.getQuest(id);
                if (questOpt.isPresent()) {
                    Optional<QuestInstance> cachedInstance = ClientQuestCache.getInstance(id);
                    if (cachedInstance.isPresent()) {
                        Minecraft.getInstance().setScreen(
                                new QuestProgressScreen(questOpt.get(), cachedInstance.get()));
                    }
                }
            });
        }
    }
}
