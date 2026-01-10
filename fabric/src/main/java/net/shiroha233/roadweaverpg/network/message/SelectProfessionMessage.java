package net.shiroha233.roadweaverpg.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.profession.ProfessionDataService;

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
    
    /**
     * 服务端处理
     */
    public void handle(ServerPlayer player) {
        ProfessionDataService.getInstance().selectProfession(player, professionId);
    }
}
