package net.shiroha233.roadweaverpg.forge.compat.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.fml.ModList;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.MagicModIds;
import net.shiroha233.roadweaverpg.compat.magic.IMagicModCompat;

import java.util.UUID;

/**
 * Iron's Spells & Spellbooks 模组兼容实现 (Forge)
 * 
 * 原理：通过反射获取 Iron's Spells 的属性注册表，
 * 然后使用 AttributeModifier 修改玩家的魔法属性
 */
public class IronsSpellsCompatForge implements IMagicModCompat {
    
    // 属性修改器UUID（确保唯一性）
    private static final UUID SPELL_POWER_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567001");
    private static final UUID MAX_MANA_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567002");
    private static final UUID MANA_REGEN_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567003");
    private static final UUID COOLDOWN_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567004");
    private static final UUID SPELL_RESIST_UUID = UUID.fromString("d1e2f3a4-b5c6-7890-abcd-ef1234567005");
    
    private static final String MODIFIER_NAME = "roadweaver_rpg.magic_bonus";
    
    private final boolean loaded;
    
    public IronsSpellsCompatForge() {
        this.loaded = ModList.get().isLoaded(MagicModIds.IRONS_SPELLS);
        if (loaded) {
            RoadWeaverRPG.LOGGER.info("Iron's Spells & Spellbooks detected, enabling compatibility");
        }
    }
    
    @Override
    public String getModId() {
        return MagicModIds.IRONS_SPELLS;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void applySpellPower(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            // 使用反射获取 SPELL_POWER 属性
            Class<?> attrRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            var spellPowerField = attrRegistry.getField("SPELL_POWER");
            var registryObject = spellPowerField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, SPELL_POWER_UUID, amount, 
                    AttributeModifier.Operation.MULTIPLY_BASE);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Iron's Spells spell power: {}", e.getMessage());
        }
    }
    
    @Override
    public void applyMaxMana(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            Class<?> attrRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            var maxManaField = attrRegistry.getField("MAX_MANA");
            var registryObject = maxManaField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, MAX_MANA_UUID, amount, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Iron's Spells max mana: {}", e.getMessage());
        }
    }
    
    @Override
    public void applyManaRegen(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            Class<?> attrRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            var manaRegenField = attrRegistry.getField("MANA_REGEN");
            var registryObject = manaRegenField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, MANA_REGEN_UUID, amount, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Iron's Spells mana regen: {}", e.getMessage());
        }
    }
    
    @Override
    public void applyCooldownReduction(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            Class<?> attrRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            var cooldownField = attrRegistry.getField("COOLDOWN_REDUCTION");
            var registryObject = cooldownField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            // Iron's Spells 的冷却缩减是乘法，值越小冷却越短
            applyModifier(player, attribute, COOLDOWN_UUID, -amount, 
                    AttributeModifier.Operation.MULTIPLY_BASE);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Iron's Spells cooldown reduction: {}", e.getMessage());
        }
    }
    
    @Override
    public void applySpellResist(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            Class<?> attrRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            var spellResistField = attrRegistry.getField("SPELL_RESIST");
            var registryObject = spellResistField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, SPELL_RESIST_UUID, amount, 
                    AttributeModifier.Operation.MULTIPLY_BASE);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Iron's Spells spell resist: {}", e.getMessage());
        }
    }
    
    @Override
    public void removeAllModifiers(ServerPlayer player) {
        if (!loaded) return;
        try {
            Class<?> attrRegistry = Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            
            removeModifierByUUID(player, attrRegistry, "SPELL_POWER", SPELL_POWER_UUID);
            removeModifierByUUID(player, attrRegistry, "MAX_MANA", MAX_MANA_UUID);
            removeModifierByUUID(player, attrRegistry, "MANA_REGEN", MANA_REGEN_UUID);
            removeModifierByUUID(player, attrRegistry, "COOLDOWN_REDUCTION", COOLDOWN_UUID);
            removeModifierByUUID(player, attrRegistry, "SPELL_RESIST", SPELL_RESIST_UUID);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to remove Iron's Spells modifiers: {}", e.getMessage());
        }
    }
    
    private void applyModifier(ServerPlayer player, 
                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                               UUID uuid, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        // 先移除旧的修改器
        instance.removeModifier(uuid);
        
        // 添加新的修改器
        if (amount != 0) {
            AttributeModifier modifier = new AttributeModifier(uuid, MODIFIER_NAME, amount, operation);
            instance.addPermanentModifier(modifier);
        }
    }
    
    private void removeModifierByUUID(ServerPlayer player, Class<?> attrRegistry, 
                                       String fieldName, UUID uuid) {
        try {
            var field = attrRegistry.getField(fieldName);
            var registryObject = field.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(uuid);
            }
        } catch (Exception ignored) {}
    }
}
