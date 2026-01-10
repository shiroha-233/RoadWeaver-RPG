package net.shiroha233.roadweaverpg.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家属性分配数据
 * 存储玩家的技能点分配情况
 * 线程安全设计
 */
public class StatAllocationData {
    
    // 可用技能点
    private final AtomicInteger availablePoints = new AtomicInteger(0);
    
    // 已分配的点数（每个属性）
    private final Map<StatType, AtomicInteger> allocatedPoints = new EnumMap<>(StatType.class);
    
    // 职业解锁的可加点属性（为空时使用默认规则）
    private volatile Set<StatType> unlockedStats = null;
    
    public StatAllocationData() {
        // 初始化所有可分配属性的点数为0
        for (StatType type : StatType.values()) {
            if (type.isAllocatable()) {
                allocatedPoints.put(type, new AtomicInteger(0));
            }
        }
    }
    
    // region 可加点属性管理
    
    /**
     * 设置职业解锁的可加点属性
     */
    public void setUnlockedStats(Set<StatType> stats) {
        this.unlockedStats = stats != null ? EnumSet.copyOf(stats) : null;
    }
    
    /**
     * 获取可加点属性列表
     */
    public Set<StatType> getUnlockedStats() {
        return unlockedStats;
    }
    
    /**
     * 检查属性是否可加点
     */
    public boolean isStatUnlocked(StatType type) {
        if (!type.isAllocatable()) return false;
        if (unlockedStats == null) return true; // 未设置限制时使用默认规则
        return unlockedStats.contains(type);
    }
    
    // endregion
    
    // ==================== 技能点操作 ====================
    
    public int getAvailablePoints() {
        return availablePoints.get();
    }
    
    public void setAvailablePoints(int points) {
        availablePoints.set(Math.max(0, points));
    }
    
    public void addAvailablePoints(int points) {
        availablePoints.addAndGet(points);
    }
    
    /**
     * 分配一点到指定属性
     * @return 是否分配成功
     */
    public synchronized boolean allocatePoint(StatType type) {
        if (!isStatUnlocked(type)) return false;
        if (availablePoints.get() <= 0) return false;
        
        availablePoints.decrementAndGet();
        allocatedPoints.get(type).incrementAndGet();
        return true;
    }
    
    /**
     * 从指定属性减少一点（返还技能点）
     * @return 是否减少成功
     */
    public synchronized boolean deallocatePoint(StatType type) {
        if (!isStatUnlocked(type)) return false;
        
        AtomicInteger points = allocatedPoints.get(type);
        if (points == null || points.get() <= 0) return false;
        
        points.decrementAndGet();
        availablePoints.incrementAndGet();
        return true;
    }
    
    /**
     * 获取指定属性已分配的点数
     */
    public int getAllocatedPoints(StatType type) {
        AtomicInteger points = allocatedPoints.get(type);
        return points != null ? points.get() : 0;
    }
    
    /**
     * 设置指定属性的分配点数
     */
    public void setAllocatedPoints(StatType type, int points) {
        AtomicInteger current = allocatedPoints.get(type);
        if (current != null) {
            current.set(Math.max(0, points));
        }
    }
    
    /**
     * 重置所有分配（返还技能点）
     */
    public synchronized void resetAllocation() {
        int totalRefund = 0;
        for (Map.Entry<StatType, AtomicInteger> entry : allocatedPoints.entrySet()) {
            totalRefund += entry.getValue().getAndSet(0);
        }
        availablePoints.addAndGet(totalRefund);
    }
    
    /**
     * 获取已分配的总点数
     */
    public int getTotalAllocatedPoints() {
        int total = 0;
        for (AtomicInteger points : allocatedPoints.values()) {
            total += points.get();
        }
        return total;
    }
    
    // ==================== NBT序列化 ====================
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("availablePoints", availablePoints.get());
        
        CompoundTag allocatedTag = new CompoundTag();
        for (Map.Entry<StatType, AtomicInteger> entry : allocatedPoints.entrySet()) {
            allocatedTag.putInt(entry.getKey().getId(), entry.getValue().get());
        }
        tag.put("allocated", allocatedTag);
        
        // 保存可加点属性限制
        if (unlockedStats != null) {
            ListTag unlockedTag = new ListTag();
            for (StatType type : unlockedStats) {
                unlockedTag.add(StringTag.valueOf(type.getId()));
            }
            tag.put("unlockedStats", unlockedTag);
        }
        
        return tag;
    }
    
    public static StatAllocationData fromNbt(CompoundTag tag) {
        StatAllocationData data = new StatAllocationData();
        
        if (tag.contains("availablePoints")) {
            data.availablePoints.set(tag.getInt("availablePoints"));
        }
        
        if (tag.contains("allocated")) {
            CompoundTag allocatedTag = tag.getCompound("allocated");
            for (String key : allocatedTag.getAllKeys()) {
                StatType type = StatType.fromId(key);
                if (type != null && type.isAllocatable()) {
                    data.allocatedPoints.get(type).set(allocatedTag.getInt(key));
                }
            }
        }
        
        // 加载可加点属性限制
        if (tag.contains("unlockedStats")) {
            ListTag unlockedTag = tag.getList("unlockedStats", Tag.TAG_STRING);
            Set<StatType> unlocked = EnumSet.noneOf(StatType.class);
            for (int i = 0; i < unlockedTag.size(); i++) {
                StatType type = StatType.fromId(unlockedTag.getString(i));
                if (type != null) {
                    unlocked.add(type);
                }
            }
            data.unlockedStats = unlocked;
        }
        
        return data;
    }
}
