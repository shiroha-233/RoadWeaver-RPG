package net.shiroha233.roadweaverpg.forge.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.shiroha233.roadweaverpg.client.gui.profession.ProfessionSelectionScreen;
import net.shiroha233.roadweaverpg.forge.network.NetworkHandlerForge;

import java.util.function.Supplier;

/**
 * 打开职业选择界面消息
 */
public class OpenProfessionSelectionMessage {
    
    private final int npcEntityId;
    
    public OpenProfessionSelectionMessage(int npcEntityId) {
        this.npcEntityId = npcEntityId;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(npcEntityId);
    }
    
    public static OpenProfessionSelectionMessage decode(FriendlyByteBuf buf) {
        return new OpenProfessionSelectionMessage(buf.readVarInt());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 设置职业选择回调
            ProfessionSelectionScreen.setOnSelectProfession(OpenProfessionSelectionMessage::sendSelectProfession);
            Minecraft.getInstance().setScreen(new ProfessionSelectionScreen());
        });
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * 发送选择职业请求到服务端
     */
    private static void sendSelectProfession(ResourceLocation professionId) {
        NetworkHandlerForge.CHANNEL.sendToServer(new SelectProfessionMessage(professionId));
    }
}
