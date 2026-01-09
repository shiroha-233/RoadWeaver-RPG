package net.shiroha233.roadweaverpg.client;

import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端属性分配数据缓存
 * 存储从服务端同步的技能点分配数据
 * 使用volatile和同步保证线程安全
 */
public final class ClientStatAllocationCache {
    
    private static final AtomicInteger availablePoints = new AtomicInteger(0);
    private static final Map<StatType, Integer> allocatedPoints = new EnumMap<>(StatType.class);
    private static final Object LOCK = new Object();
    
    private ClientStatAllocationCache() {}
    
    /**
     * 更新缓存数据（线程安全）
     */
    public static void update(int available, Map<StatType, Integer> allocated) {
        synchronized (LOCK) {
            availablePoints.set(available);
            allocatedPoints.clear();
            allocatedPoints.putAll(allocated);
        }
    }
    
    /**
     * 获取可用技能点
     */
    public static int getAvailablePoints() {
        return availablePoints.get();
    }
    
    /**
     * 获取指定属性已分配的点数
     */
    public static int getAllocatedPoints(StatType type) {
        synchronized (LOCK) {
            return allocatedPoints.getOrDefault(type, 0);
        }
    }
    
    /**
     * 获取已分配的总点数
     */
    public static int getTotalAllocatedPoints() {
        synchronized (LOCK) {
            int total = 0;
            for (int points : allocatedPoints.values()) {
                total += points;
            }
            return total;
        }
    }
    
    /**
     * 清理缓存
     */
    public static void clear() {
        synchronized (LOCK) {
            availablePoints.set(0);
            allocatedPoints.clear();
        }
    }
}
