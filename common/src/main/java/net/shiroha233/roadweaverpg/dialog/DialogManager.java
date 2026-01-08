package net.shiroha233.roadweaverpg.dialog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.condition.DialogConditionRegistry;
import net.shiroha233.roadweaverpg.dialog.filter.ConditionBasedFilter;
import net.shiroha233.roadweaverpg.dialog.service.DialogActionExecutor;
import net.shiroha233.roadweaverpg.dialog.service.DialogEventPublisher;
import net.shiroha233.roadweaverpg.dialog.service.DialogSessionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话管理器（重构版）
 * 职责：协调对话系统各个服务组件
 * 
 * 优化：
 * - 使用服务类分离职责
 * - 简化核心逻辑
 * - 提高可维护性
 */
public final class DialogManager {
    
    private static volatile DialogManager instance;
    
    // 服务组件
    private final DialogSessionManager sessionManager;
    private final DialogActionExecutor actionExecutor;
    private final DialogEventPublisher eventPublisher;
    private final ConditionBasedFilter filter;
    
    // 玩家UUID -> 会话映射（保留用于兼容）
    private final ConcurrentHashMap<UUID, DialogSession> sessions = new ConcurrentHashMap<>();
    
    private DialogManager() {
        sessionManager = DialogSessionManager.getInstance();
        actionExecutor = DialogActionExecutor.getInstance();
        eventPublisher = DialogEventPublisher.getInstance();
        filter = ConditionBasedFilter.getInstance();
        
        // 设置会话结束回调
        sessionManager.setOnSessionEnd(session -> {
            sessions.remove(session.playerId());
            eventPublisher.publishEnd(session.playerId(), session.npcEntityId(), session.state());
        });
    }
    
