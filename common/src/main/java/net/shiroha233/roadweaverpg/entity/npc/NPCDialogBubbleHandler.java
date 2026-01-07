package net.shiroha233.roadweaverpg.entity.npc;

import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import static com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData.DEFAULT_PRIORITY;
import static com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.IChatBubbleData.TYPE_2;

/**
 * NPC对话气泡处理器
 * 职责：处理NPC对话时的气泡消息显示
 * 原理：使用TouhouLittleMaid的ChatBubbleManager API显示气泡
 */
public class NPCDialogBubbleHandler {
    
    // 气泡显示时长（3秒 = 60 ticks）
    private static final int BUBBLE_DURATION = 60;
    
    /**
     * 处理公会女仆的对话气泡
     */
    public static void handleGuildMaidDialog(ServerPlayer player, int entityId, String dialogOption) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof GuildMaidEntity maid)) return;
        
        String langKey = switch (dialogOption) {
            case "SHOW_QUESTS" -> "npc.roadweaver_rpg.guild_maid.show_quests";
            case "COMPLETE_QUEST" -> "npc.roadweaver_rpg.guild_maid.complete_quest";
            case "VIEW_REPUTATION" -> "npc.roadweaver_rpg.guild_maid.view_reputation";
            case "RETRIEVE_SCROLL" -> "npc.roadweaver_rpg.guild_maid.retrieve_scroll";
            default -> null;
        };
        
        if (langKey != null) {
            addBubble(maid, Component.translatable(langKey));
        }
    }
    
    /**
     * 处理商店女仆的对话气泡
     */
    public static void handleShopMaidDialog(ServerPlayer player, int entityId, String dialogOption) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof ShopMaidEntity maid)) return;
        
        String langKey = switch (dialogOption) {
            case "OPEN_SHOP" -> "npc.roadweaver_rpg.shop_maid.open_shop";
            default -> null;
        };
        
        if (langKey != null) {
            addBubble(maid, Component.translatable(langKey));
        }
    }
    
    /**
     * 显示气泡消息（公共方法）
     */
    public static void showBubbleMessage(com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid, Component message) {
        addBubble(maid, message);
    }
    
    /**
     * 添加气泡消息（统一方法）
     */
    private static void addBubble(com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid, Component message) {
        TextChatBubbleData bubble = TextChatBubbleData.create(BUBBLE_DURATION, message, TYPE_2, DEFAULT_PRIORITY);
        maid.getChatBubbleManager().addChatBubble(bubble);
    }
}
