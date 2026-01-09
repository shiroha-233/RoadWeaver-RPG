package net.shiroha233.roadweaverpg.playerlevel.effect;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.playerlevel.effect.impl.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 等级效果注册表
 * 负责效果的序列化和反序列化
 */
public final class LevelEffectRegistry {
    
    private static final Map<LevelEffectType, Function<JsonObject, LevelEffect>> JSON_DESERIALIZERS 
            = new EnumMap<>(LevelEffectType.class);
    private static final Map<LevelEffectType, Function<FriendlyByteBuf, LevelEffect>> NETWORK_DESERIALIZERS 
            = new EnumMap<>(LevelEffectType.class);
    
    private LevelEffectRegistry() {}
    
    static {
        // 原版属性效果
        register(LevelEffectType.MAX_HEALTH, MaxHealthEffect::fromJson, MaxHealthEffect::fromNetwork);
        register(LevelEffectType.ATTACK_DAMAGE, AttackDamageEffect::fromJson, AttackDamageEffect::fromNetwork);
        register(LevelEffectType.ARMOR, ArmorEffect::fromJson, ArmorEffect::fromNetwork);
        register(LevelEffectType.MOVEMENT_SPEED, MovementSpeedEffect::fromJson, MovementSpeedEffect::fromNetwork);
        register(LevelEffectType.ATTACK_SPEED, AttackSpeedEffect::fromJson, AttackSpeedEffect::fromNetwork);
        
        // 魔法属性效果
        register(LevelEffectType.SPELL_POWER, SpellPowerEffect::fromJson, SpellPowerEffect::fromNetwork);
        register(LevelEffectType.MAX_MANA, MaxManaEffect::fromJson, MaxManaEffect::fromNetwork);
        register(LevelEffectType.MANA_REGEN, ManaRegenEffect::fromJson, ManaRegenEffect::fromNetwork);
        register(LevelEffectType.COOLDOWN_REDUCTION, CooldownReductionEffect::fromJson, CooldownReductionEffect::fromNetwork);
        register(LevelEffectType.SPELL_RESIST, SpellResistEffect::fromJson, SpellResistEffect::fromNetwork);
        
        // 特殊效果
        register(LevelEffectType.POTION, PotionLevelEffect::fromJson, PotionLevelEffect::fromNetwork);
        register(LevelEffectType.COMMAND, CommandEffect::fromJson, CommandEffect::fromNetwork);
    }
    
    public static void register(LevelEffectType type,
                                 Function<JsonObject, LevelEffect> jsonDeserializer,
                                 Function<FriendlyByteBuf, LevelEffect> networkDeserializer) {
        JSON_DESERIALIZERS.put(type, jsonDeserializer);
        NETWORK_DESERIALIZERS.put(type, networkDeserializer);
    }
    
    /**
     * 从JSON反序列化效果
     */
    public static LevelEffect fromJson(JsonObject json) {
        String typeStr = json.has("type") ? json.get("type").getAsString() : "";
        LevelEffectType type = LevelEffectType.fromString(typeStr);
        
        if (type == null) {
            RoadWeaverRPG.LOGGER.warn("Unknown level effect type: {}", typeStr);
            return null;
        }
        
        Function<JsonObject, LevelEffect> deserializer = JSON_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.warn("No deserializer for effect type: {}", type);
            return null;
        }
        
        try {
            return deserializer.apply(json);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to deserialize effect: {}", json, e);
            return null;
        }
    }
    
    /**
     * 从网络反序列化效果
     */
    public static LevelEffect fromNetwork(FriendlyByteBuf buf) {
        LevelEffectType type = buf.readEnum(LevelEffectType.class);
        
        Function<FriendlyByteBuf, LevelEffect> deserializer = NETWORK_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.error("No network deserializer for effect type: {}", type);
            return null;
        }
        
        try {
            return deserializer.apply(buf);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to deserialize effect from network", e);
            return null;
        }
    }
    
    /**
     * 序列化效果到网络
     */
    public static void toNetwork(LevelEffect effect, FriendlyByteBuf buf) {
        LevelEffectType type = LevelEffectType.fromString(effect.getTypeId());
        if (type == null) {
            RoadWeaverRPG.LOGGER.error("Unknown effect type: {}", effect.getTypeId());
            return;
        }
        buf.writeEnum(type);
        effect.toNetwork(buf);
    }
}
