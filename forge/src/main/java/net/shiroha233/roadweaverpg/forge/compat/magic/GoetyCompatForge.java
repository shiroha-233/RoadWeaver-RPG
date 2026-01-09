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
 * Goety-2 模组兼容实现 (Forge)
 * 
 * 原理：通过反射获取 Goety 的 ModAttributes，
 * 修改玩家的法术效力、施法速度等属性
 */
public class GoetyCompatForge implements IMagicModCompat {
    
    private static final UUID SPELL_POTENCY_UUID = UUID.fromString("e2f3a4b5-c6d7-8901-bcde-f23456789001");
    private static final UUID CASTING_SPEED_UUID = UUID.fromString("e2f3a4b5-c6d7-8901-bcde-f23456789002");
    private static final UUID COOLDOWN_DISCOUNT_UUID = UUID.fromString("e2f3a4b5-c6d7-8901-bcde-f23456789003");
    private static final UUID SOUL_DISCOUNT_UUID = UUID.fromString("e2f3a4b5-c6d7-8901-bcde-f23456789004");
    private static final UUID SPELL_DURATION_UUID = UUID.fromString("e2f3a4b5-c6d7-8901-bcde-f23456789005");
    
    private static final String MODIFIER_NAME = "roadweaver_rpg.goety_bonus";
    
    private final boolean loaded;
    
    public GoetyCompatForge() {
        this.loaded = ModList.get().isLoaded(MagicModIds.GOETY);
        if (loaded) {
            RoadWeaverRPG.LOGGER.info("Goety-2 detected, enabling compatibility");
        }
    }
    
    @Override
    public String getModId() {
        return MagicModIds.GOETY;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void applySpellPower(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            // Goety 使用 SPELL_POTENCY 作为法术威力
            Class<?> modAttributes = Class.forName("com.Polarice3.Goety.init.ModAttributes");
            var spellPotencyField = modAttributes.getField("SPELL_POTENCY");
            var registryObject = spellPotencyField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            // Goety 的 SPELL_POTENCY 是加法，直接加数值
            applyModifier(player, attribute, SPELL_POTENCY_UUID, amount * 10, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Goety spell potency: {}", e.getMessage());
        }
    }
    
    @Override
    public void applyMaxMana(ServerPlayer player, double amount) {
        // Goety 使用灵魂系统而非魔力系统，此方法不适用
    }
    
    @Override
    public void applyManaRegen(ServerPlayer player, double amount) {
        // Goety 使用灵魂系统，可以应用到 SOUL_DISCOUNT
        if (!loaded) return;
        try {
            Class<?> modAttributes = Class.forName("com.Polarice3.Goety.init.ModAttributes");
            var soulDiscountField = modAttributes.getField("SOUL_DISCOUNT");
            var registryObject = soulDiscountField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, SOUL_DISCOUNT_UUID, amount * 0.1, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Goety soul discount: {}", e.getMessage());
        }
    }
    
    @Override
    public void applyCooldownReduction(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            Class<?> modAttributes = Class.forName("com.Polarice3.Goety.init.ModAttributes");
            var cooldownField = modAttributes.getField("COOLDOWN_DISCOUNT");
            var registryObject = cooldownField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, COOLDOWN_DISCOUNT_UUID, amount, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Goety cooldown discount: {}", e.getMessage());
        }
    }
    
    @Override
    public void applySpellResist(ServerPlayer player, double amount) {
        // Goety 没有直接的法术抗性属性，可以通过增加法术持续时间间接实现
        if (!loaded) return;
        try {
            Class<?> modAttributes = Class.forName("com.Polarice3.Goety.init.ModAttributes");
            var durationField = modAttributes.getField("SPELL_DURATION");
            var registryObject = durationField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, SPELL_DURATION_UUID, amount * 20, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Goety spell duration: {}", e.getMessage());
        }
    }
    
    /**
     * 应用施法速度加成
     */
    public void applyCastingSpeed(ServerPlayer player, double amount) {
        if (!loaded) return;
        try {
            Class<?> modAttributes = Class.forName("com.Polarice3.Goety.init.ModAttributes");
            var castingSpeedField = modAttributes.getField("CASTING_SPEED");
            var registryObject = castingSpeedField.get(null);
            var getAttribute = registryObject.getClass().getMethod("get");
            var attribute = (net.minecraft.world.entity.ai.attributes.Attribute) getAttribute.invoke(registryObject);
            
            applyModifier(player, attribute, CASTING_SPEED_UUID, amount, 
                    AttributeModifier.Operation.ADDITION);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Goety casting speed: {}", e.getMessage());
        }
    }
    
    @Override
    public void removeAllModifiers(ServerPlayer player) {
        if (!loaded) return;
        try {
            Class<?> modAttributes = Class.forName("com.Polarice3.Goety.init.ModAttributes");
            
            removeModifierByUUID(player, modAttributes, "SPELL_POTENCY", SPELL_POTENCY_UUID);
            removeModifierByUUID(player, modAttributes, "CASTING_SPEED", CASTING_SPEED_UUID);
            removeModifierByUUID(player, modAttributes, "COOLDOWN_DISCOUNT", COOLDOWN_DISCOUNT_UUID);
            removeModifierByUUID(player, modAttributes, "SOUL_DISCOUNT", SOUL_DISCOUNT_UUID);
            removeModifierByUUID(player, modAttributes, "SPELL_DURATION", SPELL_DURATION_UUID);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to remove Goety modifiers: {}", e.getMessage());
        }
    }
    
    private void applyModifier(ServerPlayer player, 
                               net.minecraft.world.entity.ai.attributes.Attribute attribute,
                               UUID uuid, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        
        instance.removeModifier(uuid);
        
        if (amount != 0) {
            AttributeModifier modifier = new AttributeModifier(uuid, MODIFIER_NAME, amount, operation);
            instance.addPermanentModifier(modifier);
        }
    }
    
    private void removeModifierByUUID(ServerPlayer player, Class<?> modAttributes, 
                                       String fieldName, UUID uuid) {
        try {
            var field = modAttributes.getField(fieldName);
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
