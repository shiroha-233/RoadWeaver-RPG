package net.shiroha233.roadweaverpg.forge.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.entity.npc.ShopMaidEntity;
import net.shiroha233.roadweaverpg.forge.network.NetworkHandlerForge;

/**
 * Forge 平台的商店女仆实体
 */
public class ShopMaidEntityForge extends ShopMaidEntity {
    
    public ShopMaidEntityForge(EntityType<? extends ShopMaidEntityForge> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected void openShopDialogForPlayer(ServerPlayer player) {
        NetworkHandlerForge.sendOpenShopDialog(player, this.getId());
    }
}
