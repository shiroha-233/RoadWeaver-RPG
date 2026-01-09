package net.shiroha233.roadweaverpg.playerlevel.effect.impl;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;
import net.shiroha233.roadweaverpg.stats.StatEffectService;

/**
 * 冷却缩减效果
 * 通过魔法模组兼容层应用到所有已加载的魔法模组
 */
public class CooldownReductionEffect implements LevelEffect {
    
    private final double amount;
    
    public CooldownReductionEffect(double amount) {
        this.amount = amount;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.COOLDOWN_REDUCTION.getId();
    }
    
    @Override
    public Component getDescription() {
        return Component.translatable("effect.roadweaver_rpg.cooldown_reduction", 
                String.format("-%.1f%%", amount));
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        StatEffectService.applyCooldownReduction(player, amount);
    }
    
    @Override
    public void remove(ServerPlayer player) {
        // 由 StatEffectService.removeAllModifiers 统一处理
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(amount);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", getTypeId());
        json.addProperty("amount", amount);
        return json;
    }
    
    public static CooldownReductionEffect fromJson(JsonObject json) {
        double amount = json.has("amount") ? json.get("amount").getAsDouble() : 5.0;
        return new CooldownReductionEffect(amount);
    }
    
    public static CooldownReductionEffect fromNetwork(FriendlyByteBuf buf) {
        return new CooldownReductionEffect(buf.readDouble());
    }
    
    public double getAmount() { return amount; }
}
