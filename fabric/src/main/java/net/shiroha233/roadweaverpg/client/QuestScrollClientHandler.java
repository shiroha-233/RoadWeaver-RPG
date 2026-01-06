package net.shiroha233.roadweaverpg.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.client.gui.QuestProgressScreen;
import net.shiroha233.roadweaverpg.item.ModItems;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.Optional;

/**
 * 委托书物品客户端交互处理
 */
@Environment(EnvType.CLIENT)
public class QuestScrollClientHandler {
    
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClientSide()) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() == ModItems.QUEST_SCROLL.get() && QuestScrollItem.hasQuest(stack)) {
                Optional<ResourceLocation> questId = QuestScrollItem.getQuestId(stack);
                questId.ifPresent(id -> {
                    // 先请求服务端同步最新进度
                    ClientNetworkHandlerFabric.sendRequestQuestProgress(id);
                    
                    // 同时尝试用缓存显示（服务端响应后会更新）
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
                return InteractionResultHolder.success(stack);
            }
            
            return InteractionResultHolder.pass(stack);
        });
    }
}
