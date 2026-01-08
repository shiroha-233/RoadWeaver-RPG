package net.shiroha233.roadweaverpg.entity.npc.action;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorConfig;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC动作管理器
 * 职责：管理NPC的动作播放和状态
 * 原理：
 * - 单例模式确保全局唯一
 * - 使用TouhouLittleMaid的Task系统控制动画
 * - 线程安全的状态管理
 * - 数据包驱动：从NPCBehaviorLoader加载动作配置
 */
public final class NPCActionManager {
    
    private static final NPCActionManager INSTANCE = new NPCActionManager();
    
    // NPC动作状态缓存 (entityId -> ActionState)
    private final Map<Integer, ActionState> actionStates = new ConcurrentHashMap<>();
    
    // 动作完成回调 (entityId -> callback)
    private final Map<Integer, Runnable> completionCallbacks = new ConcurrentHashMap<>();
    
    private NPCActionManager() {}
    
    public static NPCActionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 播放NPC动作
     * @param entity NPC实体（必须继承EntityMaid）
     * @param behaviorId 行为ID（从数据包加载）
     */
    public void playAction(EntityMaid entity, ResourceLocation behaviorId) {
        playAction(entity, behaviorId, null);
    }
    
    /**
     * 播放NPC动作（带回调）
     * @param entity NPC实体
     * @param behaviorId 行为ID
     * @param onComplete 完成回调
     */
    public void playAction(EntityMaid entity, ResourceLocation behaviorId, Runnable onComplete) {
        if (entity == null || entity.level().isClientSide) return;
        
        // 从数据包加载行为配置
        NPCBehaviorConfig config = NPCBehaviorLoader.getInstance().getBehavior(behaviorId);
        if (config == null) {
            RoadWeaverRPG.LOGGER.warn("找不到行为配置: {}", behaviorId);
            return;
        }
        
        int entityId = entity.getId();
        
        // 停止当前动作
        stopAction(entity);
        
        // 设置新动作状态
        ActionState state = new ActionState(
                behaviorId,
                config,
                entity.level().getGameTime()
        );
        actionStates.put(entityId, state);
        
        // 保存回调
        if (onComplete != null) {
            completionCallbacks.put(entityId, onComplete);
        }
        
        // 应用TouhouLittleMaid的Task
        applyTask(entity, config);
        
        RoadWeaverRPG.LOGGER.debug("NPC {} 开始播放动作: {}", entityId, behaviorId);
    }
    
    /**
     * 停止NPC动作
     */
    public void stopAction(EntityMaid entity) {
        if (entity == null) return;
        
        int entityId = entity.getId();
        ActionState state = actionStates.remove(entityId);
        completionCallbacks.remove(entityId);
        
        if (state != null) {
            // 恢复空闲Task
            entity.setTask(TaskManager.getIdleTask());
            
            RoadWeaverRPG.LOGGER.debug("NPC {} 停止动作: {}", entityId, state.behaviorId);
        }
    }
    
    /**
     * 更新动作状态（每tick调用）
     */
    public void tick(EntityMaid entity) {
        if (entity == null || entity.level().isClientSide) return;
        
        int entityId = entity.getId();
        ActionState state = actionStates.get(entityId);
        
        if (state == null) return;
        
        // 检查是否完成（非循环动作）
        if (!state.config.isLooping()) {
            long elapsed = entity.level().getGameTime() - state.startTime;
            if (elapsed >= state.config.durationTicks()) {
                // 动作完成
                Runnable callback = completionCallbacks.remove(entityId);
                actionStates.remove(entityId);
                
                // 恢复空闲
                entity.setTask(TaskManager.getIdleTask());
                
                // 执行回调
                if (callback != null) {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        RoadWeaverRPG.LOGGER.error("动作完成回调执行失败", e);
                    }
                }
                
                RoadWeaverRPG.LOGGER.debug("NPC {} 动作完成: {}", entityId, state.behaviorId);
            }
        }
    }
    
    /**
     * 获取当前动作ID
     */
    public ResourceLocation getCurrentAction(int entityId) {
        ActionState state = actionStates.get(entityId);
        return state != null ? state.behaviorId : null;
    }
    
    /**
     * 是否正在播放动作
     */
    public boolean isPlayingAction(int entityId) {
        return actionStates.containsKey(entityId);
    }
    
    /**
     * 获取动作剩余时间
     */
    public int getActionRemainingTicks(EntityMaid entity, int entityId) {
        ActionState state = actionStates.get(entityId);
        if (state == null || state.config.isLooping()) {
            return 0;
        }
        long elapsed = entity.level().getGameTime() - state.startTime;
        return Math.max(0, (int)(state.config.durationTicks() - elapsed));
    }
    
    /**
     * 清理实体的动作状态
     */
    public void cleanup(int entityId) {
        actionStates.remove(entityId);
        completionCallbacks.remove(entityId);
    }
    
    /**
     * 应用TouhouLittleMaid的Task
     */
    private void applyTask(EntityMaid entity, NPCBehaviorConfig config) {
        // 如果task_id为空，使用默认idle task
        if (config.taskId() == null || config.taskId().isEmpty()) {
            entity.setTask(TaskManager.getIdleTask());
            return;
        }
        
        ResourceLocation taskId = config.getTaskResourceLocation();
        TaskManager.findTask(taskId).ifPresentOrElse(
                entity::setTask,
                () -> {
                    // 找不到对应Task，使用空闲
                    entity.setTask(TaskManager.getIdleTask());
                    RoadWeaverRPG.LOGGER.warn("找不到Task: {}, 使用空闲Task", taskId);
                }
        );
    }
    
    /**
     * 动作状态记录
     */
    private record ActionState(
            ResourceLocation behaviorId,
            NPCBehaviorConfig config,
            long startTime
    ) {}
}
