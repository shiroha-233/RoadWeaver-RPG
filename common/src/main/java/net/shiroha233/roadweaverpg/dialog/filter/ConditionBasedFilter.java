package net.shiroha233.roadweaverpg.dialog.filter;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.dialog.DialogData;
import net.shiroha233.roadweaverpg.dialog.condition.DialogConditionRegistry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于条件的对话过滤器
 * 职责：根据条件过滤对话内容
 */
public class ConditionBasedFilter implements DialogFilter {
    
    private static final ConditionBasedFilter INSTANCE = new ConditionBasedFilter();
    
    public static ConditionBasedFilter getInstance() {
        return INSTANCE;
    }
    
    @Override
    public List<DialogData.DialogLine> filterLines(DialogData dialog, ServerPlayer player, int npcEntityId) {
        return dialog.lines().stream()
                .filter(line -> DialogConditionRegistry.evaluate(line.condition(), player, npcEntityId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DialogData.DialogChoice> filterChoices(DialogData dialog, ServerPlayer player, int npcEntityId) {
        return dialog.choices().stream()
                .filter(choice -> DialogConditionRegistry.evaluate(choice.condition(), player, npcEntityId))
                .collect(Collectors.toList());
    }
}