    public static DialogManager getInstance() {
        if (instance == null) {
            synchronized (DialogManager.class) {
                if (instance == null) {
                    instance = new DialogManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 开始新对话
     */
    public boolean startDialog(ServerPlayer player, int npcEntityId, ResourceLocation dialogId) {
        Optional<DialogData> dialogOpt = DialogRegistry.getDialog(dialogId);
        if (dialogOpt.isEmpty()) {
            RoadWeaverRPG.LOGGER.warn("对话不存在: {}", dialogId);
            return false;
        }
        
        DialogData dialog = dialogOpt.get();
        
        // 结束现有会话
        endSession(player.getUUID());
        
        // 创建新会话
        DialogSession session = sessionManager.createSession(player, npcEntityId, dialogId);
        sessions.put(player.getUUID(), session);
        
        // 触发开始事件
        eventPublisher.publishStart(player, npcEntityId, dialog);
        
        // 过滤并发送对话
        List<DialogData.DialogLine> visibleLines = filter.filterLines(dialog, player, npcEntityId);
        sendDialogToClient(player, session, dialog, visibleLines);
        
        RoadWeaverRPG.LOGGER.debug("玩家 {} 开始对话: {}", player.getName().getString(), dialogId);
        return true;
    }
    
    /**
     * 使用NPC类型开始默认对话
     */
    public boolean startDefaultDialog(ServerPlayer player, int npcEntityId, String npcType) {
        ResourceLocation defaultId = new ResourceLocation(RoadWeaverRPG.MOD_ID, npcType + "/greeting");
        return startDialog(player, npcEntityId, defaultId);
    }
    
    /**
     * 处理玩家选择
     */
    public void handleChoice(ServerPlayer player, String choiceId, long clientVersion) {
        DialogSession session = sessions.compute(player.getUUID(), (uuid, current) -> {
            if (current == null || !current.isValid()) {
                return null;
            }
            if (!current.validateSyncVersion(clientVersion)) {
                RoadWeaverRPG.LOGGER.warn("玩家 {} 的对话版本不匹配", player.getName().getString());
                return null;
            }
            return current.touch();
        });
        
        if (session == null) {
            sendSessionInvalidNotification(player);
            return;
        }
        
        Optional<DialogData> dialogOpt = session.getCurrentDialog();
        if (dialogOpt.isEmpty()) {
            endSession(player.getUUID());
            return;
        }
        
        DialogData dialog = dialogOpt.get();
        Optional<DialogData.DialogChoice> choiceOpt = findValidChoice(dialog, choiceId, player, session.npcEntityId());
        
        if (choiceOpt.isEmpty()) {
            RoadWeaverRPG.LOGGER.warn("无效选项: {}", choiceId);
            return;
        }
        
        DialogData.DialogChoice choice = choiceOpt.get();
        
        // 更新历史
        sessions.compute(player.getUUID(), (uuid, s) -> 
                s != null ? s.addHistory("choice:" + choiceId) : null);
        
        // 触发选择事件
        eventPublisher.publishChoice(player, session.npcEntityId(), choice);
        
        // 执行动作
        actionExecutor.execute(player, session, choice);
        
        // 处理跳转
        handleDialogTransition(player, session, choice);
    }
    
    /**
     * 推进对话
     */
    public void advanceDialog(ServerPlayer player) {
        DialogSession session = sessions.compute(player.getUUID(), (uuid, current) -> {
            if (current == null || !current.isValid()) {
                return null;
            }
            if (current.isExpired() || current.isActivityTimeout()) {
                return null;
            }
            return current.touch();
        });
        
        if (session == null) {
            return;
        }
        
        Optional<DialogData> dialogOpt = session.getCurrentDialog();
        if (dialogOpt.isEmpty()) {
            endSession(player.getUUID());
            return;
        }
        
        DialogData dialog = dialogOpt.get();
        List<DialogData.DialogLine> visibleLines = filter.filterLines(dialog, player, session.npcEntityId());
        
        int nextIndex = session.currentLineIndex() + 1;
        
        if (nextIndex < visibleLines.size()) {
            final int finalNextIndex = nextIndex;
            DialogSession newSession = sessions.compute(player.getUUID(), (uuid, s) -> 
                    s != null ? s.nextLine().addHistory("line:" + finalNextIndex) : null);
            
            if (newSession != null) {
                sendDialogLineToClient(player, newSession, visibleLines.get(nextIndex), nextIndex);
            }
        } else {
            handleDialogEnd(player, session, dialog);
        }
    }
    
    /**
     * 回退对话
     */
    public void goBackDialog(ServerPlayer player) {
        DialogSession session = sessions.get(player.getUUID());
        if (session == null || !session.canGoBack()) {
            return;
        }
        
        Optional<DialogSession> previousOpt = session.previousLine();
        if (previousOpt.isEmpty()) {
            return;
        }
        
        DialogSession previousSession = previousOpt.get();
        sessions.put(player.getUUID(), previousSession);
        
        Optional<DialogData> dialogOpt = previousSession.getCurrentDialog();
        if (dialogOpt.isPresent()) {
            DialogData dialog = dialogOpt.get();
            List<DialogData.DialogLine> visibleLines = filter.filterLines(dialog, player, previousSession.npcEntityId());
            sendDialogToClient(player, previousSession, dialog, visibleLines);
        }
    }
    
    /**
     * 结束会话（带玩家引用，用于发送关闭包）
     */
    public void endSession(ServerPlayer player) {
        UUID playerId = player.getUUID();
        DialogSession session = sessions.remove(playerId);
        if (session != null) {
            sessionManager.endSession(playerId);
            // 发送关闭包到客户端
            DialogNetworkHandler.sendSessionClose(player);
        }
    }
    
    /**
     * 结束会话（仅UUID，用于内部清理）
     */
    public void endSession(UUID playerId) {
        sessionManager.endSession(playerId);
        sessions.remove(playerId);
    }
    
    /**
     * 玩家断线清理
     */
    public void onPlayerDisconnect(UUID playerId) {
        endSession(playerId);
    }
    
    /**
     * 获取会话
     */
    public Optional<DialogSession> getSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }
    
    /**
     * 检查是否在对话中
     */
    public boolean isInDialog(UUID playerId) {
        return sessionManager.isInDialog(playerId);
    }
    
    /**
     * 验证版本
     */
    public boolean validateSessionVersion(UUID playerId, long clientVersion) {
        return sessionManager.validateVersion(playerId, clientVersion);
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return sessionManager.getActiveSessionCount();
    }
    
    /**
     * 关闭管理器
     */
    public void shutdown() {
        sessionManager.shutdown();
        sessions.clear();
    }
    
    // ==================== 私有辅助方法 ====================
    
    private Optional<DialogData.DialogChoice> findValidChoice(DialogData dialog, String choiceId, 
                                                               ServerPlayer player, int npcEntityId) {
        return dialog.choices().stream()
                .filter(c -> c.id().equals(choiceId))
                .filter(c -> DialogConditionRegistry.evaluate(c.condition(), player, npcEntityId))
                .findFirst();
    }
    
    private void handleDialogTransition(ServerPlayer player, DialogSession session, DialogData.DialogChoice choice) {
        // 如果动作是close，不再处理跳转（已经在actionExecutor中处理了）
        if ("close".equals(choice.action())) {
            // 确保会话已结束并通知客户端
            if (sessions.containsKey(player.getUUID())) {
                endSession(player);
            }
            return;
        }
        
        if (choice.nextDialog().isPresent()) {
            ResourceLocation nextDialogId = choice.nextDialog().get();
            Optional<DialogData> nextDialogOpt = DialogRegistry.getDialog(nextDialogId);
            
            if (nextDialogOpt.isPresent()) {
                DialogSession newSession = sessions.compute(player.getUUID(), (uuid, s) -> 
                        s != null ? s.jumpToDialog(nextDialogId) : null);
                
                if (newSession != null) {
                    DialogData nextDialog = nextDialogOpt.get();
                    List<DialogData.DialogLine> visibleLines = filter.filterLines(nextDialog, player, newSession.npcEntityId());
                    sendDialogToClient(player, newSession, nextDialog, visibleLines);
                }
            } else {
                endSession(player);
            }
        } else {
            endSession(player);
        }
    }
    
    private void handleDialogEnd(ServerPlayer player, DialogSession session, DialogData dialog) {
        List<DialogData.DialogChoice> visibleChoices = filter.filterChoices(dialog, player, session.npcEntityId());
        
        if (!visibleChoices.isEmpty()) {
            DialogSession waitingSession = sessions.compute(player.getUUID(), (uuid, s) -> 
                    s != null ? s.waitForChoice() : null);
            
            if (waitingSession != null) {
                sendChoicesToClient(player, dialog, visibleChoices);
            }
        } else if (dialog.nextDialog().isPresent()) {
            ResourceLocation nextDialogId = dialog.nextDialog().get();
            Optional<DialogData> nextDialogOpt = DialogRegistry.getDialog(nextDialogId);
            
            if (nextDialogOpt.isPresent()) {
                DialogSession newSession = sessions.compute(player.getUUID(), (uuid, s) -> 
                        s != null ? s.jumpToDialog(nextDialogId) : null);
                
                if (newSession != null) {
                    DialogData nextDialog = nextDialogOpt.get();
                    List<DialogData.DialogLine> visibleLines = filter.filterLines(nextDialog, player, newSession.npcEntityId());
                    sendDialogToClient(player, newSession, nextDialog, visibleLines);
                } else {
                    endSession(player);
                }
            } else {
                endSession(player);
            }
        } else {
            endSession(player);
        }
    }
    
    // ==================== 网络发送方法 ====================
    
    private static DialogDataSender dialogDataSender;
    private static DialogLineSender dialogLineSender;
    private static DialogChoicesSender dialogChoicesSender;
    private static SessionInvalidSender sessionInvalidSender;
    
    @FunctionalInterface
    public interface DialogDataSender {
        void send(ServerPlayer player, int npcEntityId, DialogData dialog, List<DialogData.DialogLine> visibleLines, long syncVersion);
    }
    
    @FunctionalInterface
    public interface DialogLineSender {
        void send(ServerPlayer player, int npcEntityId, DialogData.DialogLine line, int lineIndex);
    }
    
    @FunctionalInterface
    public interface DialogChoicesSender {
        void send(ServerPlayer player, DialogData dialog, List<DialogData.DialogChoice> visibleChoices);
    }
    
    @FunctionalInterface
    public interface SessionInvalidSender {
        void send(ServerPlayer player);
    }
    
    public static void setDialogDataSender(DialogDataSender sender) {
        dialogDataSender = sender;
    }
    
    public static void setDialogLineSender(DialogLineSender sender) {
        dialogLineSender = sender;
    }
    
    public static void setDialogChoicesSender(DialogChoicesSender sender) {
        dialogChoicesSender = sender;
    }
    
    public static void setSessionInvalidSender(SessionInvalidSender sender) {
        sessionInvalidSender = sender;
    }
    
    private void sendDialogToClient(ServerPlayer player, DialogSession session, DialogData dialog, 
                                     List<DialogData.DialogLine> visibleLines) {
        if (dialogDataSender != null) {
            try {
                dialogDataSender.send(player, session.npcEntityId(), dialog, visibleLines, session.syncVersion());
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("发送对话数据失败", e);
            }
        }
        DialogNetworkHandler.sendDialogData(player, session.npcEntityId(), dialog, session.syncVersion());
    }
    
    private void sendDialogLineToClient(ServerPlayer player, DialogSession session, 
                                         DialogData.DialogLine line, int lineIndex) {
        if (dialogLineSender != null) {
            try {
                dialogLineSender.send(player, session.npcEntityId(), line, lineIndex);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("发送对话行失败", e);
            }
        }
    }
    
    private void sendChoicesToClient(ServerPlayer player, DialogData dialog, 
                                      List<DialogData.DialogChoice> visibleChoices) {
        if (dialogChoicesSender != null) {
            try {
                dialogChoicesSender.send(player, dialog, visibleChoices);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("发送对话选项失败", e);
            }
        }
    }
    
    private void sendSessionInvalidNotification(ServerPlayer player) {
        if (sessionInvalidSender != null) {
            try {
                sessionInvalidSender.send(player);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("发送会话无效通知失败", e);
            }
        }
    }
}
