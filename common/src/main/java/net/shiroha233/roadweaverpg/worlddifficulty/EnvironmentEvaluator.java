package net.shiroha233.roadweaverpg.worlddifficulty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaverpg.worlddifficulty.EnvironmentBonus.EnvironmentCondition;

import java.util.List;

/**
 * 环境条件评估器
 * 
 * 设计原理：
 * - 单一职责：只负责评估环境条件
 * - 无状态设计，线程安全
 * - 支持多种环境条件类型
 */
public final class EnvironmentEvaluator {
    
    private EnvironmentEvaluator() {}
    
    /**
     * 评估环境条件是否满足
     */
    public static boolean evaluate(EnvironmentCondition condition, LivingEntity entity) {
        if (condition == null || entity == null) return false;
        
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        
        return switch (condition.type()) {
            case ALWAYS -> true;
            case WEATHER -> evaluateWeather(condition, level);
            case TIME -> evaluateTime(condition, level);
            case BIOME -> evaluateBiome(condition, level, pos);
            case BIOME_TAG -> evaluateBiomeTag(condition, level, pos);
            case DIMENSION -> evaluateDimension(condition, level);
            case UNDERGROUND -> evaluateUnderground(level, pos);
        };
    }
    
    /**
     * 计算实体的环境加成
     */
    public static MonsterStats.Builder calculateEnvironmentBonuses(
            LivingEntity entity, 
            List<EnvironmentBonus> bonuses
    ) {
        MonsterStats.Builder builder = MonsterStats.builder();
        
        float totalArmor = 0, totalHealth = 0, totalDamage = 0, totalResist = 0;
        
        for (EnvironmentBonus bonus : bonuses) {
            if (evaluate(bonus.condition(), entity)) {
                totalArmor += bonus.armorBonus();
                totalHealth += bonus.healthBonus();
                totalDamage += bonus.damageBonus();
                totalResist += bonus.resistanceBonus();
            }
        }
        
        return builder
                .envArmor(totalArmor)
                .envHealth(totalHealth)
                .envDamage(totalDamage)
                .envResistance(totalResist);
    }
    
    private static boolean evaluateWeather(EnvironmentCondition condition, Level level) {
        String value = condition.value();
        if (value == null) return false;
        
        return switch (value.toLowerCase()) {
            case "clear" -> !level.isRaining() && !level.isThundering();
            case "rain", "raining" -> level.isRaining() && !level.isThundering();
            case "thunder", "thundering" -> level.isThundering();
            default -> false;
        };
    }
    
    private static boolean evaluateTime(EnvironmentCondition condition, Level level) {
        String value = condition.value();
        if (value == null) return false;
        
        long dayTime = level.getDayTime() % 24000;
        
        return switch (value.toLowerCase()) {
            case "day" -> dayTime >= 0 && dayTime < 12000;
            case "night" -> dayTime >= 13000 && dayTime < 23000;
            case "dawn" -> dayTime >= 23000 || dayTime < 1000;
            case "dusk" -> dayTime >= 11000 && dayTime < 13000;
            default -> false;
        };
    }
    
    private static boolean evaluateBiome(EnvironmentCondition condition, Level level, BlockPos pos) {
        String value = condition.value();
        List<String> values = condition.values();
        
        Holder<Biome> biomeHolder = level.getBiome(pos);
        ResourceLocation biomeId = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(biomeHolder.value());
        
        if (biomeId == null) return false;
        
        // 检查单个值
        if (value != null && biomeId.toString().equals(value)) {
            return true;
        }
        
        // 检查值列表
        String biomeStr = biomeId.toString();
        for (String v : values) {
            if (biomeStr.equals(v)) return true;
        }
        
        return false;
    }
    
    private static boolean evaluateBiomeTag(EnvironmentCondition condition, Level level, BlockPos pos) {
        String value = condition.value();
        if (value == null) return false;
        
        Holder<Biome> biomeHolder = level.getBiome(pos);
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, new ResourceLocation(value));
        
        return biomeHolder.is(tag);
    }
    
    private static boolean evaluateDimension(EnvironmentCondition condition, Level level) {
        String value = condition.value();
        List<String> values = condition.values();
        
        ResourceLocation dimId = level.dimension().location();
        String dimStr = dimId.toString();
        
        if (value != null && dimStr.equals(value)) {
            return true;
        }
        
        for (String v : values) {
            if (dimStr.equals(v)) return true;
        }
        
        return false;
    }
    
    private static boolean evaluateUnderground(Level level, BlockPos pos) {
        return !level.canSeeSky(pos) && pos.getY() < level.getSeaLevel();
    }
}
