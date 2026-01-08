package net.shiroha233.roadweaverpg.dialog;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 对话网络处理器
 * 职责：抽象对话数据的网络传输
 * 
 * 优化点：
 * - 添加重试机制
 * - 添加异常处理
 * - 添加发送确认
 */
public final class DialogNetworkHandler {
    
    private DialogNetworkHandler() {}
    
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 重试间隔（毫秒）
    private static final long RETRY_DELAY_MS = 100;
    
    // 网络发送回调（由平台注入）
    private static BiConsumer<ServerPlayer, DialogPacketData> dialogDataSender;
    private static BiConsumer<ServerPlayer, DialogLinePacketData> dialogLineSender;
    private static BiConsumer<ServerPlayer, DialogChoicesPacketData> dialogChoicesSender;
    private static Consumer<ServerPlayer> sessionCloseSender;
    
    /**
     * 初始化网络处理器
     */
    public static void init(
            BiConsumer<ServerPlayer, DialogPacketData> dataSender,
            BiConsumer<ServerPlayer, DialogLinePacketData> lineSender,
            BiConsumer<ServerPlayer, DialogChoicesPacketData> choicesSender
    ) {
        dialogDataSender = dataSender;
        dialogLineSender = lineSender;
        dialogChoicesSender = choicesSender;
    }
    
    /**
     * 设置会话关闭发送器
     */
    public static void setSessionCloseSender(Consumer<ServerPlayer> sender) {
        sessionCloseSender = sender;
    }
    
    /**
     * 发送完整对话数据（带重试和版本号）
     * 
     * 原理：在网络包中包含同步版本号，客户端保存后用于后续请求验证
     */
    public static void sendDialogData(ServerPlayer player, int npcEntityId, DialogData dialog, long syncVersion) {
        if (dialogDataSender == null) {
            RoadWeaverRPG.LOGGER.warn("对话数据发送器未初始化");
            return;
        }
        
        sendWithRetry(() -> dialogDataSender.accept(player, new DialogPacketData(npcEntityId, dialog, syncVersion)),
                "发送对话数据", player.getName().getString());
    }
    
    /**
     * 发送单行对话（带重试）
     */
    public static void sendDialogLine(ServerPlayer player, int npcEntityId, 
                                       DialogData dialog, int lineIndex) {
        if (dialogLineSender == null) {
            return;
        }
        
        if (lineIndex >= 0 && lineIndex < dialog.lines().size()) {
            DialogData.DialogLine line = dialog.lines().get(lineIndex);
            sendWithRetry(() -> dialogLineSender.accept(player, new DialogLinePacketData(npcEntityId, line, lineIndex)),
                    "发送对话行", player.getName().getString());
        }
    }
    
    /**
     * 发送对话选项（带重试）
     */
    public static void sendDialogChoices(ServerPlayer player, DialogData dialog) {
        if (dialogChoicesSender == null) {
            return;
        }
        
        sendWithRetry(() -> dialogChoicesSender.accept(player, new DialogChoicesPacketData(dialog.id(), dialog.choices())),
                "发送对话选项", player.getName().getString());
    }
    
    /**
     * 发送过滤后的对话选项
     */
    public static void sendFilteredDialogChoices(ServerPlayer player, DialogData dialog, 
                                                  List<DialogData.DialogChoice> filteredChoices) {
        if (dialogChoicesSender == null) {
            return;
        }
        
        sendWithRetry(() -> dialogChoicesSender.accept(player, new DialogChoicesPacketData(dialog.id(), filteredChoices)),
                "发送过滤后的对话选项", player.getName().getString());
    }
    
    /**
     * 发送会话关闭通知
     */
    public static void sendSessionClose(ServerPlayer player) {
        if (sessionCloseSender != null) {
            try {
                sessionCloseSender.accept(player);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("发送会话关闭通知失败: {}", player.getName().getString(), e);
            }
        }
    }
    
    /**
     * 带重试的发送方法
     */
    private static void sendWithRetry(Runnable sendAction, String actionName, String playerName) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                sendAction.run();
                return; // 成功
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < MAX_RETRIES) {
                    RoadWeaverRPG.LOGGER.debug("{}失败，尝试重试 ({}/{}): {}", 
                            actionName, attempts, MAX_RETRIES, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败
        RoadWeaverRPG.LOGGER.error("{}最终失败 (玩家: {}): {}", 
                actionName, playerName, lastException != null ? lastException.getMessage() : "未知错误");
    }
    
    /**
     * 检查网络处理器是否已初始化
     */
    public static boolean isInitialized() {
        return dialogDataSender != null;
    }
    
    // ==================== 数据包数据类 ====================
    
    /**
     * 完整对话数据包（包含版本号）
     */
    public record DialogPacketData(int npcEntityId, DialogData dialog, long syncVersion) {}
    
    /**
     * 单行对话数据包
     */
    public record DialogLinePacketData(int npcEntityId, DialogData.DialogLine line, int lineIndex) {}
    
    /**
     * 对话选项数据包
     */
    public record DialogChoicesPacketData(
            net.minecraft.resources.ResourceLocation dialogId,
            java.util.List<DialogData.DialogChoice> choices
    ) {}
}
