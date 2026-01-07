package net.shiroha233.roadweaverpg.network.packet.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 同步所有委托实例数据包（服务端 -> 客户端）
 * 用于玩家登录时同步所有进行中的委托
 */
public record SyncAllQuestInstancesPacket(List<QuestInstance> instances) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(instances.size());
        for (QuestInstance instance : instances) {
            instance.toNetwork(buf);
        }
    }
    
    public static SyncAllQuestInstancesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<QuestInstance> instances = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            instances.add(QuestInstance.fromNetwork(buf));
        }
        return new SyncAllQuestInstancesPacket(instances);
    }
    
    public static SyncAllQuestInstancesPacket fromCollection(Collection<QuestInstance> instances) {
        return new SyncAllQuestInstancesPacket(new ArrayList<>(instances));
    }
}
