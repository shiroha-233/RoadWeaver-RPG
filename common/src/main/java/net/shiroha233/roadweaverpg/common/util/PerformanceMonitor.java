package net.shiroha233.roadweaverpg.common.util;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控工具
 */
public final class PerformanceMonitor {
    
    private static boolean enabled = false;
    private static final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> operationTimes = new ConcurrentHashMap<>();
    
    private PerformanceMonitor() {}
    
    public static void setEnabled(boolean enable) {
        enabled = enable;
        if (!enable) {
            operationCounts.clear();
            operationTimes.clear();
        }
    }
    
    public static boolean isEnabled() { return enabled; }
    
    public static long startTiming() {
        return enabled ? System.nanoTime() : 0;
    }
    
    public static void endTiming(String operation, long startTime) {
        if (!enabled || startTime == 0) return;
        long elapsed = System.nanoTime() - startTime;
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong()).incrementAndGet();
        operationTimes.computeIfAbsent(operation, k -> new AtomicLong()).addAndGet(elapsed);
    }
    
    public static void recordOperation(String operation) {
        if (!enabled) return;
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong()).incrementAndGet();
    }
    
    public static void logStatistics() {
        if (!enabled) return;
        RoadWeaverRPG.LOGGER.info("=== Quest System Performance Statistics ===");
        for (Map.Entry<String, AtomicLong> entry : operationCounts.entrySet()) {
            String op = entry.getKey();
            long count = entry.getValue().get();
            AtomicLong timeAtomic = operationTimes.get(op);
            long totalTime = timeAtomic != null ? timeAtomic.get() : 0;
            double avgMs = count > 0 ? (totalTime / count) / 1_000_000.0 : 0;
            RoadWeaverRPG.LOGGER.info("  {}: {} calls, avg {:.3f}ms", op, count, avgMs);
        }
    }
    
    public static void reset() {
        operationCounts.clear();
        operationTimes.clear();
    }
}
