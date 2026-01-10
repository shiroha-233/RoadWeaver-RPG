package net.shiroha233.roadweaverpg.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Forge平台PlatformHelper实现
 */
public class PlatformHelperForge implements PlatformHelper.PlatformHelperImpl {
    
    @Override
    public CompoundTag getEntityPersistentData(LivingEntity entity) {
        return entity.getPersistentData();
    }
}
