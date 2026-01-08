package net.shiroha233.roadweaverpg.quest.state;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 委托状态机
 * 
 * 设计原理：
 * - 定义合法的状态转移规则，防止非法状态变更
 * - 支持状态转移前后的钩子函数
 * - 记录状态转移历史，便于调试和回溯
 * - 线程安全的状态转移操作
 * 
 * 状态转移图：
 * AVAILABLE -> IN_PROGRESS (接取)
 * IN_PROGRESS -> COMPLETED (完成所有目标)
 * IN_PROGRESS -> EXPIRED (超时)
 * IN_PROGRESS -> ABANDONED (放弃)
 * IN_PROGRESS -> FAILED (失败)
 * COMPLETED -> TURNED_IN (提交)
 * COMPLETED -> EXPIRED (超时未提交)
 */
public class QuestStateMachine {
    
    private static volatile QuestStateMachine instance;
    private static final Object LOCK = new Object();
    
    // 合法的状态转移规则
    private final Map<QuestState, Set<QuestState>> transitions = new EnumMap<>(QuestState.class);
    
    // 状态转移前的钩子
    private final Map<StateTransition, List<BiConsumer<UUID, Object>>> beforeHooks = new ConcurrentHashMap<>();
    
    // 状态转移后的钩子
    private final Map<StateTransition, List<BiConsumer<UUID, Object>>> afterHooks = new ConcurrentHashMap<>();
    
    private QuestStateMachine() {
        initializeTransitions();
    }
    
    public static QuestStateMachine getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new QuestStateMachine();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化状态转移规则
     */
    private void initializeTransitions() {
        // AVAILABLE 可以转移到 IN_PROGRESS
        transitions.put(QuestState.AVAILABLE, EnumSet.of(QuestState.IN_PROGRESS));
        
        // IN_PROGRESS 可以转移到多个状态
        transitions.put(QuestState.IN_PROGRESS, EnumSet.of(
                QuestState.COMPLETED,   // 完成所有目标
                QuestState.EXPIRED,     // 超时
                QuestState.ABANDONED,   // 放弃
                QuestState.FAILED       // 失败
        ));
        
        // COMPLETED 可以转移到 TURNED_IN 或 EXPIRED
        transitions.put(QuestState.COMPLETED, EnumSet.of(
                QuestState.TURNED_IN,   // 提交
                QuestState.EXPIRED      // 超时未提交
        ));
        
        // 终态不能转移
        transitions.put(QuestState.TURNED_IN, EnumSet.noneOf(QuestState.class));
        transitions.put(QuestState.FAILED, EnumSet.noneOf(QuestState.class));
        transitions.put(QuestState.EXPIRED, EnumSet.noneOf(QuestState.class));
        transitions.put(QuestState.ABANDONED, EnumSet.noneOf(QuestState.class));
    }
    
    /**
     * 检查状态转移是否合法
     */
    public boolean canTransition(QuestState from, QuestState to) {
        if (from == null || to == null) return false;
        Set<QuestState> allowed = transitions.get(from);
        return allowed != null && allowed.contains(to);
    }
    
    /**
     * 获取从指定状态可以转移到的所有状态
     */
    public Set<QuestState> getAllowedTransitions(QuestState from) {
        return transitions.getOrDefault(from, EnumSet.noneOf(QuestState.class));
    }
    
    /**
     * 执行状态转移（带验证和钩子）
     * 
     * @param instanceId 委托实例ID
     * @param from 当前状态
     * @param to 目标状态
     * @param context 上下文数据（传递给钩子）
     * @return 转移结果
     */
    public TransitionResult transition(UUID instanceId, QuestState from, QuestState to, Object context) {
        // 验证转移合法性
        if (!canTransition(from, to)) {
            RoadWeaverRPG.LOGGER.warn("Invalid state transition: {} -> {} for instance {}",
                    from, to, instanceId);
            return new TransitionResult(false, "Invalid transition: " + from + " -> " + to, from);
        }
        
        StateTransition transition = new StateTransition(from, to);
        
        // 执行前置钩子
        try {
            executeHooks(beforeHooks.get(transition), instanceId, context);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Before hook failed for transition {} -> {}: {}",
                    from, to, e.getMessage());
            return new TransitionResult(false, "Before hook failed: " + e.getMessage(), from);
        }
        
        // 执行后置钩子
        try {
            executeHooks(afterHooks.get(transition), instanceId, context);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.warn("After hook failed for transition {} -> {}: {}",
                    from, to, e.getMessage());
            // 后置钩子失败不影响状态转移结果
        }
        
        RoadWeaverRPG.LOGGER.debug("State transition: {} -> {} for instance {}", from, to, instanceId);
        return new TransitionResult(true, null, to);
    }
    
    /**
     * 注册状态转移前的钩子
     */
    public void registerBeforeHook(QuestState from, QuestState to, BiConsumer<UUID, Object> hook) {
        StateTransition transition = new StateTransition(from, to);
        beforeHooks.computeIfAbsent(transition, k -> new ArrayList<>()).add(hook);
    }
    
    /**
     * 注册状态转移后的钩子
     */
    public void registerAfterHook(QuestState from, QuestState to, BiConsumer<UUID, Object> hook) {
        StateTransition transition = new StateTransition(from, to);
        afterHooks.computeIfAbsent(transition, k -> new ArrayList<>()).add(hook);
    }
    
    private void executeHooks(List<BiConsumer<UUID, Object>> hooks, UUID instanceId, Object context) {
        if (hooks == null) return;
        for (BiConsumer<UUID, Object> hook : hooks) {
            hook.accept(instanceId, context);
        }
    }
    
    // ==================== 内部类 ====================
    
    /** 状态转移标识 */
    private record StateTransition(QuestState from, QuestState to) {}
    
    /** 状态转移结果 */
    public record TransitionResult(boolean success, String error, QuestState newState) {
        public boolean isSuccess() { return success; }
        public boolean isFailure() { return !success; }
    }
}
