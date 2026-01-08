package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.adventure.AdventureLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 同步冒险等级定义（服务端 -> 客户端）
 */
public record SyncAdventureLevelsPacket(Collection<AdventureLevel> levels) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(levels.size());
        for (AdventureLevel level : levels) {
            level.toNetwork(buf);
        }
    }
    
    public static SyncAdventureLevelsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<AdventureLevel> levels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            levels.add(AdventureLevel.fromNetwork(buf));
        }
        return new SyncAdventureLevelsPacket(levels);
    }
}
