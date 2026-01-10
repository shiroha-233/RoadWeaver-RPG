package net.shiroha233.roadweaverpg.combat;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 伤害指示器数据
 * 用于网络同步和客户端渲染
 */
public record DamageIndicatorData(
        double x,           // 实体位置X
        double y,           // 实体位置Y
        double z,           // 实体位置Z
        float damage,       // 伤害数值
        boolean isCritical  // 是否暴击
) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(damage);
        buf.writeBoolean(isCritical);
    }
    
    public static DamageIndicatorData decode(FriendlyByteBuf buf) {
        return new DamageIndicatorData(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readBoolean()
        );
    }
}
