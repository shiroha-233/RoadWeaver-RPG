package net.shiroha233.roadweaverpg.compat.magic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.MagicModIds;

/**
 * Goety-2 模组兼容实现 (Fabric)
 * 
 * 注意：Goety-2 是 Forge 专属模组，Fabric 版本不存在
 */
public class GoetyCompatFabric implements IMagicModCompat {
    
    private final boolean loaded;
    
    public GoetyCompatFabric() {
        this.loaded = FabricLoader.getInstance().isModLoaded(MagicModIds.GOETY);
        if (loaded) {
            RoadWeaverRPG.LOGGER.info("Goety detected on Fabric (limited support)");
        }
    }
    
    @Override
    public String getModId() {
        return MagicModIds.GOETY;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void applySpellPower(ServerPlayer player, double amount) {}
    
    @Override
    public void applyMaxMana(ServerPlayer player, double amount) {}
    
    @Override
    public void applyManaRegen(ServerPlayer player, double amount) {}
    
    @Override
    public void applyCooldownReduction(ServerPlayer player, double amount) {}
    
    @Override
    public void applySpellResist(ServerPlayer player, double amount) {}
    
    @Override
    public void removeAllModifiers(ServerPlayer player) {}
}
