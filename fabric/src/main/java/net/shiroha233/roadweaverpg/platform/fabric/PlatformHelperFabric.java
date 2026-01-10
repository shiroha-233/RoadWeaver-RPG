package net.shiroha233.roadweaverpg.platform.fabric;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.platform.PlatformHelper;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fabric平台PlatformHelper实现
 * 使用WeakHashMap为实体维护持久数据，实体被GC时自动清理
 */
public class PlatformHelperFabric implements PlatformHelper.PlatformHelperImpl {
    
    private static final Map<LivingEntity, CompoundTag> ENTITY_DATA = new WeakHashMap<>();
    
    @Override
    public synchronized CompoundTag getEntityPersistentData(LivingEntity entity) {
        return ENTITY_DATA.computeIfAbsent(entity, e -> new CompoundTag());
    }
}
