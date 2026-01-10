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
     * 处理玩家攻击：计算暴击、修改伤害、发送显示
     */
    public static float onPlayerAttack(ServerPlayer attacker, LivingEntity target, float originalDamage) {
        try {
            RoadWeaverRPG.LOGGER.info("[Combat] onPlayerAttack called: damage={}", originalDamage);
            
            CriticalHitService.CritResult critResult = 
                    CriticalHitService.calculateCriticalDamage(originalDamage, attacker);
            
            sendDamageIndicator(attacker, target, critResult.damage(), critResult.isCritical());
            
            return critResult.damage();
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("[Combat] Error: {}", e.getMessage());
            return originalDamage;
        }
    }
    
    private static void sendDamageIndicator(ServerPlayer attacker, LivingEntity target, 
                                             float damage, boolean isCritical) {
        if (damageIndicatorCallback == null) {
            RoadWeaverRPG.LOGGER.warn("[Combat] Callback is null!");
            return;
        }
        
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() + 0.5;
        double z = target.getZ();
        
        damageIndicatorCallback.accept(attacker, new DamageIndicatorData(x, y, z, damage, isCritical));
    }
}
