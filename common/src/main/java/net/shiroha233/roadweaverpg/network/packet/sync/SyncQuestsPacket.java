package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步委托定义数据包（服务端 -> 客户端）
 */
public record SyncQuestsPacket(List<QuestDefinition> quests) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(quests.size());
        for (QuestDefinition quest : quests) {
            quest.toNetwork(buf);
        }
    }
    
    public static SyncQuestsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<QuestDefinition> quests = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            quests.add(QuestDefinition.fromNetwork(buf));
        }
        return new SyncQuestsPacket(quests);
    }
}
