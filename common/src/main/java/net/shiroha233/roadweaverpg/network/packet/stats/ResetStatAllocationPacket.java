package net.shiroha233.roadweaverpg.network.packet.stats;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 重置属性分配请求（客户端 -> 服务端）
 */
public record ResetStatAllocationPacket() {
    
    public void encode(FriendlyByteBuf buf) {
        // 无需数据
    }
    
    public static ResetStatAllocationPacket decode(FriendlyByteBuf buf) {
        return new ResetStatAllocationPacket();
    }
}
