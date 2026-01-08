package net.shiroha233.roadweaverpg.network.packet.dialog;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.dialog.DialogData;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话选项数据包（服务端 -> 客户端）
 * 职责：传输过滤后的对话选项列表
 */
public record DialogChoicesPacket(ResourceLocation dialogId, List<DialogData.DialogChoice> choices) {
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dialogId);
        buf.writeVarInt(choices.size());
        for (DialogData.DialogChoice choice : choices) {
            choice.toNetwork(buf);
        }
    }
    
    public static DialogChoicesPacket decode(FriendlyByteBuf buf) {
        ResourceLocation dialogId = buf.readResourceLocation();
        int count = buf.readVarInt();
        List<DialogData.DialogChoice> choices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            choices.add(DialogData.DialogChoice.fromNetwork(buf));
        }
        return new DialogChoicesPacket(dialogId, choices);
    }
}
