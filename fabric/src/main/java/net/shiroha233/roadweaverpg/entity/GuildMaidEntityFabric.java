package net.shiroha233.roadweaverpg.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionRegistry;
import net.shiroha233.roadweaverpg.network.NetworkHandlerFabric;

/**
 * Fabric 平台的公会女仆实体
 */
public class GuildMaidEntityFabric extends GuildMaidEntity {
    
    public GuildMaidEntityFabric(EntityType<? extends GuildMaidEntityFabric> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected void openInteractionMenu(ServerPlayer player) {
        var entries = NPCInteractionRegistry.getEntries(this);
        NetworkHandlerFabric.sendOpenInteractionMenu(player, this.getId(), entries);
    }
    
    @Override
    protected void openDialogForPlayer(ServerPlayer player) {
        NetworkHandlerFabric.sendOpenDialog(player, this.getId());
    }
}
