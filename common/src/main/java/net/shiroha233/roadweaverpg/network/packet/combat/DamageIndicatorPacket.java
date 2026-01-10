package net.shiroha233.roadweaverpg.network.packet.combat;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.combat.DamageIndicatorData;

/**
 * 伤害指示器网络包
 * 服务端 -> 客户端
 */
public class DamageIndicatorPacket {
    
    private final DamageIndicatorData data;
    
    public DamageIndicatorPacket(DamageIndicatorData data) {
        this.data = data;
    }
    
    public void encode(FriendlyByteBuf buf) {
        data.encode(buf);
    }
    
    public static DamageIndicatorPacket decode(FriendlyByteBuf buf) {
        return new DamageIndicatorPacket(DamageIndicatorData.decode(buf));
    }
    
    public DamageIndicatorData getData() {
        return data;
    }
}
