package net.shiroha233.roadweaverpg.network.message;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.network.NetworkHandler;

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
    
    public int getNpcEntityId() {
        return npcEntityId;
    }
    
    /**
     * 发送消息到客户端
     */
    public static void send(ServerPlayer player, int npcEntityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new OpenProfessionSelectionMessage(npcEntityId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.OPEN_PROFESSION_SELECTION, buf);
    }
}
