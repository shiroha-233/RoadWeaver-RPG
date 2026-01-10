package net.shiroha233.roadweaverpg.network.packet.interaction;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 打开交互菜单数据包（服务端 -> 客户端）
 * 职责：传输NPC的交互入口列表
 */
public record OpenInteractionMenuPacket(int npcEntityId, List<NPCInteractionEntry> entries) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(npcEntityId);
        buf.writeVarInt(entries.size());
        
        for (NPCInteractionEntry entry : entries) {
            buf.writeUtf(entry.id());
            buf.writeComponent(entry.displayName());
            buf.writeComponent(entry.description());
            buf.writeUtf(entry.iconType());
            buf.writeVarInt(entry.priority());
            buf.writeUtf(entry.actionType());
            buf.writeResourceLocation(entry.actionData());
            buf.writeUtf(entry.condition() != null ? entry.condition() : "");
        }
    }
    
    public static OpenInteractionMenuPacket decode(FriendlyByteBuf buf) {
        int npcEntityId = buf.readInt();
        int count = buf.readVarInt();
        
        List<NPCInteractionEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            var displayName = buf.readComponent();
            var description = buf.readComponent();
            String iconType = buf.readUtf();
            int priority = buf.readVarInt();
            String actionType = buf.readUtf();
            ResourceLocation actionData = buf.readResourceLocation();
            String condition = buf.readUtf();
            
            entries.add(new NPCInteractionEntry(
                    id, displayName, description, iconType, priority, actionType, actionData, condition
            ));
        }
        
        return new OpenInteractionMenuPacket(npcEntityId, entries);
    }
}
