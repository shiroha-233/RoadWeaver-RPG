package net.shiroha233.roadweaverpg.entity.npc.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.event.DialogChoiceEvent;
import net.shiroha233.roadweaverpg.dialog.event.DialogStartEvent;
import net.shiroha233.roadweaverpg.quest.event.QuestEventBus;

/**
 * 对话行为集成
 * 职责：将对话系统与NPC行为系统集成
 * 原理：监听对话事件，自动触发对应的动作和语音（从数据包加载）
 */
public final class DialogBehaviorIntegration {
    
    private static boolean initialized = false;
    
    private DialogBehaviorIntegration() {}
    
    /**
     * 初始化集成
     */
    public static void init() {
        if (initialized) return;
        
        QuestEventBus eventBus = QuestEventBus.getInstance();
        
        // 监听对话开始事件
        eventBus.subscribe(DialogStartEvent.class, DialogBehaviorIntegration::onDialogStart);
        
        // 监听对话选择事件
        eventBus.subscribe(DialogChoiceEvent.class, DialogBehaviorIntegration::onDialogChoice);
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("对话行为集成已初始化");
    }
    
    /**
     * 对话开始时触发
     */
    private static void onDialogStart(DialogStartEvent event) {
        ServerPlayer player = event.getPlayer();
        int npcEntityId = event.getNpcEntityId();
        
        Entity entity = player.level().getEntity(npcEntityId);
        if (!(entity instanceof EntityMaid maid)) return;
        
        // 播放打招呼行为
        ResourceLocation presetId = new ResourceLocation(RoadWeaverRPG.MOD_ID, "greeting");
        NPCBehaviorManager.getInstance().playPreset(maid, presetId);
    }
    
    /**
     * 对话选择时触发
     */
    private static void onDialogChoice(DialogChoiceEvent event) {
        ServerPlayer player = event.getPlayer();
        int npcEntityId = event.getNpcEntityId();
        DialogData.DialogChoice choice = event.getChoice();
        
        Entity entity = player.level().getEntity(npcEntityId);
        if (!(entity instanceof EntityMaid maid)) return;
        
        // 根据选项动作类型播放对应行为
        String action = choice.action();
        playBehaviorForAction(maid, action);
    }
    
    /**
     * 根据动作类型播放行为
     */
    private static void playBehaviorForAction(EntityMaid maid, String action) {
        NPCBehaviorManager manager = NPCBehaviorManager.getInstance();
        
        // 构建预设ID
        ResourceLocation presetId = switch (action) {
            case "accept", "yes" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "confirm");
            case "deny", "no", "refuse" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "deny");
            case "quest_accept" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "quest_accept");
            case "quest_complete" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "quest_complete");
            case "shop", "buy" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop_buy");
            case "happy" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "happy");
            case "sad" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "sad");
            case "angry" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "angry");
            case "thinking" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "thinking");
            case "close", "leave" -> new ResourceLocation(RoadWeaverRPG.MOD_ID, "farewell");
            default -> {
                // 默认播放点头确认
                if (!action.isEmpty() && !"none".equals(action)) {
                    yield new ResourceLocation(RoadWeaverRPG.MOD_ID, "confirm");
                }
                yield null;
            }
        };
        
        if (presetId != null) {
            manager.playPreset(maid, presetId);
        }
    }
}
