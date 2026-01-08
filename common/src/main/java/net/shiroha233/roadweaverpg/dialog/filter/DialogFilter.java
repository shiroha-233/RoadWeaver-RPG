package net.shiroha233.roadweaverpg.dialog.filter;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.dialog.DialogData;

import java.util.List;

/**
 * 对话过滤器接口
 * 职责：过滤对话行和选项
 * 原理：策略模式，支持不同的过滤策略
 */
@FunctionalInterface
public interface DialogFilter {
    
    /**
     * 过滤对话行
     */
    List<DialogData.DialogLine> filterLines(DialogData dialog, ServerPlayer player, int npcEntityId);
    
    /**
     * 过滤对话选项（默认实现）
     */
    default List<DialogData.DialogChoice> filterChoices(DialogData dialog, ServerPlayer player, int npcEntityId) {
        return dialog.choices();
    }
}
