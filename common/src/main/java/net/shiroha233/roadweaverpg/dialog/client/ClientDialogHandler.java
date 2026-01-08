package net.shiroha233.roadweaverpg.dialog.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.network.packet.dialog.DialogSessionClosePacket;

import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端对话处理器
 * 职责：处理客户端的对话数据和界面显示
 * 
 * 优化：
 * - 支持更新现有界面而不是重新创建
 * - 支持玩家回话显示
 */
public final class ClientDialogHandler {
    
    private ClientDialogHandler() {}
    
    // 回调函数（由平台特定代码设置）
    private static Consumer<String> onChoiceSelected;
    private static Runnable onAdvanceRequested;
    
    // 当前对话状态
    private static int currentNpcEntityId = -1;
    private static DialogData currentDialog = null;
    private static long currentSyncVersion = 0L;
    
    /**
     * 初始化回调
     */
    public static void init(Consumer<String> choiceCallback, Runnable advanceCallback) {
        onChoiceSelected = choiceCallback;
        onAdvanceRequested = advanceCallback;
    }
    
    /**
     * 处理完整对话数据（带版本号）
     * 
     * 原理：
     * - 如果当前已有对话界面且NPC相同，更新界面内容而不是重新创建
     * - 这样可以避免选择选项后UI闪烁
     */
    public static void handleDialogData(int npcEntityId, DialogData dialog, long syncVersion) {
        currentNpcEntityId = npcEntityId;
        currentDialog = dialog;
        currentSyncVersion = syncVersion;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        var entity = mc.level.getEntity(npcEntityId);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity npc)) return;
        
        // 检查是否可以更新现有界面
        if (mc.screen instanceof DialogScreenInterface dialogScreen) {
            // 更新现有界面的对话内容
            dialogScreen.updateDialog(dialog);
        } else {
            // 创建新的对话界面
            showDialogScreen(npc, dialog);
        }
    }
    
    /**
     * 处理单行对话
     */
    public static void handleDialogLine(int npcEntityId, DialogData.DialogLine line, int lineIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogScreenInterface dialogScreen) {
            dialogScreen.updateDialogLine(line, lineIndex);
        }
    }
    
    /**
     * 处理对话选项
     */
    public static void handleDialogChoices(ResourceLocation dialogId, List<DialogData.DialogChoice> choices) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogScreenInterface dialogScreen) {
            dialogScreen.showChoices(choices);
        } else {
            showChoicesScreen(choices);
        }
    }
    
    /**
     * 处理会话关闭
     */
    public static void handleSessionClose(DialogSessionClosePacket.CloseReason reason) {
        currentNpcEntityId = -1;
        currentDialog = null;
        currentSyncVersion = 0L;
        
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.screen instanceof DialogScreenInterface) {
            mc.setScreen(null);
        }
        
        // 非正常关闭时显示消息
        if (reason != DialogSessionClosePacket.CloseReason.NORMAL && mc.player != null) {
            String messageKey = switch (reason) {
                case TIMEOUT -> "gui.roadweaver_rpg.dialog.session_timeout";
                case CANCELLED -> "gui.roadweaver_rpg.dialog.session_cancelled";
                case ERROR -> "gui.roadweaver_rpg.dialog.session_error";
                default -> null;
            };
            
            if (messageKey != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(messageKey), true);
            }
        }
    }
    
    /**
     * 选择对话选项
     */
    public static void selectChoice(String choiceId) {
        if (onChoiceSelected != null) {
            onChoiceSelected.accept(choiceId);
        }
    }
    
    /**
     * 请求推进对话
     */
    public static void requestAdvance() {
        if (onAdvanceRequested != null) {
            onAdvanceRequested.run();
        }
    }
    
    /**
     * 显示对话界面
     */
    private static void showDialogScreen(net.minecraft.world.entity.LivingEntity npc, DialogData dialog) {
        Minecraft mc = Minecraft.getInstance();
        
        // 直接创建 GalgameDialogScreen 并设置完整数据
        var screen = new net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen(currentNpcEntityId);
        
        // 设置对话数据引用
        screen.setDialogData(dialog);
        
        // 添加所有对话行
        for (DialogData.DialogLine line : dialog.lines()) {
            if (line.isNpc()) {
                screen.addDialog(npc.getDisplayName(), line.getTextComponent(), true);
            } else {
                screen.addDialog(mc.player.getDisplayName(), line.getTextComponent(), false);
            }
        }
        
        // 添加选项（包含完整信息：choiceId、action、responseText）
        if (!dialog.choices().isEmpty()) {
            var options = new java.util.ArrayList<net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen.DialogOptionData>();
            for (DialogData.DialogChoice choice : dialog.choices()) {
                options.add(new net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogScreen.DialogOptionData(
                        choice.getTextComponent(),
                        choice.id(),      // choiceId
                        choice.action(),  // action（用于判断close）
                        choice.hasResponse() ? choice.getResponseComponent() : net.minecraft.network.chat.Component.empty()
                ));
            }
            
            // 设置选项和回调
            screen.setOptions(options, index -> {
                if (index >= 0 && index < dialog.choices().size()) {
                    selectChoice(dialog.choices().get(index).id());
                }
            });
        }
        
        mc.setScreen(screen);
    }
    
    /**
     * 显示选项界面
     */
    private static void showChoicesScreen(List<DialogData.DialogChoice> choices) {
        if (choices.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        
        var builder = net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogBuilder
                .create(currentNpcEntityId);
        
        if (mc.level != null && currentNpcEntityId >= 0) {
            var entity = mc.level.getEntity(currentNpcEntityId);
            if (entity instanceof net.minecraft.world.entity.LivingEntity npc) {
                builder.npcName(npc.getDisplayName());
            }
        }
        
        for (DialogData.DialogChoice choice : choices) {
            builder.addOption(choice.getTextComponent(), choice.id());
        }
        
        builder.onOptionSelected(index -> {
            if (index >= 0 && index < choices.size()) {
                selectChoice(choices.get(index).id());
            }
        });
        
        mc.setScreen(builder.build());
    }
    
    public static int getCurrentNpcEntityId() {
        return currentNpcEntityId;
    }
    
    public static DialogData getCurrentDialog() {
        return currentDialog;
    }
    
    public static long getCurrentSyncVersion() {
        return currentSyncVersion;
    }
    
    public static boolean isInDialog() {
        return currentNpcEntityId >= 0 && currentDialog != null;
    }
    
    /**
     * 对话界面接口
     */
    public interface DialogScreenInterface {
        void updateDialogLine(DialogData.DialogLine line, int lineIndex);
        void showChoices(List<DialogData.DialogChoice> choices);
        /** 更新整个对话内容（用于选项跳转后更新界面） */
        void updateDialog(DialogData dialog);
    }
}
