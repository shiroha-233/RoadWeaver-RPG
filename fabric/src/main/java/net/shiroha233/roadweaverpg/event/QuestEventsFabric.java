package net.shiroha233.roadweaverpg.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
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
                // 冒险等级经验（击杀怪物）
                net.shiroha233.roadweaverpg.adventure.AdventureEventHandler.onEntityKilled(entity, killer);
                // 玩家等级不再通过击杀怪物获得，改为完成委托获得
            }
        });
        
        // 玩家登录事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            QuestEventHandler.onPlayerLogin(handler.getPlayer());
            // 同步冒险等级数据
            net.shiroha233.roadweaverpg.adventure.AdventureEventHandler.onPlayerLogin(handler.getPlayer());
            // 同步玩家等级数据
            net.shiroha233.roadweaverpg.playerlevel.PlayerLevelEventHandler.onPlayerLogin(handler.getPlayer());
            // 同步钱包数据
            syncWalletOnLogin(handler.getPlayer());
        });
        
        // 玩家登出事件（清理缓存，防止内存泄漏）
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            QuestEventHandler.onPlayerLogout(handler.getPlayer());
        });
        
        // 玩家重生事件（重新应用等级效果）
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            net.shiroha233.roadweaverpg.playerlevel.PlayerLevelEventHandler.onPlayerRespawn(newPlayer);
        });
        
        // 服务端Tick事件（用于玩家Tick）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                QuestEventHandler.onPlayerTick(player);
            }
        });
    }
    
    /** 玩家登录时同步钱包数据 */
    private static void syncWalletOnLogin(ServerPlayer player) {
        long coins = net.shiroha233.roadweaverpg.wallet.WalletService.getCoins(player);
        net.shiroha233.roadweaverpg.network.NetworkHandlerFabric.sendSyncWallet(player, coins);
    }
}
