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
 * 统一管理所有属性的应用
 */
public final class StatEffectService {
    
    private StatEffectService() {}
    
    // 原版属性修改器UUID（技能点分配）
    private static final UUID MAX_HEALTH_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111001");
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111002");
    private static final UUID ARMOR_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111003");
    private static final UUID ARMOR_TOUGHNESS_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111004");
    private static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111005");
    private static final UUID ATTACK_COOLDOWN_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111009");
    private static final UUID KNOCKBACK_RESIST_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111007");
    private static final UUID LUCK_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-111111111008");
    
    // 职业基础属性UUID
    private static final UUID PROF_MAX_HEALTH_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-222222222001");
    private static final UUID PROF_ATTACK_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-222222222002");
    private static final UUID PROF_DEFENSE_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-222222222003");
    private static final UUID PROF_MAGIC_DEF_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-222222222004");
    private static final UUID PROF_MOVE_SPEED_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-222222222005");
    
    // 职业成长属性UUID
    private static final UUID GROWTH_MAX_HEALTH_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-333333333001");
    private static final UUID GROWTH_ATTACK_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-333333333002");
    private static final UUID GROWTH_DEFENSE_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-333333333003");
    private static final UUID GROWTH_MAGIC_DEF_UUID = UUID.fromString("f1a2b3c4-d5e6-7890-abcd-333333333004");
    
    private static final String MODIFIER_NAME = "roadweaver_rpg.stat_bonus";
    private static final String PROF_MODIFIER_NAME = "roadweaver_rpg.profession_base";
    private static final String GROWTH_MODIFIER_NAME = "roadweaver_rpg.profession_growth";
    
    // ==================== 原版属性应用 ====================
    
    public static void applyMaxHealth(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.MAX_HEALTH, MAX_HEALTH_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    public static void applyAttackDamage(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    public static void applyArmor(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR, ARMOR_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    public static void applyArmorToughness(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    public static void applyMovementSpeed(ServerPlayer player, double percent) {
        applyVanillaAttribute(player, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_UUID, percent / 100.0,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }
    
    /**
     * 应用攻击冷却缩减（百分比）
     * 原理：冷却缩减转换为攻击速度加成 speedBonus = 1/(1-cdr) - 1
     */
    public static void applyAttackCooldown(ServerPlayer player, double percent) {
        double cdr = Math.min(percent, 80) / 100.0; // 最大80%
        double speedBonus = cdr > 0 ? (1.0 / (1.0 - cdr)) - 1.0 : 0;
        applyVanillaAttribute(player, Attributes.ATTACK_SPEED, ATTACK_COOLDOWN_UUID, speedBonus,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }
    
    public static void applyKnockbackResistance(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    public static void applyLuck(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.LUCK, LUCK_UUID, amount,
                AttributeModifier.Operation.ADDITION);
    }
    
    // ==================== 魔法属性应用 ====================
    
    public static void applySpellPower(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applySpellPowerToAll(player, amount);
    }
    
    public static void applyMaxMana(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyMaxManaToAll(player, amount);
    }
    
    public static void applyManaRegen(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyManaRegenToAll(player, amount);
    }
    
    public static void applyCooldownReduction(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyCooldownReductionToAll(player, amount);
    }
    
    public static void applySpellResist(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applySpellResistToAll(player, amount);
    }
    
    // ==================== 移除所有效果 ====================
    
    public static void removeAllModifiers(ServerPlayer player) {
        // 技能点分配属性
        removeVanillaModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_UUID);
        removeVanillaModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID);
        removeVanillaModifier(player, Attributes.ARMOR, ARMOR_UUID);
        removeVanillaModifier(player, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_UUID);
        removeVanillaModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_UUID);
        removeVanillaModifier(player, Attributes.ATTACK_SPEED, ATTACK_COOLDOWN_UUID);
        removeVanillaModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESIST_UUID);
        removeVanillaModifier(player, Attributes.LUCK, LUCK_UUID);
        
        // 职业基础属性
        removeVanillaModifier(player, Attributes.MAX_HEALTH, PROF_MAX_HEALTH_UUID);
        removeVanillaModifier(player, Attributes.ATTACK_DAMAGE, PROF_ATTACK_UUID);
        removeVanillaModifier(player, Attributes.ARMOR, PROF_DEFENSE_UUID);
        removeVanillaModifier(player, Attributes.ARMOR_TOUGHNESS, PROF_MAGIC_DEF_UUID);
        removeVanillaModifier(player, Attributes.MOVEMENT_SPEED, PROF_MOVE_SPEED_UUID);
        
        // 职业成长属性
        removeVanillaModifier(player, Attributes.MAX_HEALTH, GROWTH_MAX_HEALTH_UUID);
        removeVanillaModifier(player, Attributes.ATTACK_DAMAGE, GROWTH_ATTACK_UUID);
        removeVanillaModifier(player, Attributes.ARMOR, GROWTH_DEFENSE_UUID);
        removeVanillaModifier(player, Attributes.ARMOR_TOUGHNESS, GROWTH_MAGIC_DEF_UUID);
        
        MagicModCompatRegistry.removeAllModifiers(player);
    }
    
    // ==================== 职业基础属性应用 ====================
    
    public static void applyProfessionMaxHealth(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.MAX_HEALTH, PROF_MAX_HEALTH_UUID, amount,
                AttributeModifier.Operation.ADDITION, PROF_MODIFIER_NAME);
    }
    
    public static void applyProfessionAttack(ServerPlayer player, double amount) {
        RoadWeaverRPG.LOGGER.debug("applyProfessionAttack: amount={}, uuid={}", amount, PROF_ATTACK_UUID);
        applyVanillaAttribute(player, Attributes.ATTACK_DAMAGE, PROF_ATTACK_UUID, amount,
                AttributeModifier.Operation.ADDITION, PROF_MODIFIER_NAME);
    }
    
    public static void applyProfessionDefense(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR, PROF_DEFENSE_UUID, amount,
                AttributeModifier.Operation.ADDITION, PROF_MODIFIER_NAME);
    }
    
