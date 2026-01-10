package net.shiroha233.roadweaverpg.network.forge.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shiroha233.roadweaverpg.network.forge.NetworkHandlerForge;

import java.util.function.Supplier;

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
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 更新客户端职业缓存
            net.shiroha233.roadweaverpg.client.ClientProfessionCache.setPlayerProfession(professionId);
        });
        ctx.get().setPacketHandled(true);
    }
    
    public ResourceLocation getProfessionId() {
        return professionId;
    }
    
    /**
     * 发送消息到客户端
     */
    public static void send(ServerPlayer player, ResourceLocation professionId) {
        NetworkHandlerForge.sendToPlayer(player, new SyncPlayerProfessionMessage(professionId));
    }
}
