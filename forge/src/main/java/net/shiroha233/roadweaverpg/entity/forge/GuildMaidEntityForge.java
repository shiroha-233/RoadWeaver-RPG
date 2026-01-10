package net.shiroha233.roadweaverpg.entity.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionRegistry;
import net.shiroha233.roadweaverpg.network.forge.NetworkHandlerForge;

/**
 * Forge 平台的公会女仆实体
 */
public class GuildMaidEntityForge extends GuildMaidEntity {
    
    public GuildMaidEntityForge(EntityType<? extends GuildMaidEntityForge> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected void openInteractionMenu(ServerPlayer player) {
        var entries = NPCInteractionRegistry.getFilteredEntries(getNPCType().getId(), player);
        NetworkHandlerForge.sendOpenInteractionMenu(player, this.getId(), entries);
    }
    
    @Override
    protected void openDialogForPlayer(ServerPlayer player) {
        NetworkHandlerForge.sendOpenDialog(player, this.getId());
    }
}
