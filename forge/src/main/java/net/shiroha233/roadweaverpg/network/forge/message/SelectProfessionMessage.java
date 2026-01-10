package net.shiroha233.roadweaverpg.network.forge.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shiroha233.roadweaverpg.profession.ProfessionDataService;

import java.util.function.Supplier;

/**
 * 客户端请求选择职业
 */
public class SelectProfessionMessage {
    
    private final ResourceLocation professionId;
    
    public SelectProfessionMessage(ResourceLocation professionId) {
        this.professionId = professionId;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(professionId);
    }
    
    public static SelectProfessionMessage decode(FriendlyByteBuf buf) {
        return new SelectProfessionMessage(buf.readResourceLocation());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ProfessionDataService.getInstance().selectProfession(player, professionId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
