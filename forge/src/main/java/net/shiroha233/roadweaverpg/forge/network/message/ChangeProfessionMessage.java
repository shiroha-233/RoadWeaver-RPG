package net.shiroha233.roadweaverpg.forge.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shiroha233.roadweaverpg.profession.ProfessionDataService;

import java.util.function.Supplier;

/**
 * 客户端请求转职
 */
public class ChangeProfessionMessage {
    
    private final ResourceLocation professionId;
    private final boolean resetStats;
    
    public ChangeProfessionMessage(ResourceLocation professionId, boolean resetStats) {
        this.professionId = professionId;
        this.resetStats = resetStats;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(professionId);
        buf.writeBoolean(resetStats);
    }
    
    public static ChangeProfessionMessage decode(FriendlyByteBuf buf) {
        return new ChangeProfessionMessage(buf.readResourceLocation(), buf.readBoolean());
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ProfessionDataService.getInstance().changeProfession(player, professionId, resetStats);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
