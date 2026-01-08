package net.shiroha233.roadweaverpg.client.gui.galgame;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

/**
 * Galgame对话服务
 * 职责：提供便捷的API来创建和显示Galgame风格对话
 * 原理：封装GalgameDialogBuilder，提供更简洁的调用方式
 */
public final class GalgameDialogService {
    
    private GalgameDialogService() {}
    
    /**
     * 打开简单对话（单条NPC对话）
     */
    public static void openSimpleDialog(int npcEntityId, Component npcName, Component message) {
        GalgameDialogScreen screen = GalgameDialogBuilder.create(npcEntityId)
                .npcName(npcName)
                .npcSays(message)
                .build();
        
        Minecraft.getInstance().setScreen(screen);
    }
    
    /**
     * 打开带选项的对话
     */
    public static void openDialogWithOptions(int npcEntityId, Component npcName, 
                                              Component message, 
                                              java.util.List<GalgameDialogScreen.DialogOptionData> options,
                                              Consumer<Integer> onOptionSelected) {
        GalgameDialogBuilder builder = GalgameDialogBuilder.create(npcEntityId)
                .npcName(npcName)
                .npcSays(message)
                .onOptionSelected(onOptionSelected);
        
        // 添加选项（使用choiceId）
        for (GalgameDialogScreen.DialogOptionData option : options) {
            builder.addOption(option.text(), option.choiceId());
        }
        
        Minecraft.getInstance().setScreen(builder.build());
    }
    
    /**
     * 创建对话构建器（用于复杂对话）
     */
    public static GalgameDialogBuilder createBuilder(int npcEntityId) {
        return GalgameDialogBuilder.create(npcEntityId);
    }
    
    /**
     * 从实体创建对话构建器
     */
    public static GalgameDialogBuilder createBuilder(LivingEntity npc) {
        return GalgameDialogBuilder.create(npc.getId())
                .npcName(npc.getDisplayName());
    }
    
    /**
     * 显示对话界面
     */
    public static void showDialog(GalgameDialogScreen screen) {
        Minecraft.getInstance().setScreen(screen);
    }
}
