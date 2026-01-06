package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 打开委托看板界面的数据包
 */
public record OpenQuestBoardPacket() {
    
    public void encode(FriendlyByteBuf buf) {
        // 无数据
    }
    
    public static OpenQuestBoardPacket decode(FriendlyByteBuf buf) {
        return new OpenQuestBoardPacket();
    }
}
