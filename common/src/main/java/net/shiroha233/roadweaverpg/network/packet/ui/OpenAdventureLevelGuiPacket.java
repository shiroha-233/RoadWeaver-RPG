package net.shiroha233.roadweaverpg.network.packet.ui;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 打开冒险等级界面数据包（服务端 -> 客户端）
 */
public record OpenAdventureLevelGuiPacket() {
    
    public void encode(FriendlyByteBuf buf) {
        // 无需额外数据，数据已通过同步包发送
    }
    
    public static OpenAdventureLevelGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenAdventureLevelGuiPacket();
    }
}
