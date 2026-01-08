package net.shiroha233.roadweaverpg.quest.objective;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;

import java.util.List;
import java.util.Optional;

/**
 * 目标进度检查器
 * 
 * 设计原理：
 * - 统一处理所有目标类型的进度检查逻辑
 * - 将事件匹配、条件评估、进度计算分离
 * - 提供可扩展的检查策略
 * - 支持自定义检查器注册
 */
public final class ObjectiveProgressChecker {
    
    private ObjectiveProgressChecker() {}
    
    /**
     * 检查目标进度（统一入口）
     * 
     * @param objective 目标定义
     * @param player 玩家
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @return 进度检查结果
     */
    public static ObjectiveProgressResult check(QuestObjective objective,
                                                 ServerPlayer player,
                                                 String eventType,
                                                 Object eventData) {
        return switch (objective.getType()) {
            case COLLECT -> checkCollect(objective, player, eventType);
            case KILL -> checkKill(objective, player, eventType, eventData);
            case LOCATION_KILL -> checkLocationKill(objective, player, eventType, eventData);
            case EXPLORE -> checkExplore(objective, player, eventType);
            default -> {
                RoadWeaverRPG.LOGGER.debug("不支持的目标类型: {}, 目标ID: {}", 
                        objective.getType(), objective.getId());
                yield ObjectiveProgressResult.skip("unsupported_type");
            }
        };
    }
    
    // ==================== 收集类目标检查 ====================
    
    /**
     * 收集类目标检查
     * 
     * 设计说明：
     * - 收集类目标的条件是"持续条件"，在检查背包时评估
     * - 适用于：高度限制、维度限制、群系限制等持续性条件
     * - 不适用于：in_water、in_lava 等瞬时条件
     * 
     * 如果需要瞬时条件（如在水中采集），应该使用其他机制：
     * 1. 使用自定义事件在采集时触发
     * 2. 或者使用组合目标（先到达位置，再采集物品）
     */
    private static ObjectiveProgressResult checkCollect(QuestObjective objective,
                                                         ServerPlayer player,
                                                         String eventType) {
        if (!"inventory_check".equals(eventType)) {
            return ObjectiveProgressResult.Skip.EVENT_MISMATCH;
        }
        
        if (!(objective instanceof CollectObjective collectObj)) {
            return ObjectiveProgressResult.skip("invalid_objective_type");
        }
        
        // 评估额外条件（持续条件）
        List<PlayerCondition<ConditionContext>> conditions = collectObj.getConditions();
        if (!conditions.isEmpty()) {
            ConditionContext ctx = ObjectiveConditionEvaluator.buildPlayerContext(player);
            if (!ObjectiveConditionEvaluator.evaluateAll(player, conditions, ctx)) {
                return ObjectiveProgressResult.Skip.CONDITION_NOT_MET;
            }
        }
        
        // 计算物品数量
        int count = countItemInInventory(player, objective.getTargetResource());
        return ObjectiveProgressResult.absolute(count);
    }
    
    // ==================== 击杀类目标检查 ====================
    
    private static ObjectiveProgressResult checkKill(QuestObjective objective,
                                                      ServerPlayer player,
                                                      String eventType,
                                                      Object eventData) {
        if (!"entity_kill".equals(eventType) || !(eventData instanceof Entity entity)) {
            return ObjectiveProgressResult.Skip.EVENT_MISMATCH;
        }
        
        // 检查实体类型
        if (!matchEntityType(entity, objective.getTargetResource())) {
            return ObjectiveProgressResult.Skip.TARGET_MISMATCH;
        }
        
        // 评估额外条件
        if (objective instanceof KillObjective killObj) {
            List<PlayerCondition<ConditionContext>> conditions = killObj.getConditions();
            if (!conditions.isEmpty()) {
                ConditionContext ctx = ObjectiveConditionEvaluator.buildKillContext(player, entity);
                if (!ObjectiveConditionEvaluator.evaluateAll(player, conditions, ctx)) {
                    return ObjectiveProgressResult.Skip.CONDITION_NOT_MET;
                }
            }
        }
        
        return ObjectiveProgressResult.one();
    }
    
