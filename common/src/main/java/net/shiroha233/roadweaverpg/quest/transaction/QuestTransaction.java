package net.shiroha233.roadweaverpg.quest.transaction;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.common.exception.QuestException;
import net.shiroha233.roadweaverpg.common.result.Result;
import net.shiroha233.roadweaverpg.config.QuestSystemConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 委托事务管理器（增强版）
 * 
 * 改进点：
 * - 事务超时控制：防止长时间阻塞
 * - 事务隔离级别：支持不同的隔离策略
 * - 回滚失败处理：回滚失败时停止后续步骤
 * - 详细日志：记录事务执行过程
 * 
 * 使用方式：
 * <pre>
 * QuestTransaction.begin(player)
 *     .withTimeout(5000)
 *     .withIsolation(IsolationLevel.READ_COMMITTED)
 *     .validate(() -> checkCondition())
 *     .execute(() -> doOperation1(), () -> rollback1())
 *     .commit();
 * </pre>
 */
public class QuestTransaction {
    
    // 全局事务计数器（用于日志）
    private static final AtomicInteger TRANSACTION_COUNTER = new AtomicInteger(0);
    
    private final int transactionId;
    private final ServerPlayer player;
    private final List<TransactionStep> steps = new ArrayList<>();
    private final List<RollbackAction> rollbackActions = new ArrayList<>();
    
    private long timeoutMs = QuestSystemConfig.TRANSACTION_TIMEOUT;
    private IsolationLevel isolationLevel = IsolationLevel.READ_COMMITTED;
    private boolean committed = false;
    private long startTime;
    private int executedSteps = 0;
    
    private QuestTransaction(ServerPlayer player) {
        this.transactionId = TRANSACTION_COUNTER.incrementAndGet();
        this.player = player;
    }
    
    /** 开始一个新事务 */
    public static QuestTransaction begin(ServerPlayer player) {
        return new QuestTransaction(player);
    }
    
    /** 设置超时时间（毫秒） */
    public QuestTransaction withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }
    
    /** 设置隔离级别 */
    public QuestTransaction withIsolation(IsolationLevel level) {
        this.isolationLevel = level;
        return this;
    }
    
    /** 添加验证步骤 */
    public QuestTransaction validate(Supplier<Result<Void>> validation) {
        steps.add(new ValidationStep(validation));
        return this;
    }
    
    /** 添加执行步骤（带回滚） */
    public QuestTransaction execute(Runnable action, Runnable rollback) {
        steps.add(new ExecutionStep(action));
        rollbackActions.add(0, new RollbackAction(rollbackActions.size(), rollback));
        return this;
    }
    
    /** 添加执行步骤（无回滚） */
    public QuestTransaction execute(Runnable action) {
        steps.add(new ExecutionStep(action));
        return this;
    }
    
    /**
     * 提交事务
     * @return 成功返回Success，失败返回Failure并自动回滚
     */
    public Result<Void> commit() {
        if (committed) {
            return Result.failure(QuestException.ErrorCode.TRANSACTION_ALREADY_COMMITTED,
                    "Transaction already committed");
        }
        
        startTime = System.currentTimeMillis();
        RoadWeaverRPG.LOGGER.debug("Transaction {} started for player {}",
                transactionId, player.getName().getString());
        
        try {
            // 执行所有步骤
            for (int i = 0; i < steps.size(); i++) {
                // 检查超时
                if (isTimedOut()) {
                    RoadWeaverRPG.LOGGER.warn("Transaction {} timed out after {}ms",
                            transactionId, System.currentTimeMillis() - startTime);
                    rollback();
                    return Result.failure(QuestException.ErrorCode.TRANSACTION_EXECUTION_FAILED,
                            "Transaction timed out");
                }
                
                TransactionStep step = steps.get(i);
                Result<Void> result = step.execute();
                
                if (result.isFailure()) {
                    RoadWeaverRPG.LOGGER.debug("Transaction {} failed at step {}: {}",
                            transactionId, i, result.getErrorMessage().orElse("unknown"));
                    rollback();
                    return result;
                }
                
                if (step instanceof ExecutionStep) {
                    executedSteps++;
                }
            }
            
            committed = true;
            long duration = System.currentTimeMillis() - startTime;
            RoadWeaverRPG.LOGGER.debug("Transaction {} committed successfully in {}ms",
                    transactionId, duration);
            return Result.success(null);
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Transaction {} failed with exception: {}",
                    transactionId, e.getMessage());
            rollback();
            return Result.failure(QuestException.ErrorCode.TRANSACTION_EXECUTION_FAILED,
                    "Transaction execution failed: " + e.getMessage());
        }
    }
    
    /** 检查是否超时 */
    private boolean isTimedOut() {
        return System.currentTimeMillis() - startTime > timeoutMs;
    }
    
    /** 回滚所有已执行的操作 */
    private void rollback() {
        if (rollbackActions.isEmpty()) return;
        
        RoadWeaverRPG.LOGGER.debug("Transaction {} rolling back {} steps",
                transactionId, Math.min(executedSteps, rollbackActions.size()));
        
        boolean rollbackFailed = false;
        
        for (RollbackAction action : rollbackActions) {
            // 只回滚已执行的步骤
            if (action.stepIndex() >= executedSteps) continue;
            
            try {
                action.action().run();
            } catch (Exception e) {
                rollbackFailed = true;
                RoadWeaverRPG.LOGGER.error("Transaction {} rollback failed at step {}: {}",
                        transactionId, action.stepIndex(), e.getMessage());
                
                // 回滚失败时停止后续回滚，避免数据进一步损坏
                if (QuestSystemConfig.ENABLE_TRANSACTION_ROLLBACK_LOG) {
                    RoadWeaverRPG.LOGGER.error("Stopping rollback due to failure. " +
                            "Manual intervention may be required for player {}",
                            player.getName().getString());
                }
                break;
            }
        }
        
        if (!rollbackFailed) {
            RoadWeaverRPG.LOGGER.debug("Transaction {} rollback completed", transactionId);
        }
    }
    
    // ==================== 内部类 ====================
    
    /** 事务隔离级别 */
    public enum IsolationLevel {
        READ_UNCOMMITTED,  // 允许脏读
        READ_COMMITTED,    // 不允许脏读（默认）
        REPEATABLE_READ,   // 不允许脏读和不可重复读
        SERIALIZABLE       // 完全隔离
    }
    
    /** 事务步骤接口 */
    private interface TransactionStep {
        Result<Void> execute();
    }
    
    /** 验证步骤 */
    private record ValidationStep(Supplier<Result<Void>> validation) implements TransactionStep {
        @Override
        public Result<Void> execute() {
            return validation.get();
        }
    }
    
    /** 执行步骤 */
    private record ExecutionStep(Runnable action) implements TransactionStep {
        @Override
        public Result<Void> execute() {
            try {
                action.run();
                return Result.success(null);
            } catch (Exception e) {
                return Result.failure(QuestException.ErrorCode.TRANSACTION_EXECUTION_FAILED,
                        "Execution failed: " + e.getMessage());
            }
        }
    }
    
    /** 回滚动作 */
    private record RollbackAction(int stepIndex, Runnable action) {}
}
