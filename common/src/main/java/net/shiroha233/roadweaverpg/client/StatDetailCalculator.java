package net.shiroha233.roadweaverpg.client;

import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry;
import net.shiroha233.roadweaverpg.stats.StatType;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 属性详情计算器 - 简化版
 * 
 * 设计原则：
 * - 只从ClientStatsCache读取当前值
 * - 不再分解属性来源，避免数据不一致
 */
public final class StatDetailCalculator {
    
    private StatDetailCalculator() {}
    
    /**
     * 获取属性tooltip
     */
    public static List<Component> getStatDetails(StatType type) {
        List<Component> details = new ArrayList<>();
        boolean hasMagicMod = MagicModCompatRegistry.hasAnyMagicMod();
        
        // 魔法属性在没有魔法模组时显示特殊提示
        if (type.requiresMagicMod() && !hasMagicMod) {
            details.add(Component.translatable(type.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
            details.add(Component.translatable("tooltip.roadweaver_rpg.stat.requires_magic_mod")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return details;
        }
        
        // 标题
        details.add(Component.translatable(type.getTranslationKey())
                .withStyle(style -> style.withColor(type.getColor()).withBold(true)));
        
        // 当前值
        double value = ClientStatsCache.getValue(type);
        String valueStr = formatValue(type, value);
        details.add(Component.translatable("tooltip.roadweaver_rpg.stat.current")
                .append(": ")
                .append(Component.literal(valueStr).withStyle(ChatFormatting.WHITE)));
        
        // 已分配技能点
        if (type.isAllocatable()) {
            int points = ClientStatAllocationCache.getAllocatedPoints(type);
            if (points > 0) {
                details.add(Component.translatable("tooltip.roadweaver_rpg.stat.allocated_points", points)
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        
        return details;
    }
    
    private static String formatValue(StatType type, double value) {
        if (isPercentageStat(type)) {
            return String.format("%.1f%%", value);
        }
        return String.format("%.1f", value);
    }
    
    private static boolean isPercentageStat(StatType type) {
        return type == StatType.CRIT_RATE || type == StatType.CRIT_DAMAGE ||
               type == StatType.HIT_RATE || type == StatType.DODGE_RATE ||
               type == StatType.ATTACK_COOLDOWN || type == StatType.MOVE_SPEED ||
               type == StatType.LIFE_STEAL || type == StatType.MANA_STEAL ||
               type == StatType.COOLDOWN_REDUCTION || type == StatType.EXP_BONUS ||
               type == StatType.DROP_BONUS;
    }
}
