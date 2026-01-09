package net.shiroha233.roadweaverpg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端属性详情计算器
 * 计算并显示属性的各个来源（基础值、装备、技能点等）
 */
public final class StatDetailCalculator {
    
    private StatDetailCalculator() {}
    
    /**
     * 获取属性的详细来源列表（用于tooltip显示）
     */
    public static List<Component> getStatDetails(StatType type) {
        List<Component> details = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return details;
        
        Player player = mc.player;
        int allocatedPoints = ClientStatAllocationCache.getAllocatedPoints(type);
        double bonusPerPoint = getBonusPerPoint(type);
        double skillBonus = allocatedPoints * bonusPerPoint;
        
        // 标题
        details.add(Component.translatable(type.getTranslationKey())
                .withStyle(style -> style.withColor(type.getColor()).withBold(true)));
        details.add(Component.empty());
        
        switch (type) {
            case MAX_HEALTH -> addHealthDetails(details, player, skillBonus, allocatedPoints, bonusPerPoint);
            case ATTACK -> addAttackDetails(details, player, skillBonus, allocatedPoints, bonusPerPoint);
            case DEFENSE -> addDefenseDetails(details, player, skillBonus, allocatedPoints, bonusPerPoint);
            case MAGIC_ATTACK -> addMagicAttackDetails(details, skillBonus, allocatedPoints, bonusPerPoint);
            case MAGIC_DEFENSE -> addMagicDefenseDetails(details, player, skillBonus, allocatedPoints, bonusPerPoint);
            case MAX_MANA -> addManaDetails(details, skillBonus, allocatedPoints, bonusPerPoint);
            default -> addGenericDetails(details, type, skillBonus, allocatedPoints, bonusPerPoint);
        }
        
        return details;
    }
    
