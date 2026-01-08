package net.shiroha233.roadweaverpg.dialog.service;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.DialogRegistry;
import net.shiroha233.roadweaverpg.dialog.DialogSession;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * 对话跳转处理器
 * 职责：处理对话之间的跳转逻辑
 */
public class DialogTransitionHandler {
    
    private static final DialogTransitionHandler INSTANCE = new DialogTransitionHandler();
    
    private BiConsumer<ServerPlayer, DialogData> onDialogSend;
    
    public static DialogTransitionHandler getInstance() {
        return INSTANCE;
    }
    
    public void setOnDialogSend(BiConsumer<ServerPlayer, DialogData> callback) {
        this.onDialogSend = callback;
    }
    
    /**
     * 处理对话跳转
     */
    public boolean handleTransition(ServerPlayer player, DialogSession session, 
                                   DialogData.DialogChoice choice) {
        if (choice.nextDialog().isEmpty()) {
            return false;
        }
        
        ResourceLocation nextDialogId = choice.nextDialog().get();
        Optional<DialogData> nextDialogOpt = DialogRegistry.getDialog(nextDialogId);
        
        if (nextDialogOpt.isEmpty()) {
            RoadWeaverRPG.LOGGER.warn("跳转目标对话不存在: {}", nextDialogId);
            return false;
        }
        
        DialogData nextDialog = nextDialogOpt.get();
        
        // 发送新对话
        if (onDialogSend != null) {
            onDialogSend.accept(player, nextDialog);
        }
        
        return true;
    }
    
    /**
     * 处理自动跳转
     */
    public boolean handleAutoTransition(ServerPlayer player, DialogSession session, 
                                       DialogData dialog) {
        if (dialog.nextDialog().isEmpty()) {
            return false;
        }
        
        ResourceLocation nextDialogId = dialog.nextDialog().get();
        Optional<DialogData> nextDialogOpt = DialogRegistry.getDialog(nextDialogId);
        
        if (nextDialogOpt.isEmpty()) {
            RoadWeaverRPG.LOGGER.warn("自动跳转目标对话不存在: {}", nextDialogId);
            return false;
        }
        
        DialogData nextDialog = nextDialogOpt.get();
        
        // 发送新对话
        if (onDialogSend != null) {
            onDialogSend.accept(player, nextDialog);
        }
        
        return true;
    }
}
