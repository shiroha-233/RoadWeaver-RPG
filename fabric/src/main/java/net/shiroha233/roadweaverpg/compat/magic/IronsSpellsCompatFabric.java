package net.shiroha233.roadweaverpg.compat.magic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.MagicModIds;

/**
 * Iron's Spells & Spellbooks 模组兼容实现 (Fabric)
 * 
 * 注意：Iron's Spells 主要是 Forge 模组，
 * Fabric 版本可能不存在或 API 不同
 */
public class IronsSpellsCompatFabric implements IMagicModCompat {
    
    private final boolean loaded;
    
    public IronsSpellsCompatFabric() {
        this.loaded = FabricLoader.getInstance().isModLoaded(MagicModIds.IRONS_SPELLS);
        if (loaded) {
            RoadWeaverRPG.LOGGER.info("Iron's Spells detected on Fabric (limited support)");
        }
    }
    
    @Override
    public String getModId() {
        return MagicModIds.IRONS_SPELLS;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void applySpellPower(ServerPlayer player, double amount) {
        // Fabric 版本暂不支持
    }
    
    @Override
    public void applyMaxMana(ServerPlayer player, double amount) {
        // Fabric 版本暂不支持
    }
    
    @Override
    public void applyManaRegen(ServerPlayer player, double amount) {
        // Fabric 版本暂不支持
    }
    
    @Override
    public void applyCooldownReduction(ServerPlayer player, double amount) {
        // Fabric 版本暂不支持
    }
    
    @Override
    public void applySpellResist(ServerPlayer player, double amount) {
        // Fabric 版本暂不支持
    }
    
    @Override
    public void removeAllModifiers(ServerPlayer player) {
        // Fabric 版本暂不支持
    }
}
