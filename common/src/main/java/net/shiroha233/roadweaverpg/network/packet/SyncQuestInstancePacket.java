package net.shiroha233.roadweaverpg.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

/**
 * 同步委托实例数据包（服务端 -> 客户端）
 * 用于同步单个委托的进度信息
 */
public record SyncQuestInstancePacket(ResourceLocation questId, QuestInstance instance) {
    
    public SyncQuestInstancePacket(QuestInstance instance) {
        this(instance != null ? instance.getQuestId() : null, instance);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(questId != null);
        if (questId != null) {
            buf.writeResourceLocation(questId);
        }
        buf.writeBoolean(instance != null);
        if (instance != null) {
            instance.toNetwork(buf);
        }
    }
    
    public static SyncQuestInstancePacket decode(FriendlyByteBuf buf) {
        ResourceLocation qId = null;
        if (buf.readBoolean()) {
            qId = buf.readResourceLocation();
        }
        QuestInstance inst = null;
        if (buf.readBoolean()) {
            inst = QuestInstance.fromNetwork(buf);
        }
        return new SyncQuestInstancePacket(qId, inst);
    }
}
