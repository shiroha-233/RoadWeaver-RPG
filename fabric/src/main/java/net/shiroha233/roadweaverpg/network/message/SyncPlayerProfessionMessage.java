package net.shiroha233.roadweaverpg.network.message;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.network.NetworkHandler;

/**
 * 同步玩家职业数据到客户端
 */
public class SyncPlayerProfessionMessage {
    
    private final ResourceLocation professionId; // null表示未选择职业
    
    public SyncPlayerProfessionMessage(ResourceLocation professionId) {
        this.professionId = professionId;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(professionId != null);
        if (professionId != null) {
            buf.writeResourceLocation(professionId);
        }
    }
    
    public static SyncPlayerProfessionMessage decode(FriendlyByteBuf buf) {
        boolean hasProfession = buf.readBoolean();
        ResourceLocation professionId = hasProfession ? buf.readResourceLocation() : null;
        return new SyncPlayerProfessionMessage(professionId);
    }
    
    public ResourceLocation getProfessionId() {
        return professionId;
    }
    
    /**
     * 发送消息到客户端
     */
    public static void send(ServerPlayer player, ResourceLocation professionId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncPlayerProfessionMessage(professionId).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_PLAYER_PROFESSION, buf);
    }
}
