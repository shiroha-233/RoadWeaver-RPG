package net.shiroha233.roadweaverpg.api;

import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.worlddifficulty.MonsterRarity;
import net.shiroha233.roadweaverpg.worlddifficulty.compat.MonsterDataProvider;

import java.util.Optional;

/**
 * 世界难度系统公开API
 * 
 * 设计原理：
 * - 为外部模组（如Jade、WAILA）提供统一接口
 * - 隐藏内部实现细节
 * - 稳定的API契约
 */
public final class WorldDifficultyAPI {
    
    private WorldDifficultyAPI() {}
    
    /**
     * 检查实体是否有难度数据
     */
    public static boolean hasMonsterData(LivingEntity entity) {
        return MonsterDataProvider.hasData(entity);
    }
    
    /**
     * 获取怪物等级
     */
    public static Optional<Integer> getMonsterLevel(LivingEntity entity) {
        return MonsterDataProvider.getLevel(entity);
    }
    
    /**
     * 获取怪物稀有度ID
     */
    public static Optional<String> getMonsterRarityId(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity).map(MonsterRarity::getId);
    }
    
    /**
     * 获取怪物稀有度显示名称
     */
    public static Optional<String> getMonsterRarityName(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(r -> r.getDisplayName().getString());
    }
    
    /**
     * 获取怪物稀有度颜色代码
     */
    public static Optional<Integer> getMonsterRarityColor(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(r -> r.getColor().getColor());
    }
    
    /**
     * 检查是否为精英怪物
     */
    public static boolean isElite(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(r -> r == MonsterRarity.ELITE)
                .orElse(false);
    }
    
    /**
     * 检查是否为首领怪物
     */
    public static boolean isBoss(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(r -> r == MonsterRarity.BOSS)
                .orElse(false);
    }
    
    /**
     * 检查是否为传说怪物
     */
    public static boolean isLegendary(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(r -> r == MonsterRarity.LEGENDARY)
                .orElse(false);
    }
    
    /**
     * 获取怪物属性倍率
     */
    public static float getStatMultiplier(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(MonsterRarity::getStatMultiplier)
                .orElse(1.0f);
    }
    
    /**
     * 获取怪物掉落倍率
     */
    public static float getDropMultiplier(LivingEntity entity) {
        return MonsterDataProvider.getRarity(entity)
                .map(MonsterRarity::getDropMultiplier)
                .orElse(1.0f);
    }
}
