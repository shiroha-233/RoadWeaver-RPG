package net.shiroha233.roadweaverpg.forge.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.shiroha233.roadweaverpg.forge.network.ForgeNetworkHandler;

/**
 * 同步RPG属性到客户端
 */
public class SyncRpgStatsMessage {
    
    private final double maxMana;
    private final double magicAttack;
    private final double critRate;
    private final double critDamage;
    private final double hitRate;
    private final double dodgeRate;
    private final double healthRegen;
    private final double manaRegen;
    private final double lifeSteal;
    private final double manaSteal;
    private final double cooldownReduction;
    private final double expBonus;
    private final double dropBonus;
    
    public SyncRpgStatsMessage(double maxMana, double magicAttack, double critRate, double critDamage,
                                double hitRate, double dodgeRate, double healthRegen, double manaRegen,
                                double lifeSteal, double manaSteal, double cooldownReduction,
                                double expBonus, double dropBonus) {
        this.maxMana = maxMana;
        this.magicAttack = magicAttack;
        this.critRate = critRate;
        this.critDamage = critDamage;
        this.hitRate = hitRate;
        this.dodgeRate = dodgeRate;
        this.healthRegen = healthRegen;
        this.manaRegen = manaRegen;
        this.lifeSteal = lifeSteal;
        this.manaSteal = manaSteal;
        this.cooldownReduction = cooldownReduction;
        this.expBonus = expBonus;
        this.dropBonus = dropBonus;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(maxMana);
        buf.writeDouble(magicAttack);
        buf.writeDouble(critRate);
        buf.writeDouble(critDamage);
        buf.writeDouble(hitRate);
        buf.writeDouble(dodgeRate);
        buf.writeDouble(healthRegen);
        buf.writeDouble(manaRegen);
        buf.writeDouble(lifeSteal);
        buf.writeDouble(manaSteal);
        buf.writeDouble(cooldownReduction);
        buf.writeDouble(expBonus);
        buf.writeDouble(dropBonus);
    }
    
    public static SyncRpgStatsMessage decode(FriendlyByteBuf buf) {
        return new SyncRpgStatsMessage(
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble()
        );
    }
    
    // Getters
    public double getMaxMana() { return maxMana; }
    public double getMagicAttack() { return magicAttack; }
    public double getCritRate() { return critRate; }
    public double getCritDamage() { return critDamage; }
    public double getHitRate() { return hitRate; }
    public double getDodgeRate() { return dodgeRate; }
    public double getHealthRegen() { return healthRegen; }
    public double getManaRegen() { return manaRegen; }
    public double getLifeSteal() { return lifeSteal; }
    public double getManaSteal() { return manaSteal; }
    public double getCooldownReduction() { return cooldownReduction; }
    public double getExpBonus() { return expBonus; }
    public double getDropBonus() { return dropBonus; }
    
    /**
     * 发送消息到客户端
     */
    public static void send(ServerPlayer player, double maxMana, double magicAttack, 
            double critRate, double critDamage, double hitRate, double dodgeRate,
            double healthRegen, double manaRegen, double lifeSteal, double manaSteal,
            double cooldownReduction, double expBonus, double dropBonus) {
        ForgeNetworkHandler.CHANNEL.sendTo(
                new SyncRpgStatsMessage(maxMana, magicAttack, critRate, critDamage, hitRate, dodgeRate,
                        healthRegen, manaRegen, lifeSteal, manaSteal, cooldownReduction, expBonus, dropBonus),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}
