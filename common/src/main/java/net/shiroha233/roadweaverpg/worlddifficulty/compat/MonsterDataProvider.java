package net.shiroha233.roadweaverpg.worlddifficulty.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.worlddifficulty.MonsterData;
import net.shiroha233.roadweaverpg.worlddifficulty.MonsterRarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 怪物数据提供器
 * 
 * 设计原理：
 * - 为Jade/WAILA等模组提供统一的数据接口
 * - 不直接依赖Jade API，通过抽象接口解耦
 * - 支持多种显示模组
 */
public final class MonsterDataProvider {
    
    private MonsterDataProvider() {}
    
    /**
     * 获取怪物的显示信息
     * 返回格式化的文本组件列表，供Jade等模组显示
     */
    public static List<Component> getDisplayInfo(LivingEntity entity) {
        List<Component> lines = new ArrayList<>();
        
        MonsterData data = MonsterData.fromEntity(entity);
        if (data == null || !data.isScaled()) {
            return lines;
        }
        
        // 等级和稀有度
        MutableComponent levelLine = Component.translatable(
                "tooltip.roadweaver_rpg.monster.level", data.level());
        
        MonsterRarity rarity = data.rarity();
        if (rarity != MonsterRarity.NORMAL) {
            levelLine.append(" ").append(rarity.getDisplayName());
        }
        lines.add(levelLine);
        
        return lines;
    }
    
    /**
     * 获取怪物等级
     */
    public static Optional<Integer> getLevel(LivingEntity entity) {
        MonsterData data = MonsterData.fromEntity(entity);
        if (data != null && data.isScaled()) {
            return Optional.of(data.level());
        }
        return Optional.empty();
    }
    
    /**
     * 获取怪物稀有度
     */
    public static Optional<MonsterRarity> getRarity(LivingEntity entity) {
        MonsterData data = MonsterData.fromEntity(entity);
        if (data != null && data.isScaled()) {
            return Optional.of(data.rarity());
        }
        return Optional.empty();
    }
    
    /**
     * 检查实体是否有难度数据
     */
    public static boolean hasData(LivingEntity entity) {
        MonsterData data = MonsterData.fromEntity(entity);
        return data != null && data.isScaled();
    }
    
    /**
     * 获取格式化的等级文本（用于名称显示）
     */
    public static Component getFormattedLevel(LivingEntity entity) {
        MonsterData data = MonsterData.fromEntity(entity);
        if (data == null || !data.isScaled()) {
            return Component.empty();
        }
        
        MonsterRarity rarity = data.rarity();
        return Component.literal("Lv." + data.level() + " ")
                .withStyle(rarity.getColor());
    }
    
    /**
     * 获取完整的怪物名称（包含等级和稀有度）
     */
    public static Component getFullName(LivingEntity entity) {
        MonsterData data = MonsterData.fromEntity(entity);
        if (data == null || !data.isScaled()) {
            return entity.getDisplayName();
        }
        
        MonsterRarity rarity = data.rarity();
        MutableComponent name = Component.literal("Lv." + data.level() + " ")
                .withStyle(rarity.getColor());
        
        if (rarity != MonsterRarity.NORMAL) {
            name.append("[").append(rarity.getDisplayName()).append("] ");
        }
        
        name.append(entity.getDisplayName());
        return name;
    }
}
