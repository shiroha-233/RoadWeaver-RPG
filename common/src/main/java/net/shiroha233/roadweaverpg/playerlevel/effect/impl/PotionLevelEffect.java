package net.shiroha233.roadweaverpg.playerlevel.effect.impl;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;

/**
 * 药水效果增益
 * 原理：给予玩家持续的药水效果（无限时长，每次刷新时重新应用）
 */
public class PotionLevelEffect implements LevelEffect {
    
    private static final int INFINITE_DURATION = 999999; // 约13.8小时，足够长
    
    private final ResourceLocation effectId;
    private final int amplifier;
    private final boolean ambient;
    private final boolean showParticles;
    
    public PotionLevelEffect(ResourceLocation effectId, int amplifier, boolean ambient, boolean showParticles) {
        this.effectId = effectId;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.showParticles = showParticles;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.POTION.getId();
    }
    
    @Override
    public Component getDescription() {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) {
            return Component.literal("Unknown Effect");
        }
        return Component.translatable("effect.roadweaver_rpg.potion", 
                effect.getDisplayName(), amplifier + 1);
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) {
            RoadWeaverRPG.LOGGER.warn("Unknown potion effect: {}", effectId);
            return;
        }
        
        // 移除旧效果并添加新效果
        player.removeEffect(effect);
        MobEffectInstance instance = new MobEffectInstance(
                effect, INFINITE_DURATION, amplifier, ambient, showParticles, true);
        player.addEffect(instance);
        
        RoadWeaverRPG.LOGGER.debug("Applied potion effect {} to {}", 
                effectId, player.getName().getString());
    }
    
    @Override
    public void remove(ServerPlayer player) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect != null) {
            player.removeEffect(effect);
        }
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(effectId);
        buf.writeVarInt(amplifier);
        buf.writeBoolean(ambient);
        buf.writeBoolean(showParticles);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", getTypeId());
        json.addProperty("effect", effectId.toString());
        json.addProperty("amplifier", amplifier);
        json.addProperty("ambient", ambient);
        json.addProperty("show_particles", showParticles);
        return json;
    }
    
    public static PotionLevelEffect fromJson(JsonObject json) {
        ResourceLocation effectId = new ResourceLocation(
                json.has("effect") ? json.get("effect").getAsString() : "minecraft:speed");
        int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
        boolean ambient = json.has("ambient") && json.get("ambient").getAsBoolean();
        boolean showParticles = !json.has("show_particles") || json.get("show_particles").getAsBoolean();
        return new PotionLevelEffect(effectId, amplifier, ambient, showParticles);
    }
    
    public static PotionLevelEffect fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation effectId = buf.readResourceLocation();
        int amplifier = buf.readVarInt();
        boolean ambient = buf.readBoolean();
        boolean showParticles = buf.readBoolean();
        return new PotionLevelEffect(effectId, amplifier, ambient, showParticles);
    }
    
    public ResourceLocation getEffectId() { return effectId; }
}