    // ==================== 定点击杀目标检查 ====================
    
    private static ObjectiveProgressResult checkLocationKill(QuestObjective objective,
                                                              ServerPlayer player,
                                                              String eventType,
                                                              Object eventData) {
        if (!"entity_kill".equals(eventType) || !(eventData instanceof Entity entity)) {
            return ObjectiveProgressResult.Skip.EVENT_MISMATCH;
        }
        
        // 检查实体类型
        if (!matchEntityType(entity, objective.getTargetResource())) {
            return ObjectiveProgressResult.Skip.TARGET_MISMATCH;
        }
        
        if (!(objective instanceof LocationKillObjective locKillObj)) {
            return ObjectiveProgressResult.skip("invalid_objective_type");
        }
        
        // 构建上下文
        ConditionContext ctx = ObjectiveConditionEvaluator.buildKillContext(player, entity);
        
        // 评估额外条件
        List<PlayerCondition<ConditionContext>> conditions = locKillObj.getConditions();
        if (!conditions.isEmpty()) {
            if (!ObjectiveConditionEvaluator.evaluateAll(player, conditions, ctx)) {
                return ObjectiveProgressResult.Skip.CONDITION_NOT_MET;
            }
        }
        
        // 使用condition系统检查位置
        var locations = locKillObj.getLocations();
        if (!locations.isEmpty()) {
            boolean inLocation = false;
            for (var loc : locations) {
                // 使用toCondition()生成的条件进行判定
                if (loc.toCondition().evaluate(player, ctx)) {
                    inLocation = true;
                    break;
                }
            }
            
            if (!inLocation) {
                return ObjectiveProgressResult.Skip.LOCATION_MISMATCH;
            }
        }
        
        return ObjectiveProgressResult.one();
    }
    
    // ==================== 探索类目标检查 ====================
    
    private static ObjectiveProgressResult checkExplore(QuestObjective objective,
                                                         ServerPlayer player,
                                                         String eventType) {
        if (!"player_move".equals(eventType)) {
            return ObjectiveProgressResult.Skip.EVENT_MISMATCH;
        }
        
        if (!(objective instanceof ExploreObjective exploreObj)) {
            return ObjectiveProgressResult.skip("invalid_objective_type");
        }
        
        // 构建上下文
        ConditionContext ctx = ObjectiveConditionEvaluator.buildPlayerContext(player);
        
        // 评估额外条件
        List<PlayerCondition<ConditionContext>> extraConditions = exploreObj.getExtraConditions();
        if (!extraConditions.isEmpty()) {
            if (!ObjectiveConditionEvaluator.evaluateAll(player, extraConditions, ctx)) {
                return ObjectiveProgressResult.Skip.CONDITION_NOT_MET;
            }
        }
        
        // 使用condition系统统计已到达的目标点
        int reached = 0;
        for (var target : exploreObj.getTargets()) {
            // 使用toCondition()生成的条件进行判定
            if (target.toCondition().evaluate(player, ctx)) {
                reached++;
            }
        }
        
        return ObjectiveProgressResult.absolute(reached);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查实体类型是否匹配
     */
    private static boolean matchEntityType(Entity entity, ResourceLocation targetType) {
        Optional<ResourceLocation> entityType = RegistryHelper.getEntityTypeId(entity.getType());
        return entityType.isPresent() && entityType.get().equals(targetType);
    }
    
    /**
     * 统计背包中指定物品的数量
     */
    private static int countItemInInventory(ServerPlayer player, ResourceLocation itemId) {
        Optional<Item> targetItem = RegistryHelper.getItem(itemId);
        if (targetItem.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        Item item = targetItem.get();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
