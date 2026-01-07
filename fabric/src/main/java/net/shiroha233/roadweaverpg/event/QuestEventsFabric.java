package net.shiroha233.roadweaverpg.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.quest.event.QuestEventHandler;

/**
 * Fabric 平台的委托事件注册
 */
public final class QuestEventsFabric {
    
    private QuestEventsFabric() {}
    
    public static void register() {
        // 实体死亡事件
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getEntity() instanceof ServerPlayer killer) {
                QuestEventHandler.onEntityKilled(killer, entity);
            }
        });
        
        // 玩家登录事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            QuestEventHandler.onPlayerLogin(handler.getPlayer());
        });
        
        // 玩家登出事件（清理缓存，防止内存泄漏）
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            QuestEventHandler.onPlayerLogout(handler.getPlayer());
        });
        
        // 服务端Tick事件（用于玩家Tick）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                QuestEventHandler.onPlayerTick(player);
            }
        });
    }
}
