package net.shiroha233.roadweaverpg.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fabric平台PlatformHelper实现
 * 
 * 原理：
 * - Fabric没有Forge的getPersistentData()方法
 * - 使用WeakHashMap为每个实体维护一个持久数据标签
 * - WeakHashMap确保实体被GC时数据自动清理，防止内存泄漏
 */
public class PlatformHelperFabric implements PlatformHelper.PlatformHelperImpl {
    
    // 使用WeakHashMap存储实体的持久数据，实体被回收时自动清理
    private static final Map<LivingEntity, CompoundTag> ENTITY_DATA = new WeakHashMap<>();
    
    @Override
    public synchronized CompoundTag getEntityPersistentData(LivingEntity entity) {
        return ENTITY_DATA.computeIfAbsent(entity, e -> new CompoundTag());
    }
}
