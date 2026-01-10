package net.shiroha233.roadweaverpg.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.function.BiConsumer;

/**
 * 战斗事件处理器
 */
public final class CombatEventHandler {
    
    private CombatEventHandler() {}
    
    private static BiConsumer<ServerPlayer, DamageIndicatorData> damageIndicatorCallback;
    
    public static void setDamageIndicatorCallback(BiConsumer<ServerPlayer, DamageIndicatorData> callback) {
        damageIndicatorCallback = callback;
        RoadWeaverRPG.LOGGER.info("[Combat] Damage indicator callback set");
    }
    
    /**
     * 计算暴击伤害（不发送显示）
     */
    public static CriticalHitService.CritResult calculateCriticalDamage(ServerPlayer attacker, float damage) {
        try {
            return CriticalHitService.calculateCriticalDamage(damage, attacker);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("[Combat] calculateCriticalDamage error: {}", e.getMessage());
            return new CriticalHitService.CritResult(false, damage);
        }
    }
    
    /**
     * 发送伤害显示（真实扣血量）
     */
    public static void sendDamageIndicator(ServerPlayer attacker, LivingEntity target, 
                                            float actualDamage, boolean isCritical) {
        if (damageIndicatorCallback == null) return;
        
        try {
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() + 0.5;
            double z = target.getZ();
            
            damageIndicatorCallback.accept(attacker, new DamageIndicatorData(x, y, z, actualDamage, isCritical));
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("[Combat] sendDamageIndicator error: {}", e.getMessage());
        }
    }
}
