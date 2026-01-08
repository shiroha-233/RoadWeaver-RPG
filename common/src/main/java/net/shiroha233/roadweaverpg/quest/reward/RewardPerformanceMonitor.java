package net.shiroha233.roadweaverpg.quest.reward;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.config.QuestSystemConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 奖励系统性能监控器
 * 
 * 功能：
 * - 追踪奖励发放性能指标
 * - 检测性能瓶颈
 * - 提供性能报告
 * 
 * 设计原则：
 * - 低开销：使用原子操作，避免锁竞争
 * - 可配置：通过配置开关控制
 * - 线程安全：使用并发集合
 */
public class RewardPerformanceMonitor {
    
    private static volatile RewardPerformanceMonitor instance;
    private static final Object LOCK = new Object();
    
    // 性能指标
    private final AtomicLong totalProcessTime = new AtomicLong(0);
    private final AtomicLong totalProcessCount = new AtomicLong(0);
    private final AtomicLong maxProcessTime = new AtomicLong(0);
    private final AtomicLong minProcessTime = new AtomicLong(Long.MAX_VALUE);
    
    // 按奖励类型统计
    private final ConcurrentHashMap<RewardType, TypeStats> typeStats = new ConcurrentHashMap<>();
    
    // 上次报告时间
    private volatile long lastReportTime = System.currentTimeMillis();
    private static final long REPORT_INTERVAL = 300_000L; // 5分钟
    
    private RewardPerformanceMonitor() {}
    
    public static RewardPerformanceMonitor getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RewardPerformanceMonitor();
                }
            }
        }
        return instance;
    }
    
    /**
     * 记录奖励处理时间
     */
    public void recordProcessTime(RewardType type, long timeMs) {
        if (!QuestSystemConfig.ENABLE_PERFORMANCE_MONITORING) return;
        
        totalProcessTime.addAndGet(timeMs);
        totalProcessCount.incrementAndGet();
        
        // 更新最大/最小值
        updateMax(maxProcessTime, timeMs);
        updateMin(minProcessTime, timeMs);
        
        // 按类型统计
        typeStats.computeIfAbsent(type, k -> new TypeStats()).record(timeMs);
        
        // 定期生成报告
        generateReportIfNeeded();
    }
    
    /**
     * 开始计时
     */
    public long startTiming() {
        return QuestSystemConfig.ENABLE_PERFORMANCE_MONITORING ? System.nanoTime() : 0;
    }
    
    /**
     * 结束计时并记录
     */
    public void endTiming(RewardType type, long startTime) {
        if (!QuestSystemConfig.ENABLE_PERFORMANCE_MONITORING || startTime == 0) return;
        
        long elapsed = (System.nanoTime() - startTime) / 1_000_000; // 转换为毫秒
        recordProcessTime(type, elapsed);
    }
    
    /**
     * 获取性能报告
     */
    public PerformanceReport getReport() {
        long count = totalProcessCount.get();
        if (count == 0) {
            return new PerformanceReport(0, 0, 0, 0, new ConcurrentHashMap<>());
        }
        
        long avgTime = totalProcessTime.get() / count;
        return new PerformanceReport(
                avgTime,
                maxProcessTime.get(),
                minProcessTime.get(),
                count,
                new ConcurrentHashMap<>(typeStats)
        );
    }
    
    /**
     * 重置统计数据
     */
    public void reset() {
        totalProcessTime.set(0);
        totalProcessCount.set(0);
        maxProcessTime.set(0);
        minProcessTime.set(Long.MAX_VALUE);
        typeStats.clear();
        lastReportTime = System.currentTimeMillis();
    }
    
    /**
     * 定期生成性能报告
     */
    private void generateReportIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastReportTime < REPORT_INTERVAL) return;
        
        lastReportTime = now;
        PerformanceReport report = getReport();
        
        if (report.totalCount() > 0) {
            RoadWeaverRPG.LOGGER.info("=== Reward Performance Report ===");
            RoadWeaverRPG.LOGGER.info("Total processed: {}", report.totalCount());
            RoadWeaverRPG.LOGGER.info("Average time: {}ms", report.avgTime());
            RoadWeaverRPG.LOGGER.info("Max time: {}ms", report.maxTime());
            RoadWeaverRPG.LOGGER.info("Min time: {}ms", report.minTime());
            
            report.typeStats().forEach((type, stats) -> {
                RoadWeaverRPG.LOGGER.info("  {}: count={}, avg={}ms", 
                        type, stats.count.get(), stats.getAvgTime());
            });
            RoadWeaverRPG.LOGGER.info("================================");
        }
    }
    
    // 原子更新最大值
    private void updateMax(AtomicLong atomic, long value) {
        long current;
        do {
            current = atomic.get();
            if (value <= current) return;
        } while (!atomic.compareAndSet(current, value));
    }
    
    // 原子更新最小值
    private void updateMin(AtomicLong atomic, long value) {
        long current;
        do {
            current = atomic.get();
            if (value >= current) return;
        } while (!atomic.compareAndSet(current, value));
    }
    
    /**
     * 按类型统计
     */
    private static class TypeStats {
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);
        
        void record(long timeMs) {
            totalTime.addAndGet(timeMs);
            count.incrementAndGet();
        }
        
        long getAvgTime() {
            long c = count.get();
            return c > 0 ? totalTime.get() / c : 0;
        }
    }
    
    /**
     * 性能报告
     */
    public record PerformanceReport(
            long avgTime,
            long maxTime,
            long minTime,
            long totalCount,
            ConcurrentHashMap<RewardType, TypeStats> typeStats
    ) {}
}
