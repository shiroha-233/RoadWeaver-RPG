package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步每日委托数据包（服务端 -> 客户端）
 */
public record SyncDailyQuestsPacket(
        List<ResourceLocation> dailyQuestIds,
        String refreshDate,
        int timeUntilRefresh
) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(dailyQuestIds.size());
        for (ResourceLocation id : dailyQuestIds) {
            buf.writeResourceLocation(id);
        }
        buf.writeUtf(refreshDate);
        buf.writeInt(timeUntilRefresh);
    }
    
    public static SyncDailyQuestsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ResourceLocation> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readResourceLocation());
        }
        String date = buf.readUtf();
        int time = buf.readInt();
        return new SyncDailyQuestsPacket(ids, date, time);
    }
}
