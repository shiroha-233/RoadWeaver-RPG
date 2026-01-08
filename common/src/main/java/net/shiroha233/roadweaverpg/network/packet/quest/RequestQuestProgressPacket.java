package net.shiroha233.roadweaverpg.network.packet.quest;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 请求委托进度数据包（客户端 -> 服务端）
 * 修复：同时发送questId和instanceId，支持精确匹配
 */
public record RequestQuestProgressPacket(ResourceLocation questId, @Nullable UUID instanceId) {
    
    public RequestQuestProgressPacket(ResourceLocation questId) {
        this(questId, null);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(questId);
        buf.writeBoolean(instanceId != null);
        if (instanceId != null) {
            buf.writeUUID(instanceId);
        }
    }
    
    public static RequestQuestProgressPacket decode(FriendlyByteBuf buf) {
        ResourceLocation qId = buf.readResourceLocation();
        UUID instId = buf.readBoolean() ? buf.readUUID() : null;
        return new RequestQuestProgressPacket(qId, instId);
    }
}
