package net.shiroha233.roadweaverpg.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * 平台抽象层助手类
 * 
 * 设计原理：
 * - 隐藏平台特定的API差异
 * - 提供统一的接口供common模块使用
 * - 由平台特定代码实现
 */
public final class PlatformHelper {
    
    private static PlatformHelperImpl impl;
    
    private PlatformHelper() {}
    
    /**
     * 设置平台实现
     */
    public static void setImplementation(PlatformHelperImpl implementation) {
        impl = implementation;
    }
    
    /**
     * 获取实体的持久化数据NBT
     */
    public static CompoundTag getEntityPersistentData(LivingEntity entity) {
        if (impl == null) {
            throw new IllegalStateException("PlatformHelper not initialized");
        }
        return impl.getEntityPersistentData(entity);
    }
    
    /**
     * 平台实现接口
     */
    public interface PlatformHelperImpl {
        CompoundTag getEntityPersistentData(LivingEntity entity);
    }
}
