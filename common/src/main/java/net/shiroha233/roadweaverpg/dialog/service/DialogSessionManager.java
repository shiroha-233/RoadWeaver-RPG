package net.shiroha233.roadweaverpg.dialog.service;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.DialogSession;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 对话会话管理器
 * 职责：管理对话会话的生命周期
 * 原理：单一职责原则，只负责会话的创建、更新、删除和清理
 */
public class DialogSessionManager {
    
    private static final DialogSessionManager INSTANCE = new DialogSessionManager();
    
    private final ConcurrentHashMap<UUID, DialogSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;
    private static final int CLEANUP_INTERVAL_SECONDS = 30;
    
    // 会话结束回调
    private Consumer<DialogSession> onSessionEnd;
    
    private DialogSessionManager() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DialogSessionManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredSessions,
                CLEANUP_INTERVAL_SECONDS,
                CLEANUP_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }
    
    public static DialogSessionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 设置会话结束回调
     */
    public void setOnSessionEnd(Consumer<DialogSession> callback) {
        this.onSessionEnd = callback;
    }
    
    /**
     * 创建新会话
     */
    public DialogSession createSession(ServerPlayer player, int npcEntityId, 
                                       net.minecraft.resources.ResourceLocation dialogId) {
        DialogSession session = DialogSession.create(player, npcEntityId, dialogId);
        sessions.put(player.getUUID(), session);
        RoadWeaverRPG.LOGGER.debug("创建对话会话: {}", session.sessionId());
        return session;
    }
    
    /**
     * 更新会话
     */
    public Optional<DialogSession> updateSession(UUID playerId, 
                                                 java.util.function.Function<DialogSession, DialogSession> updater) {
        DialogSession updated = sessions.compute(playerId, (uuid, current) -> {
            if (current == null || !current.isValid()) {
                return null;
            }
            return updater.apply(current);
        });
        return Optional.ofNullable(updated);
    }
    
    /**
     * 获取会话
     */
    public Optional<DialogSession> getSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }
    
    /**
     * 结束会话
     */
    public void endSession(UUID playerId) {
        DialogSession session = sessions.remove(playerId);
        if (session != null) {
            if (onSessionEnd != null) {
                onSessionEnd.accept(session);
            }
            RoadWeaverRPG.LOGGER.debug("对话会话结束: {}", session.sessionId());
        }
    }
    
    /**
     * 检查会话是否存在且有效
     */
    public boolean isInDialog(UUID playerId) {
        DialogSession session = sessions.get(playerId);
        return session != null && session.isValid();
    }
    
    /**
     * 验证同步版本
     */
    public boolean validateVersion(UUID playerId, long clientVersion) {
        DialogSession session = sessions.get(playerId);
        return session != null && session.validateSyncVersion(clientVersion);
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream().filter(DialogSession::isValid).count();
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        int[] removed = {0};
        sessions.entrySet().removeIf(entry -> {
            DialogSession session = entry.getValue();
            boolean shouldRemove = session.isExpired() || session.isActivityTimeout();
            
            if (shouldRemove) {
                removed[0]++;
                if (onSessionEnd != null) {
                    onSessionEnd.accept(session);
                }
                RoadWeaverRPG.LOGGER.debug("清理过期会话: {} (原因: {})", 
                        session.sessionId(),
                        session.isExpired() ? "总时长超时" : "活动超时");
            }
            return shouldRemove;
        });
        
        if (removed[0] > 0) {
            RoadWeaverRPG.LOGGER.debug("清理了 {} 个过期对话会话，当前活跃: {}", removed[0], sessions.size());
        }
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        sessions.clear();
        RoadWeaverRPG.LOGGER.info("DialogSessionManager 已关闭");
    }
}
