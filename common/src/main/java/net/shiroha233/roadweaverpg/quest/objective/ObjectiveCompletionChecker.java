package net.shiroha233.roadweaverpg.quest.objective;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.ObjectiveProgress;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.List;

/**
 * 目标完成检查器
 * 
 * 设计原理：
 * - 统一处理目标完成判定逻辑
 * - 支持条件性完成（需要满足额外条件才算完成）
 * - 支持批量检查优化
 */
public final class ObjectiveCompletionChecker {
    
    private ObjectiveCompletionChecker() {}
    
    /**
     * 检查单个目标是否完成
     * 
     * @param objective 目标定义
     * @param progress 目标进度
     * @param player 玩家（用于条件评估）
     * @return 是否完成
     */
    public static boolean isObjectiveComplete(QuestObjective objective,
                                               ObjectiveProgress progress,
                                               ServerPlayer player) {
        if (progress == null) {
            return false;
        }
        
        // 基础进度检查
        if (!progress.isCompleted()) {
            return false;
        }
        
        // 如果有完成条件，还需要检查条件
        List<PlayerCondition<ConditionContext>> completionConditions = 
                getCompletionConditions(objective);
        
        if (completionConditions.isEmpty()) {
            return true;
        }
        
        // 评估完成条件
        ConditionContext ctx = ObjectiveConditionEvaluator.buildPlayerContext(player);
        return ObjectiveConditionEvaluator.evaluateAll(player, completionConditions, ctx);
    }
    
    /**
     * 检查委托是否所有目标都完成
     */
    public static boolean areAllObjectivesComplete(QuestInstance instance,
                                                    QuestDefinition definition,
                                                    ServerPlayer player) {
        for (QuestObjective objective : definition.getObjectives()) {
            ObjectiveProgress progress = instance.getObjectiveProgress(objective.getId());
            if (!isObjectiveComplete(objective, progress, player)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 计算委托完成进度百分比
     */
    public static float calculateCompletionPercent(QuestInstance instance,
                                                    QuestDefinition definition) {
        if (definition.getObjectives().isEmpty()) {
            return 0f;
        }
        
        float total = 0f;
        for (QuestObjective objective : definition.getObjectives()) {
            ObjectiveProgress progress = instance.getObjectiveProgress(objective.getId());
            if (progress != null) {
                total += progress.getProgressPercent();
            }
        }
        
        return total / definition.getObjectives().size();
    }
    
    /**
     * 获取目标的完成条件
     */
    private static List<PlayerCondition<ConditionContext>> getCompletionConditions(
            QuestObjective objective) {
        // 目前只有收集类目标有额外条件
        // 未来可以扩展到其他目标类型
        if (objective instanceof CollectObjective collectObj) {
            return collectObj.getConditions();
        }
        return List.of();
    }
    
    /**
     * 检查目标进度是否有效
     */
    public static boolean isProgressValid(ObjectiveProgress progress, QuestObjective objective) {
        if (progress == null || objective == null) {
            return false;
        }
        
        int current = progress.getCurrentProgress();
        int required = objective.getRequiredAmount();
        
        // 进度不能为负
        if (current < 0) {
            return false;
        }
        
        // 进度不能超过需求的2倍（允许一定溢出）
        if (current > required * 2) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 修复无效的进度值
     */
    public static void fixInvalidProgress(ObjectiveProgress progress, QuestObjective objective) {
        if (progress == null || objective == null) {
            return;
        }
        
        int current = progress.getCurrentProgress();
        int required = objective.getRequiredAmount();
        
        if (current < 0) {
            progress.setProgress(0);
        } else if (current > required * 2) {
            progress.setProgress(required);
        }
    }
}
