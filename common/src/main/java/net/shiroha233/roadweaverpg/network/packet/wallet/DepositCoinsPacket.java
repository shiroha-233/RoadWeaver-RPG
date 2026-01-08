package net.shiroha233.roadweaverpg.network.packet.wallet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 存入金币请求数据包（客户端 -> 服务端）
 * 用于玩家主动将背包金币存入钱包
 */
public record DepositCoinsPacket() {
    
    public void encode(FriendlyByteBuf buf) {
        // 无需额外数据
    }
    
    public static DepositCoinsPacket decode(FriendlyByteBuf buf) {
        return new DepositCoinsPacket();
    }
}
