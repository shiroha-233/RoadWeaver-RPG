package net.shiroha233.roadweaverpg.network.packet.wallet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 同步钱包金币数据包（服务端 -> 客户端）
 */
public record SyncWalletPacket(long coins, long addedAmount) {
    
    /** 仅同步总数 */
    public SyncWalletPacket(long coins) {
        this(coins, 0);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(coins);
        buf.writeVarLong(addedAmount);
    }
    
    public static SyncWalletPacket decode(FriendlyByteBuf buf) {
        return new SyncWalletPacket(buf.readVarLong(), buf.readVarLong());
    }
}
