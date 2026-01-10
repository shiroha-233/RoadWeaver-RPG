package net.shiroha233.roadweaverpg.forge.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.shiroha233.roadweaverpg.client.ClientProfessionCache;
import net.shiroha233.roadweaverpg.profession.ProfessionDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * 同步职业定义到客户端
 */
public class SyncProfessionsMessage {
    
    private final List<ProfessionDefinition> professions;
    
    public SyncProfessionsMessage(Collection<ProfessionDefinition> professions) {
        this.professions = new ArrayList<>(professions);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(professions.size());
        for (ProfessionDefinition prof : professions) {
            prof.toNetwork(buf);
        }
    }
    
    public static SyncProfessionsMessage decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ProfessionDefinition> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(ProfessionDefinition.fromNetwork(buf));
        }
        return new SyncProfessionsMessage(list);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 更新客户端缓存
            ClientProfessionCache.updateProfessions(professions);
        });
        ctx.get().setPacketHandled(true);
    }
}
