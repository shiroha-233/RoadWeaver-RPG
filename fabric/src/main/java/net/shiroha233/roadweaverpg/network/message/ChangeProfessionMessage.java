package net.shiroha233.roadweaverpg.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.profession.ProfessionDataService;

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
    
    /**
     * 服务端处理
     */
    public void handle(ServerPlayer player) {
        ProfessionDataService.getInstance().changeProfession(player, professionId, resetStats);
    }
}
