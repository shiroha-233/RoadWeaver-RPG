package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 同步所有声望等级定义（服务端 -> 客户端）
 */
public record SyncReputationLevelsPacket(Collection<ReputationLevel> levels) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(levels.size());
        for (ReputationLevel level : levels) {
            level.toNetwork(buf);
        }
    }
    
    public static SyncReputationLevelsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ReputationLevel> levels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            levels.add(ReputationLevel.fromNetwork(buf));
        }
        return new SyncReputationLevelsPacket(levels);
    }
}
