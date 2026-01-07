package net.shiroha233.roadweaverpg.entity.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * NPC服务层
 * 职责：管理NPC的注册、查找和通用操作
 * 原理：单例模式 + 服务层模式，降低耦合度
 */
public final class NPCService {
    
    private static final NPCService INSTANCE = new NPCService();
    
    // 线程安全的NPC注册表
    private final Map<Integer, INPCEntity> npcRegistry = new ConcurrentHashMap<>();
    
    // 交互回调（用于平台特定实现）
    private BiConsumer<ServerPlayer, INPCEntity> interactionCallback;
    
    private NPCService() {}
    
    public static NPCService getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册NPC
     */
    public void registerNPC(INPCEntity npc) {
        if (npc == null || npc.asEntity() == null) return;
        npcRegistry.put(npc.asEntity().getId(), npc);
    }
    
    /**
     * 注销NPC
     */
    public void unregisterNPC(int entityId) {
        npcRegistry.remove(entityId);
    }
    
    /**
     * 获取NPC
     */
    public INPCEntity getNPC(int entityId) {
        return npcRegistry.get(entityId);
    }
    
    /**
     * 获取NPC（通过实体）
     */
    public INPCEntity getNPC(LivingEntity entity) {
        if (entity instanceof INPCEntity npc) {
            return npc;
        }
        return getNPC(entity.getId());
    }
    
    /**
     * 处理NPC交互
     */
    public void handleInteraction(ServerPlayer player, INPCEntity npc) {
        if (npc == null || player == null) return;
        
        try {
            // 调用NPC自身的交互逻辑
            npc.handlePlayerInteraction(player);
            
            // 触发回调（用于平台特定逻辑）
            if (interactionCallback != null) {
                interactionCallback.accept(player, npc);
            }
        } catch (Exception e) {
            // 异常处理，防止单个NPC错误导致整体崩溃
            net.shiroha233.roadweaverpg.RoadWeaverRPG.LOGGER.error(
                    "Error handling NPC interaction for {}: {}", 
                    npc.getNPCType(), e.getMessage(), e);
        }
    }
    
    /**
     * 设置交互回调
     */
    public void setInteractionCallback(BiConsumer<ServerPlayer, INPCEntity> callback) {
        this.interactionCallback = callback;
    }
    
    /**
     * 清理已移除的NPC
     */
    public void cleanup() {
        npcRegistry.entrySet().removeIf(entry -> {
            LivingEntity entity = entry.getValue().asEntity();
            return entity == null || entity.isRemoved();
        });
    }
}
