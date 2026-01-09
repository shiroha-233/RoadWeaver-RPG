package net.shiroha233.roadweaverpg.compat.magic;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔法模组兼容注册表
 * 统一管理所有魔法模组的兼容实现
 * 遵循开闭原则：对扩展开放，对修改关闭
 */
public final class MagicModCompatRegistry {
    
    private static final Map<String, IMagicModCompat> COMPATS = new ConcurrentHashMap<>();
    
    private MagicModCompatRegistry() {}
    
    /**
     * 注册魔法模组兼容实现
     */
    public static void register(IMagicModCompat compat) {
        if (compat.isLoaded()) {
            COMPATS.put(compat.getModId(), compat);
            RoadWeaverRPG.LOGGER.info("Registered magic mod compat: {}", compat.getModId());
        }
    }
    
    /**
     * 获取指定模组的兼容实现
     */
    public static IMagicModCompat getCompat(String modId) {
        return COMPATS.get(modId);
    }
    
    /**
     * 检查是否有任何魔法模组已加载
     */
    public static boolean hasAnyMagicMod() {
        return !COMPATS.isEmpty();
    }
    
    /**
     * 对所有已加载的魔法模组应用法术威力加成
     */
    public static void applySpellPowerToAll(ServerPlayer player, double amount) {
        COMPATS.values().forEach(compat -> {
            try {
                compat.applySpellPower(player, amount);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to apply spell power for {}: {}", 
                        compat.getModId(), e.getMessage());
            }
        });
    }
    
    /**
     * 对所有已加载的魔法模组应用最大魔力加成
     */
    public static void applyMaxManaToAll(ServerPlayer player, double amount) {
        COMPATS.values().forEach(compat -> {
            try {
                compat.applyMaxMana(player, amount);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to apply max mana for {}: {}", 
                        compat.getModId(), e.getMessage());
            }
        });
    }
    
    /**
     * 对所有已加载的魔法模组应用魔力回复加成
     */
    public static void applyManaRegenToAll(ServerPlayer player, double amount) {
        COMPATS.values().forEach(compat -> {
            try {
                compat.applyManaRegen(player, amount);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to apply mana regen for {}: {}", 
                        compat.getModId(), e.getMessage());
            }
        });
    }
    
    /**
     * 对所有已加载的魔法模组应用冷却缩减
     */
    public static void applyCooldownReductionToAll(ServerPlayer player, double amount) {
        COMPATS.values().forEach(compat -> {
            try {
                compat.applyCooldownReduction(player, amount);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to apply cooldown reduction for {}: {}", 
                        compat.getModId(), e.getMessage());
            }
        });
    }
    
    /**
     * 对所有已加载的魔法模组应用法术抗性
     */
    public static void applySpellResistToAll(ServerPlayer player, double amount) {
        COMPATS.values().forEach(compat -> {
            try {
                compat.applySpellResist(player, amount);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to apply spell resist for {}: {}", 
                        compat.getModId(), e.getMessage());
            }
        });
    }
    
    /**
     * 移除所有魔法模组的属性修改器
     */
    public static void removeAllModifiers(ServerPlayer player) {
        COMPATS.values().forEach(compat -> {
            try {
                compat.removeAllModifiers(player);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to remove modifiers for {}: {}", 
                        compat.getModId(), e.getMessage());
            }
        });
    }
    
    /**
     * 获取所有已注册的模组ID
     */
    public static Iterable<String> getRegisteredModIds() {
        return COMPATS.keySet();
    }
}
