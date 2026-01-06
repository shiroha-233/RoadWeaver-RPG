package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 打开声望界面数据包（服务端 -> 客户端）
 */
public record OpenReputationGuiPacket() {
    
    public void encode(FriendlyByteBuf buf) {
        // 无需额外数据
    }
    
    public static OpenReputationGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenReputationGuiPacket();
    }
}
