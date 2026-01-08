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
 * 护甲值增益效果
 */
public class ArmorEffect implements LevelEffect {
    
    private static final UUID MODIFIER_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-345678901234");
    private static final String MODIFIER_NAME = "roadweaver_rpg.player_level.armor";
    
    private final double amount;
    private final AttributeModifier.Operation operation;
    
    public ArmorEffect(double amount, AttributeModifier.Operation operation) {
        this.amount = amount;
        this.operation = operation;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.ARMOR.getId();
    }
    
    @Override
    public Component getDescription() {
        String opStr = operation == AttributeModifier.Operation.ADDITION ? "+" : "×";
        String valueStr = operation == AttributeModifier.Operation.ADDITION 
                ? String.format("%.1f", amount) 
                : String.format("%.0f%%", amount * 100);
        return Component.translatable("effect.roadweaver_rpg.armor", opStr + valueStr);
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
        if (attribute == null) return;
        
        attribute.removeModifier(MODIFIER_UUID);
        
        AttributeModifier modifier = new AttributeModifier(
                MODIFIER_UUID, MODIFIER_NAME, amount, operation);
        attribute.addPermanentModifier(modifier);
        
        RoadWeaverRPG.LOGGER.debug("Applied armor effect to {}: +{}", 
                player.getName().getString(), amount);
    }
    
    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
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
    
    public static ArmorEffect fromJson(JsonObject json) {
        double amount = json.has("amount") ? json.get("amount").getAsDouble() : 1.0;
        String opStr = json.has("operation") ? json.get("operation").getAsString() : "addition";
        AttributeModifier.Operation op = parseOperation(opStr);
        return new ArmorEffect(amount, op);
    }
    
    public static ArmorEffect fromNetwork(FriendlyByteBuf buf) {
        double amount = buf.readDouble();
        AttributeModifier.Operation op = buf.readEnum(AttributeModifier.Operation.class);
        return new ArmorEffect(amount, op);
    }
    
    private static AttributeModifier.Operation parseOperation(String str) {
        return switch (str.toLowerCase()) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }
}
