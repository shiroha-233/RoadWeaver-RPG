package net.shiroha233.roadweaverpg.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry;

import java.util.UUID;

/**
 * 属性效果应用服务
 * 统一管理所有属性的应用，包括原版属性和魔法模组属性
 * 遵循单一职责原则
 */
public final class StatEffectService {
    
    private StatEffectService() {}
    
    // 原版属性修改器UUID
    private static final UUID MAX_HEALTH_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111001");
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111002");
    private static final UUID ARMOR_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111003");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111004");
    private static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111005");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111006");
    private static final UUID KNOCKBACK_RESIST_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111007");
    private static final UUID LUCK_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111008");
    
    private static final String MODIFIER_NAME = "roadweaver_rpg.stat_bonus";
    
    // ==================== 原版属性应用 ====================
    
    /**
     * 应用最大生命值加成
     */
    public static void applyMaxHealth(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.MAX_HEALTH, MAX_HEALTH_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    /**
     * 应用攻击伤害加成
     */
    public static void applyAttackDamage(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    /**
     * 应用护甲值加成
     */
    public static void applyArmor(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR, ARMOR_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    /**
     * 应用护甲韧性加成（魔法防御）
     */
    public static void applyArmorToughness(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    /**
     * 应用移动速度加成（百分比）
     */
    public static void applyMovementSpeed(ServerPlayer player, double percent) {
        applyVanillaAttribute(player, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_UUID, percent / 100.0,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }
    
    /**
     * 应用攻击速度加成（百分比）
     */
    public static void applyAttackSpeed(ServerPlayer player, double percent) {
        applyVanillaAttribute(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID, percent / 100.0,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }
    
    /**
     * 应用击退抗性加成
     */
    public static void applyKnockbackResistance(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    /**
     * 应用幸运值加成（影响掉落）
     */
    public static void applyLuck(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.LUCK, LUCK_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    // ==================== 魔法属性应用 ====================
    
    /**
     * 应用法术威力加成（对所有已加载的魔法模组）
     */
    public static void applySpellPower(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applySpellPowerToAll(player, amount);
    }
    
    /**
     * 应用最大魔力加成
     */
    public static void applyMaxMana(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyMaxManaToAll(player, amount);
    }
    
    /**
     * 应用魔力回复加成
     */
    public static void applyManaRegen(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyManaRegenToAll(player, amount);
    }
    
    /**
     * 应用冷却缩减
     */
    public static void applyCooldownReduction(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyCooldownReductionToAll(player, amount);
    }
    
    /**
     * 应用法术抗性
     */
    public static void applySpellResist(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applySpellResistToAll(player, amount);
    }
    
    // ==================== 移除所有效果 ====================
    
    /**
     * 移除所有属性修改器
     */
    public static void removeAllModifiers(ServerPlayer player) {
        // 移除原版属性修改器
        removeVanillaModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_UUID);
        removeVanillaModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID);
        removeVanillaModifier(player, Attributes.ARMOR, ARMOR_UUID);
        removeVanillaModifier(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID);
        removeVanillaModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_UUID);
        removeVanillaModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID);
        removeVanillaModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_UUID);
        removeVanillaModifier(player, Attributes.LUCK, LUCK_UUID);
        
        // 移除魔法模组修改器
        MagicModCompatRegistry.removeAllModifiers(player);
    }
    
    // ==================== 内部方法 ====================
    
    private static void applyVanillaAttribute(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID uuid, double amount,
                                               AttributeModifier.Operation operation) {
        try {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;
            
            // 先移除旧的修改器
            instance.removeModifier(uuid);
            
            // 添加新的修改器
            if (amount != 0) {
                AttributeModifier modifier = new AttributeModifier(uuid, MODIFIER_NAME, amount, operation);
                instance.addPermanentModifier(modifier);
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to apply attribute {}: {}", 
                    attribute.getDescriptionId(), e.getMessage());
        }
    }
    
    private static void removeVanillaModifier(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID uuid) {
        try {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(uuid);
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to remove modifier: {}", e.getMessage());
        }
    }
}