    private static void addHealthDetails(List<Component> details, Player player, 
            double skillBonus, int points, double perPoint) {
        double total = player.getMaxHealth();
        double base = 20.0;
        // 装备加成 = 总值 - 基础值 - 技能点加成
        double equipment = total - base - skillBonus;
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", base));
        if (Math.abs(equipment) > 0.01) {
            details.add(formatLine("tooltip.roadweaver_rpg.stat.equipment", equipment));
        }
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total));
    }
    
    private static void addAttackDetails(List<Component> details, Player player, 
            double skillBonus, int points, double perPoint) {
        double base = 1.0;
        double weapon = 0.0;
        
        // 从主手物品获取武器攻击力加成
        var mainHandItem = player.getMainHandItem();
        if (!mainHandItem.isEmpty()) {
            var modifiers = mainHandItem.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            var attackModifiers = modifiers.get(Attributes.ATTACK_DAMAGE);
            for (var modifier : attackModifiers) {
                if (modifier.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) {
                    weapon += modifier.getAmount();
                }
            }
        }
        
        double total = base + weapon + skillBonus;
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", base));
        if (Math.abs(weapon) > 0.01) {
            details.add(formatLine("tooltip.roadweaver_rpg.stat.weapon", weapon));
        }
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total));
    }
    
    private static void addDefenseDetails(List<Component> details, Player player, 
            double skillBonus, int points, double perPoint) {
        double total = player.getArmorValue();
        // 护甲加成 = 总值 - 技能点加成
        double armor = total - skillBonus;
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", 0));
        if (Math.abs(armor) > 0.01) {
            details.add(formatLine("tooltip.roadweaver_rpg.stat.armor", armor));
        }
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total));
    }
    
    private static void addMagicAttackDetails(List<Component> details, 
            double skillBonus, int points, double perPoint) {
        double base = 1.0;
        double total = base + skillBonus;
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", base));
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total));
    }
    
    private static void addMagicDefenseDetails(List<Component> details, Player player, 
            double skillBonus, int points, double perPoint) {
        double total = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double armorToughness = total - skillBonus;
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", 0));
        if (Math.abs(armorToughness) > 0.01) {
            details.add(formatLine("tooltip.roadweaver_rpg.stat.armor_toughness", armorToughness));
        }
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total));
    }
    
    private static void addManaDetails(List<Component> details, 
            double skillBonus, int points, double perPoint) {
        double base = 100.0;
        double total = base + skillBonus;
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", base));
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total));
    }
    
    private static void addGenericDetails(List<Component> details, StatType type, 
            double skillBonus, int points, double perPoint) {
        double base = getDefaultValue(type);
        double total = base + skillBonus;
        boolean isPercent = isPercentageStat(type);
        
        details.add(formatLine("tooltip.roadweaver_rpg.stat.base", base, isPercent));
        if (points > 0) {
            details.add(formatSkillLine(points, perPoint, skillBonus, isPercent));
        }
        details.add(Component.empty());
        details.add(formatTotalLine(total, isPercent));
    }
    
    // ==================== 格式化方法 ====================
    
    private static Component formatLine(String key, double value) {
        return formatLine(key, value, false);
    }
    
    private static Component formatLine(String key, double value, boolean isPercent) {
        String valueStr = isPercent ? String.format("%.1f%%", value) : String.format("%.1f", value);
        return Component.translatable(key).append(": ")
                .append(Component.literal(valueStr).withStyle(style -> style.withColor(0xAAAAAA)));
    }
    
    private static Component formatSkillLine(int points, double perPoint, double total) {
        return formatSkillLine(points, perPoint, total, false);
    }
    
    private static Component formatSkillLine(int points, double perPoint, double total, boolean isPercent) {
        String perPointStr = isPercent ? String.format("%.1f%%", perPoint) : String.format("%.1f", perPoint);
        String totalStr = isPercent ? String.format("+%.1f%%", total) : String.format("+%.1f", total);
        return Component.translatable("tooltip.roadweaver_rpg.stat.skill_points", points, perPointStr)
                .append(" = ")
                .append(Component.literal(totalStr).withStyle(style -> style.withColor(0x44FF44)));
    }
    
    private static Component formatTotalLine(double total) {
        return formatTotalLine(total, false);
    }
    
    private static Component formatTotalLine(double total, boolean isPercent) {
        String valueStr = isPercent ? String.format("%.1f%%", total) : String.format("%.1f", total);
        return Component.translatable("tooltip.roadweaver_rpg.stat.total")
                .withStyle(style -> style.withColor(0xFFD700).withBold(true))
                .append(": ")
                .append(Component.literal(valueStr).withStyle(style -> style.withColor(0xFFFFFF).withBold(false)));
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取每点技能点的加成值
     * 优先从配置读取，配置未初始化时使用默认值
     */
    private static double getBonusPerPoint(StatType type) {
        // 尝试从配置获取
        if (net.shiroha233.roadweaverpg.stats.StatAllocationConfig.isInitialized()) {
            return net.shiroha233.roadweaverpg.stats.StatAllocationConfig.getInstance().getBonusPerPoint(type);
        }
        // 默认值（与StatAllocationConfig.DEFAULT_BONUS保持一致）
        return switch (type) {
            case MAX_HEALTH -> 5.0;
            case MAX_MANA -> 10.0;
            case ATTACK, DEFENSE, MAGIC_ATTACK, MAGIC_DEFENSE -> 1.0;
            case CRIT_RATE, DODGE_RATE, MOVE_SPEED -> 0.5;
            case CRIT_DAMAGE -> 2.0;
            case HIT_RATE, ATTACK_SPEED -> 1.0;
            case HEALTH_REGEN -> 0.1;
            case MANA_REGEN -> 0.2;
            default -> 0.0;
        };
    }
    
    private static double getDefaultValue(StatType type) {
        return switch (type) {
            case CRIT_RATE -> 5.0;
            case CRIT_DAMAGE -> 150.0;
            case HIT_RATE -> 100.0;
            case ATTACK_SPEED, MOVE_SPEED -> 100.0;
            case MANA_REGEN -> 1.0;
            default -> 0.0;
        };
    }
    
    private static boolean isPercentageStat(StatType type) {
        return type == StatType.CRIT_RATE || type == StatType.CRIT_DAMAGE ||
               type == StatType.HIT_RATE || type == StatType.DODGE_RATE ||
               type == StatType.ATTACK_SPEED || type == StatType.MOVE_SPEED ||
               type == StatType.LIFE_STEAL || type == StatType.MANA_STEAL ||
               type == StatType.COOLDOWN_REDUCTION || type == StatType.EXP_BONUS ||
               type == StatType.DROP_BONUS;
    }
}
