package net.shiroha233.roadweaverpg.playerlevel.effect.impl;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;

import java.util.UUID;

/**
 * 攻击伤害增益效果
 * 原理：通过AttributeModifier修改玩家的ATTACK_DAMAGE属性
 */
public class AttackDamageEffect implements LevelEffect {
    
    private static final UUID MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");
    private static final String MODIFIER_NAME = "roadweaver_rpg.player_level.attack_damage";
    
    private final double amount;
    private final AttributeModifier.Operation operation;
    
    public AttackDamageEffect(double amount, AttributeModifier.Operation operation) {
        this.amount = amount;
        this.operation = operation;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.ATTACK_DAMAGE.getId();
    }
    
    @Override
    public Component getDescription() {
        String opStr = operation == AttributeModifier.Operation.ADDITION ? "+" : "×";
        String valueStr = operation == AttributeModifier.Operation.ADDITION 
                ? String.format("%.1f", amount) 
                : String.format("%.0f%%", amount * 100);
        return Component.translatable("effect.roadweaver_rpg.attack_damage", opStr + valueStr);
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute == null) return;
        
        attribute.removeModifier(MODIFIER_UUID);
        
        AttributeModifier modifier = new AttributeModifier(
                MODIFIER_UUID, MODIFIER_NAME, amount, operation);
        attribute.addPermanentModifier(modifier);
        
        RoadWeaverRPG.LOGGER.debug("Applied attack damage effect to {}: +{}", 
                player.getName().getString(), amount);
    }
    
    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER_UUID);
        }
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(amount);
        buf.writeEnum(operation);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", getTypeId());
        json.addProperty("amount", amount);
        json.addProperty("operation", operation.name().toLowerCase());
        return json;
    }
    
    public static AttackDamageEffect fromJson(JsonObject json) {
        double amount = json.has("amount") ? json.get("amount").getAsDouble() : 1.0;
        String opStr = json.has("operation") ? json.get("operation").getAsString() : "addition";
        AttributeModifier.Operation op = parseOperation(opStr);
        return new AttackDamageEffect(amount, op);
    }
    
    public static AttackDamageEffect fromNetwork(FriendlyByteBuf buf) {
        double amount = buf.readDouble();
        AttributeModifier.Operation op = buf.readEnum(AttributeModifier.Operation.class);
        return new AttackDamageEffect(amount, op);
    }
    
    private static AttributeModifier.Operation parseOperation(String str) {
        return switch (str.toLowerCase()) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }
}
