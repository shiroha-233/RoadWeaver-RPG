package net.shiroha233.roadweaverpg.profession;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * 职业系统事件处理器
 * 
 * 设计原理：
 * - 单一职责：专注于职业相关的事件处理
 * - 线程安全：所有方法都是静态的，无状态
 */
public class ProfessionEventHandler {
    
    // 同步职业定义到客户端的回调
    private static BiConsumer<ServerPlayer, Collection<ProfessionDefinition>> syncDefinitionsCallback;
    
    // 打开职业选择界面的回调
    private static BiConsumer<ServerPlayer, Integer> openProfessionSelectionCallback;
    
    /**
     * 设置同步职业定义的回调
     */
    public static void setSyncDefinitionsCallback(BiConsumer<ServerPlayer, Collection<ProfessionDefinition>> callback) {
        syncDefinitionsCallback = callback;
    }
    
    /**
     * 设置打开职业选择界面的回调
     */
    public static void setOpenProfessionSelectionCallback(BiConsumer<ServerPlayer, Integer> callback) {
        openProfessionSelectionCallback = callback;
    }
    
    /**
     * 玩家登录时同步职业数据
     */
    public static void onPlayerLogin(ServerPlayer player) {
        try {
            ProfessionDataService service = ProfessionDataService.getInstance();
            
            // 同步职业定义到客户端
            syncProfessionDefinitions(player);
            
            // 刷新职业效果
            service.refreshProfessionEffects(player);
            
            // 同步职业数据到客户端
            service.syncToClient(player);
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to handle profession login for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 玩家重生时刷新职业效果
     */
    public static void onPlayerRespawn(ServerPlayer player) {
        try {
            ProfessionDataService.getInstance().refreshProfessionEffects(player);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to refresh profession on respawn for {}: {}", 
                    player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * 同步职业定义到客户端
     */
    public static void syncProfessionDefinitions(ServerPlayer player) {
        if (!ProfessionManager.isInitialized()) return;
        
        if (syncDefinitionsCallback != null) {
            Collection<ProfessionDefinition> definitions = ProfessionManager.getInstance().getAllProfessions();
            syncDefinitionsCallback.accept(player, definitions);
        }
    }
    
    /**
     * 打开职业选择界面
     */
    public static void openProfessionSelection(ServerPlayer player, int npcEntityId) {
        if (openProfessionSelectionCallback != null) {
            openProfessionSelectionCallback.accept(player, npcEntityId);
        }
    }
}
