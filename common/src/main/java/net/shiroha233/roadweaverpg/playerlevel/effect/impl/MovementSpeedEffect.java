package net.shiroha233.roadweaverpg.playerlevel.effect.impl;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;
import net.shiroha233.roadweaverpg.stats.StatEffectService;

/**
 * 移动速度效果
 * 通过原版属性系统应用百分比加成
 */
public class MovementSpeedEffect implements LevelEffect {
    
    private final double percent;
    
    public MovementSpeedEffect(double percent) {
        this.percent = percent;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.MOVEMENT_SPEED.getId();
    }
    
    @Override
    public Component getDescription() {
        return Component.translatable("effect.roadweaver_rpg.movement_speed", 
                String.format("+%.1f%%", percent));
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        StatEffectService.applyMovementSpeed(player, percent);
    }
    
    @Override
    public void remove(ServerPlayer player) {
        // 由 StatEffectService.removeAllModifiers 统一处理
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(percent);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", getTypeId());
        json.addProperty("amount", percent);
        return json;
    }
    
    public static MovementSpeedEffect fromJson(JsonObject json) {
        double percent = json.has("amount") ? json.get("amount").getAsDouble() : 5.0;
        return new MovementSpeedEffect(percent);
    }
    
    public static MovementSpeedEffect fromNetwork(FriendlyByteBuf buf) {
        return new MovementSpeedEffect(buf.readDouble());
    }
    
    public double getPercent() { return percent; }
}
