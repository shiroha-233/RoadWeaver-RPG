package net.shiroha233.roadweaverpg.network.message;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.network.NetworkHandler;
import net.shiroha233.roadweaverpg.profession.ProfessionDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    
    public List<ProfessionDefinition> getProfessions() {
        return professions;
    }
    
    /**
     * 发送消息到客户端
     */
    public static void send(ServerPlayer player, Collection<ProfessionDefinition> professions) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        new SyncProfessionsMessage(professions).encode(buf);
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_PROFESSIONS, buf);
    }
}
