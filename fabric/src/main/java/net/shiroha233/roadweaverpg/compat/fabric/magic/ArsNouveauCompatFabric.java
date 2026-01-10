package net.shiroha233.roadweaverpg.compat.fabric.magic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.MagicModIds;
import net.shiroha233.roadweaverpg.compat.magic.IMagicModCompat;

/**
 * Ars Nouveau 模组兼容实现 (Fabric)
 * 
 * 注意：Ars Nouveau 主要是 Forge 模组，
 * Fabric 版本可能不存在或 API 不同
 */
public class ArsNouveauCompatFabric implements IMagicModCompat {
    
    private final boolean loaded;
    
    public ArsNouveauCompatFabric() {
        this.loaded = FabricLoader.getInstance().isModLoaded(MagicModIds.ARS_NOUVEAU);
        if (loaded) {
            RoadWeaverRPG.LOGGER.info("Ars Nouveau detected on Fabric (limited support)");
        }
    }
    
    @Override
    public String getModId() {
        return MagicModIds.ARS_NOUVEAU;
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
