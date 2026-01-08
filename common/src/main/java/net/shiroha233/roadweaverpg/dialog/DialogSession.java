package net.shiroha233.roadweaverpg.dialog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 对话会话
 * 职责：跟踪单个玩家与NPC的对话状态
 * 原理：不可变状态对象，每次状态变化创建新实例
 * 
 * 优化点：
 * - 添加对话历史记录
 * - 添加网络同步版本号
 * - 添加会话元数据
 * - 支持对话回退
 */
public record DialogSession(
        UUID sessionId,
        UUID playerId,
        int npcEntityId,
        ResourceLocation currentDialogId,
        int currentLineIndex,
        long startTime,
        long lastActivityTime,
        SessionState state,
        long syncVersion,
        List<String> dialogHistory,
        List<DialogSnapshot> snapshots  // 对话快照用于回退
) {
    
    /**
     * 会话状态
     */
    public enum SessionState {
        ACTIVE,      // 对话进行中
        WAITING,     // 等待玩家选择
        COMPLETED,   // 对话完成
        CANCELLED,   // 对话取消
        TIMEOUT      // 会话超时
    }
    
    // 会话超时时间（5分钟）
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000;
    // 活动超时时间（2分钟无操作）
    private static final long ACTIVITY_TIMEOUT_MS = 2 * 60 * 1000;
    // 最大历史记录数
    private static final int MAX_HISTORY_SIZE = 50;
    // 最大快照数（用于回退）
    private static final int MAX_SNAPSHOT_SIZE = 10;
    
    /**
     * 对话快照（用于回退）
     */
    public record DialogSnapshot(
            ResourceLocation dialogId,
            int lineIndex,
            long timestamp
    ) {}
    
    /**
     * 创建新会话
     */
    public static DialogSession create(ServerPlayer player, int npcEntityId, ResourceLocation dialogId) {
        long now = System.currentTimeMillis();
        return new DialogSession(
                UUID.randomUUID(),
                player.getUUID(),
                npcEntityId,
                dialogId,
                0,
                now,
                now,
                SessionState.ACTIVE,
                1L,
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
    
    /**
     * 前进到下一行（保存快照用于回退）
     */
    public DialogSession nextLine() {
        List<DialogSnapshot> newSnapshots = new ArrayList<>(snapshots);
        newSnapshots.add(new DialogSnapshot(currentDialogId, currentLineIndex, System.currentTimeMillis()));
        while (newSnapshots.size() > MAX_SNAPSHOT_SIZE) {
            newSnapshots.remove(0);
        }
        
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex + 1, startTime, System.currentTimeMillis(),
                state, syncVersion + 1, dialogHistory, newSnapshots
        );
    }
    
    /**
     * 回退到上一步
     */
    public Optional<DialogSession> previousLine() {
        if (snapshots.isEmpty()) {
            return Optional.empty();
        }
        
        List<DialogSnapshot> newSnapshots = new ArrayList<>(snapshots);
        DialogSnapshot lastSnapshot = newSnapshots.remove(newSnapshots.size() - 1);
        
        return Optional.of(new DialogSession(
                sessionId, playerId, npcEntityId, lastSnapshot.dialogId(),
                lastSnapshot.lineIndex(), startTime, System.currentTimeMillis(),
                SessionState.ACTIVE, syncVersion + 1, dialogHistory, newSnapshots
        ));
    }
    
    /**
     * 检查是否可以回退
     */
    public boolean canGoBack() {
        return !snapshots.isEmpty();
    }
    
    /**
     * 跳转到新对话
     */
    public DialogSession jumpToDialog(ResourceLocation newDialogId) {
        return new DialogSession(
                sessionId, playerId, npcEntityId, newDialogId,
                0, startTime, System.currentTimeMillis(),
                SessionState.ACTIVE, syncVersion + 1, dialogHistory, snapshots
        );
    }
    
    /**
     * 设置等待选择状态
     */
    public DialogSession waitForChoice() {
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex, startTime, System.currentTimeMillis(),
                SessionState.WAITING, syncVersion + 1, dialogHistory, snapshots
        );
    }
    
    /**
     * 完成对话
     */
    public DialogSession complete() {
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex, startTime, System.currentTimeMillis(),
                SessionState.COMPLETED, syncVersion + 1, dialogHistory, snapshots
        );
    }
    
    /**
     * 取消对话
     */
    public DialogSession cancel() {
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex, startTime, System.currentTimeMillis(),
                SessionState.CANCELLED, syncVersion + 1, dialogHistory, snapshots
        );
    }
    
    /**
     * 标记为超时
     */
    public DialogSession timeout() {
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex, startTime, System.currentTimeMillis(),
                SessionState.TIMEOUT, syncVersion + 1, dialogHistory, snapshots
        );
    }
    
    /**
     * 更新活动时间
     */
    public DialogSession touch() {
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex, startTime, System.currentTimeMillis(),
                state, syncVersion, dialogHistory, snapshots
        );
    }
    
    /**
     * 添加历史记录
     */
    public DialogSession addHistory(String entry) {
        List<String> newHistory = new ArrayList<>(dialogHistory);
        newHistory.add(entry);
        // 限制历史记录大小
        while (newHistory.size() > MAX_HISTORY_SIZE) {
            newHistory.remove(0);
        }
        return new DialogSession(
                sessionId, playerId, npcEntityId, currentDialogId,
                currentLineIndex, startTime, System.currentTimeMillis(),
                state, syncVersion, newHistory, snapshots
        );
    }
    
    /**
     * 获取当前对话数据
     */
    public Optional<DialogData> getCurrentDialog() {
        return DialogRegistry.getDialog(currentDialogId);
    }
    
    /**
     * 检查会话是否有效
     */
    public boolean isValid() {
        return state == SessionState.ACTIVE || state == SessionState.WAITING;
    }
    
    /**
     * 检查会话是否超时（总时长）
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > SESSION_TIMEOUT_MS;
    }
    
    /**
     * 检查是否活动超时（无操作）
     */
    public boolean isActivityTimeout() {
        return System.currentTimeMillis() - lastActivityTime > ACTIVITY_TIMEOUT_MS;
    }
    
    /**
     * 获取会话持续时间（秒）
     */
    public int getDurationSeconds() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }
    
    /**
     * 获取剩余超时时间（秒）
     */
    public int getRemainingTimeoutSeconds() {
        long remaining = SESSION_TIMEOUT_MS - (System.currentTimeMillis() - startTime);
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }
    
    /**
     * 获取不可变的历史记录
     */
    public List<String> getDialogHistory() {
        return Collections.unmodifiableList(dialogHistory);
    }
    
    /**
     * 验证同步版本
     */
    public boolean validateSyncVersion(long clientVersion) {
        return clientVersion == syncVersion || clientVersion == syncVersion - 1;
    }
}