    public static void applyProfessionMagicDefense(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR_TOUGHNESS, PROF_MAGIC_DEF_UUID, amount,
                AttributeModifier.Operation.ADDITION, PROF_MODIFIER_NAME);
    }
    
    public static void applyProfessionMoveSpeed(ServerPlayer player, double percent) {
        applyVanillaAttribute(player, Attributes.MOVEMENT_SPEED, PROF_MOVE_SPEED_UUID, percent / 100.0,
                AttributeModifier.Operation.MULTIPLY_BASE, PROF_MODIFIER_NAME);
    }
    
    public static void applyProfessionMaxMana(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyProfessionMaxMana(player, amount);
    }
    
    public static void applyProfessionSpellPower(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyProfessionSpellPower(player, amount);
    }
    
    public static void applyProfessionManaRegen(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyProfessionManaRegen(player, amount);
    }
    
    // ==================== 职业成长属性应用 ====================
    
    public static void applyGrowthMaxHealth(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.MAX_HEALTH, GROWTH_MAX_HEALTH_UUID, amount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void applyGrowthAttack(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ATTACK_DAMAGE, GROWTH_ATTACK_UUID, amount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void applyGrowthDefense(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR, GROWTH_DEFENSE_UUID, amount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void applyGrowthMagicDefense(ServerPlayer player, double amount) {
        applyVanillaAttribute(player, Attributes.ARMOR_TOUGHNESS, GROWTH_MAGIC_DEF_UUID, amount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void applyGrowthMaxMana(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyGrowthMaxMana(player, amount);
    }
    
    public static void applyGrowthSpellPower(ServerPlayer player, double amount) {
        MagicModCompatRegistry.applyGrowthSpellPower(player, amount);
    }
    
    // 增量添加成长属性（升级时使用）
    public static void addGrowthMaxHealth(ServerPlayer player, double additionalAmount) {
        addToVanillaAttribute(player, Attributes.MAX_HEALTH, GROWTH_MAX_HEALTH_UUID, additionalAmount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void addGrowthAttack(ServerPlayer player, double additionalAmount) {
        addToVanillaAttribute(player, Attributes.ATTACK_DAMAGE, GROWTH_ATTACK_UUID, additionalAmount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void addGrowthDefense(ServerPlayer player, double additionalAmount) {
        addToVanillaAttribute(player, Attributes.ARMOR, GROWTH_DEFENSE_UUID, additionalAmount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void addGrowthMagicDefense(ServerPlayer player, double additionalAmount) {
        addToVanillaAttribute(player, Attributes.ARMOR_TOUGHNESS, GROWTH_MAGIC_DEF_UUID, additionalAmount,
                AttributeModifier.Operation.ADDITION, GROWTH_MODIFIER_NAME);
    }
    
    public static void addGrowthMaxMana(ServerPlayer player, double additionalAmount) {
        MagicModCompatRegistry.addGrowthMaxMana(player, additionalAmount);
    }
    
    public static void addGrowthSpellPower(ServerPlayer player, double additionalAmount) {
        MagicModCompatRegistry.addGrowthSpellPower(player, additionalAmount);
    }
    
    // ==================== 内部方法 ====================
    
    private static void applyVanillaAttribute(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID uuid, double amount,
                                               AttributeModifier.Operation operation) {
        applyVanillaAttribute(player, attribute, uuid, amount, operation, MODIFIER_NAME);
    }
    
    private static void applyVanillaAttribute(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID uuid, double amount,
                                               AttributeModifier.Operation operation,
                                               String modifierName) {
        try {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                RoadWeaverRPG.LOGGER.warn("Attribute instance is null for: {}", attribute.getDescriptionId());
                return;
            }
            
            instance.removeModifier(uuid);
            if (amount != 0) {
                AttributeModifier modifier = new AttributeModifier(uuid, modifierName, amount, operation);
                instance.addPermanentModifier(modifier);
                RoadWeaverRPG.LOGGER.debug("Applied modifier: {} = {} to {}", 
                        attribute.getDescriptionId(), amount, player.getName().getString());
            }
            
            // 验证修改器是否存在
            AttributeModifier applied = instance.getModifier(uuid);
            if (applied != null) {
                RoadWeaverRPG.LOGGER.debug("Verified modifier exists: {} = {}", uuid, applied.getAmount());
            } else {
                RoadWeaverRPG.LOGGER.warn("Modifier not found after applying: {}", uuid);
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to apply attribute {}: {}", 
                    attribute.getDescriptionId(), e.getMessage());
        }
    }
    
    /**
     * 增量添加属性（在现有基础上增加）
     */
    private static void addToVanillaAttribute(ServerPlayer player,
                                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                               UUID uuid, double additionalAmount,
                                               AttributeModifier.Operation operation,
                                               String modifierName) {
        try {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;
            
            double currentAmount = 0;
            AttributeModifier existing = instance.getModifier(uuid);
            if (existing != null) {
                currentAmount = existing.getAmount();
                instance.removeModifier(uuid);
            }
            
            double newAmount = currentAmount + additionalAmount;
            if (newAmount != 0) {
                AttributeModifier modifier = new AttributeModifier(uuid, modifierName, newAmount, operation);
                instance.addPermanentModifier(modifier);
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to add to attribute {}: {}", 
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
