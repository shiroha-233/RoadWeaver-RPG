package net.shiroha233.roadweaverpg.network.packet.dialog;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 对话会话关闭数据包（服务端 -> 客户端）
 * 职责：通知客户端关闭对话界面
 */
public record DialogSessionClosePacket(CloseReason reason) {
    
    /**
     * 关闭原因
     */
    public enum CloseReason {
        NORMAL,      // 正常结束
        TIMEOUT,     // 会话超时
        CANCELLED,   // 被取消
        ERROR        // 发生错误
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(reason);
    }
    
    public static DialogSessionClosePacket decode(FriendlyByteBuf buf) {
        CloseReason reason = buf.readEnum(CloseReason.class);
        return new DialogSessionClosePacket(reason);
    }
    
    /**
     * 创建正常关闭包
     */
    public static DialogSessionClosePacket normal() {
        return new DialogSessionClosePacket(CloseReason.NORMAL);
    }
    
    /**
     * 创建超时关闭包
     */
    public static DialogSessionClosePacket timeout() {
        return new DialogSessionClosePacket(CloseReason.TIMEOUT);
    }
    
    /**
     * 创建取消关闭包
     */
    public static DialogSessionClosePacket cancelled() {
        return new DialogSessionClosePacket(CloseReason.CANCELLED);
    }
    
    /**
     * 创建错误关闭包
     */
    public static DialogSessionClosePacket error() {
        return new DialogSessionClosePacket(CloseReason.ERROR);
    }
}
