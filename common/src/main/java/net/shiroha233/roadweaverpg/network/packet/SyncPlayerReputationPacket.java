package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import java.util.HashMap;
import java.util.Map;

/**
 * 同步玩家声望数据（服务端 -> 客户端）
 */
public record SyncPlayerReputationPacket(Map<String, Integer> reputations, Map<String, Integer> levels) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(reputations.size());
        reputations.forEach((faction, xp) -> {
            buf.writeUtf(faction);
            buf.writeVarInt(xp);
        });
        
        buf.writeVarInt(levels.size());
        levels.forEach((faction, level) -> {
            buf.writeUtf(faction);
            buf.writeVarInt(level);
        });
    }
    
    public static SyncPlayerReputationPacket decode(FriendlyByteBuf buf) {
        int repSize = buf.readVarInt();
        Map<String, Integer> reps = new HashMap<>(repSize);
        for (int i = 0; i < repSize; i++) {
            reps.put(buf.readUtf(), buf.readVarInt());
        }
        
        int levelSize = buf.readVarInt();
        Map<String, Integer> levels = new HashMap<>(levelSize);
        for (int i = 0; i < levelSize; i++) {
            levels.put(buf.readUtf(), buf.readVarInt());
        }
        
        return new SyncPlayerReputationPacket(reps, levels);
    }
}
