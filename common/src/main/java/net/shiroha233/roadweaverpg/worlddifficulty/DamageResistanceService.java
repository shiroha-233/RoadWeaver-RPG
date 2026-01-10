package net.shiroha233.roadweaverpg.worlddifficulty;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 伤害抗性计算服务
 * 
 * 设计原理：
 * - 单一职责：只负责计算伤害减免
 * - 无状态设计，线程安全
 * - 区分物理和魔法伤害
 */
public final class DamageResistanceService {
    
    private DamageResistanceService() {}
    
    /**
     * 计算经过抗性减免后的伤害
     * 
     * @param entity 受伤实体
     * @param source 伤害来源
     * @param originalDamage 原始伤害
     * @return 减免后的伤害
     */
    public static float calculateReducedDamage(LivingEntity entity, DamageSource source, float originalDamage) {
        MonsterData data = MonsterData.fromEntity(entity);
        if (data == null || !data.isScaled()) {
            return originalDamage;
        }
        
        MonsterStats stats = data.stats();
        if (stats == null) {
            return originalDamage;
        }
        
        // 判断伤害类型
        boolean isMagic = isMagicDamage(source);
        float resistance = isMagic ? stats.magicResistance() : stats.physicalResistance();
        
        // 限制最大抗性为80%
        resistance = Math.min(0.8f, resistance);
        
        float reducedDamage = originalDamage * (1.0f - resistance);
        
        RoadWeaverRPG.LOGGER.debug("伤害减免: {} -> {} ({}抗性: {}%)", 
                originalDamage, reducedDamage, isMagic ? "魔法" : "物理", resistance * 100);
        
        return reducedDamage;
    }
    
    /**
     * 判断是否为魔法伤害
     */
    public static boolean isMagicDamage(DamageSource source) {
        // 魔法伤害类型
        if (source.is(DamageTypes.MAGIC) || 
            source.is(DamageTypes.INDIRECT_MAGIC) ||
            source.is(DamageTypes.DRAGON_BREATH) ||
            source.is(DamageTypes.WITHER) ||
            source.is(DamageTypes.SONIC_BOOM)) {
            return true;
        }
        
        // 检查伤害源名称（兼容其他魔法模组）
        String msgId = source.getMsgId();
        return msgId.contains("magic") || 
               msgId.contains("spell") || 
               msgId.contains("arcane") ||
               msgId.contains("elemental");
    }
    
    /**
     * 判断是否为物理伤害
     */
    public static boolean isPhysicalDamage(DamageSource source) {
        return !isMagicDamage(source) && !isEnvironmentalDamage(source);
    }
    
    /**
     * 判断是否为环境伤害（不受抗性影响）
     */
    public static boolean isEnvironmentalDamage(DamageSource source) {
        return source.is(DamageTypes.FALL) ||
               source.is(DamageTypes.DROWN) ||
               source.is(DamageTypes.IN_FIRE) ||
               source.is(DamageTypes.ON_FIRE) ||
               source.is(DamageTypes.LAVA) ||
               source.is(DamageTypes.IN_WALL) ||
               source.is(DamageTypes.STARVE) ||
               source.is(DamageTypes.OUTSIDE_BORDER);
    }
}
