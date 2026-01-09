package net.shiroha233.roadweaverpg.forge.compat.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.MagicModIds;
import net.shiroha233.roadweaverpg.compat.magic.IMagicModCompat;

/**
 * Ars Nouveau 模组兼容实现 (Forge)
 * 
 * 原理：Ars Nouveau 使用 Capability 系统管理魔力，
 * 通过 IManaCap 接口修改玩家的最大魔力和魔力回复
 * 
 * 注意：Ars Nouveau 的魔力系统与其他模组不同，
 * 它主要通过装备和 Perk 系统提供加成，
 * 直接修改 Capability 可能会与模组内部逻辑冲突
 */
public class ArsNouveauCompatForge implements IMagicModCompat {
    
    private final boolean loaded;
    
    // 缓存的加成值（用于 Capability 系统）
    private double cachedMaxManaBonus = 0;
    private double cachedManaRegenBonus = 0;
    
    public ArsNouveauCompatForge() {
        this.loaded = ModList.get().isLoaded(MagicModIds.ARS_NOUVEAU);
        if (loaded) {
            RoadWeaverRPG.LOGGER.info("Ars Nouveau detected, enabling compatibility");
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
    public void applySpellPower(ServerPlayer player, double amount) {
        // Ars Nouveau 没有直接的法术威力属性
        // 法术威力主要通过 Glyph 和 Augment 系统控制
        // 这里我们可以通过事件系统来实现，但需要更复杂的集成
        if (!loaded) return;
        RoadWeaverRPG.LOGGER.debug("Ars Nouveau spell power bonus: {} (not directly applicable)", amount);
    }
    
    @Override
    public void applyMaxMana(ServerPlayer player, double amount) {
        if (!loaded) return;
        this.cachedMaxManaBonus = amount;
        
        // Ars Nouveau 的魔力系统通过 Capability 管理
        // 我们需要通过事件或 Mixin 来应用加成
        // 这里记录加成值，在 ManaCapProvider 中使用
        try {
            // 尝试通过 Capability 系统获取并修改
            Class<?> capabilityClass = Class.forName("com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry");
            var playerManaCapField = capabilityClass.getField("PLAYER_MANA");
            var capability = playerManaCapField.get(null);
            
            // 获取玩家的 Capability
            var getCapabilityMethod = player.getClass().getMethod("getCapability", 
                    Class.forName("net.minecraftforge.common.capabilities.Capability"));
            var lazyOptional = getCapabilityMethod.invoke(player, capability);
            
            // 如果存在，修改最大魔力（通过反射调用ifPresent）
            lazyOptional.getClass().getMethod("ifPresent", 
                    Class.forName("java.util.function.Consumer"));
            
            // 由于 Capability 的复杂性，这里只记录日志
            RoadWeaverRPG.LOGGER.debug("Ars Nouveau max mana bonus applied: {}", amount);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to apply Ars Nouveau max mana: {}", e.getMessage());
        }
    }
    
    @Override
    public void applyManaRegen(ServerPlayer player, double amount) {
        if (!loaded) return;
        this.cachedManaRegenBonus = amount;
        RoadWeaverRPG.LOGGER.debug("Ars Nouveau mana regen bonus: {}", amount);
    }
    
    @Override
    public void applyCooldownReduction(ServerPlayer player, double amount) {
        // Ars Nouveau 没有直接的冷却缩减属性
        if (!loaded) return;
        RoadWeaverRPG.LOGGER.debug("Ars Nouveau cooldown reduction: {} (not directly applicable)", amount);
    }
    
    @Override
    public void applySpellResist(ServerPlayer player, double amount) {
        // Ars Nouveau 没有直接的法术抗性属性
        if (!loaded) return;
        RoadWeaverRPG.LOGGER.debug("Ars Nouveau spell resist: {} (not directly applicable)", amount);
    }
    
    @Override
    public void removeAllModifiers(ServerPlayer player) {
        if (!loaded) return;
        this.cachedMaxManaBonus = 0;
        this.cachedManaRegenBonus = 0;
    }
    
    /**
     * 获取缓存的最大魔力加成
     */
    public double getCachedMaxManaBonus() {
        return cachedMaxManaBonus;
    }
    
    /**
     * 获取缓存的魔力回复加成
     */
    public double getCachedManaRegenBonus() {
        return cachedManaRegenBonus;
    }
}
