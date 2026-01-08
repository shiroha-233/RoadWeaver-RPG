package net.shiroha233.roadweaverpg.interaction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogManager;
import net.shiroha233.roadweaverpg.dialog.UnifiedActionHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * NPC交互处理器
 * 职责：处理交互入口的动作执行
 * 原理：策略模式，通过注册处理器来扩展功能
 * 
 * 优化点：
 * - 使用 UnifiedActionHandler 统一动作处理
 * - 添加异常处理和日志
 */
public final class NPCInteractionHandler {
    
    private NPCInteractionHandler() {}
    
    // 自定义功能处理器（用于扩展）
    private static final ConcurrentHashMap<ResourceLocation, BiConsumer<ServerPlayer, Integer>> 
            CUSTOM_HANDLERS = new ConcurrentHashMap<>();
    
    /**
     * 初始化默认处理器
     */
    public static void init() {
        // 初始化统一动作处理器
        UnifiedActionHandler.init();
        
        RoadWeaverRPG.LOGGER.info("NPC 交互处理器已初始化");
    }
    
    /**
     * 注册自定义功能处理器
     */
    public static void registerFunction(ResourceLocation id, BiConsumer<ServerPlayer, Integer> handler) {
        CUSTOM_HANDLERS.put(id, handler);
    }
    
    /**
     * 处理交互入口选择
     */
    public static void handleInteraction(ServerPlayer player, int npcEntityId, NPCInteractionEntry entry) {
        try {
            switch (entry.actionType()) {
                case NPCInteractionEntry.ACTION_DIALOG -> handleDialog(player, npcEntityId, entry);
                case NPCInteractionEntry.ACTION_FUNCTION -> handleFunction(player, npcEntityId, entry);
                case NPCInteractionEntry.ACTION_MENU -> handleMenu(player, npcEntityId, entry);
                default -> RoadWeaverRPG.LOGGER.warn("未知的动作类型: {}", entry.actionType());
            }
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("处理NPC交互失败: {} - {}", entry.id(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理对话动作
     */
    private static void handleDialog(ServerPlayer player, int npcEntityId, NPCInteractionEntry entry) {
        ResourceLocation dialogId = entry.actionData();
        if (dialogId != null) {
            boolean success = DialogManager.getInstance().startDialog(player, npcEntityId, dialogId);
            if (!success) {
                RoadWeaverRPG.LOGGER.warn("启动对话失败: {}", dialogId);
            }
        } else {
            RoadWeaverRPG.LOGGER.warn("对话入口缺少对话ID: {}", entry.id());
        }
    }
    
    /**
     * 处理功能动作
     */
    private static void handleFunction(ServerPlayer player, int npcEntityId, NPCInteractionEntry entry) {
        ResourceLocation functionId = entry.actionData();
        if (functionId == null) {
            RoadWeaverRPG.LOGGER.warn("功能入口缺少功能ID: {}", entry.id());
            return;
        }
        
        // 优先查找自定义处理器
        BiConsumer<ServerPlayer, Integer> customHandler = CUSTOM_HANDLERS.get(functionId);
        if (customHandler != null) {
            try {
                customHandler.accept(player, npcEntityId);
                return;
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("执行自定义功能失败: {}", functionId, e);
            }
        }
        
        // 使用统一动作处理器
        if (!UnifiedActionHandler.execute(player, npcEntityId, functionId)) {
            RoadWeaverRPG.LOGGER.warn("未找到功能处理器: {}", functionId);
        }
    }
    
    /**
     * 处理子菜单动作
     */
    private static void handleMenu(ServerPlayer player, int npcEntityId, NPCInteractionEntry entry) {
        // 未来扩展：打开子菜单
        RoadWeaverRPG.LOGGER.debug("子菜单功能待实现: {}", entry.id());
    }
    
    /**
     * 移除自定义处理器
     */
    public static void unregisterFunction(ResourceLocation id) {
        CUSTOM_HANDLERS.remove(id);
    }
    
    /**
     * 清空所有自定义处理器
     */
    public static void clearCustomHandlers() {
        CUSTOM_HANDLERS.clear();
    }
}
