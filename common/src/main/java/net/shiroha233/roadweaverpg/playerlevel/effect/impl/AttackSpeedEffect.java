package net.shiroha233.roadweaverpg.playerlevel.effect.impl;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;
import net.shiroha233.roadweaverpg.stats.StatEffectService;

/**
 * 攻击速度效果
 * 通过原版属性系统应用百分比加成
 */
public class AttackSpeedEffect implements LevelEffect {
    
    private final double percent;
    
    public AttackSpeedEffect(double percent) {
        this.percent = percent;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.ATTACK_SPEED.getId();
    }
    
    @Override
    public Component getDescription() {
        return Component.translatable("effect.roadweaver_rpg.attack_speed", 
                String.format("+%.1f%%", percent));
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        // 攻击速度效果通过攻击冷却缩减实现
        StatEffectService.applyAttackCooldown(player, percent);
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
    
    public static AttackSpeedEffect fromJson(JsonObject json) {
        double percent = json.has("amount") ? json.get("amount").getAsDouble() : 5.0;
        return new AttackSpeedEffect(percent);
    }
    
    public static AttackSpeedEffect fromNetwork(FriendlyByteBuf buf) {
        return new AttackSpeedEffect(buf.readDouble());
    }
    
    public double getPercent() { return percent; }
}
