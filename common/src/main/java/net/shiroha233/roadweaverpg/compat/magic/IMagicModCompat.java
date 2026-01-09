package net.shiroha233.roadweaverpg.compat.magic;

import net.minecraft.server.level.ServerPlayer;

/**
 * 魔法模组兼容接口
 * 遵循接口隔离原则，定义最小必要方法
 */
public interface IMagicModCompat {
    
    /**
     * 获取模组ID
     */
    String getModId();
    
    /**
     * 检查模组是否已加载
     */
    boolean isLoaded();
    
    /**
     * 应用法术威力加成
     * @param player 目标玩家
     * @param amount 加成数值（百分比，如0.1表示10%）
     */
    void applySpellPower(ServerPlayer player, double amount);
    
    /**
     * 应用最大魔力加成
     */
    void applyMaxMana(ServerPlayer player, double amount);
    
    /**
     * 应用魔力回复加成
     */
    void applyManaRegen(ServerPlayer player, double amount);
    
    /**
     * 应用冷却缩减
     */
    void applyCooldownReduction(ServerPlayer player, double amount);
    
    /**
     * 应用法术抗性
     */
    void applySpellResist(ServerPlayer player, double amount);
    
    /**
     * 移除所有本模组应用的属性修改器
     */
    void removeAllModifiers(ServerPlayer player);
    
    /**
     * 刷新玩家的魔法属性
     */
    default void refresh(ServerPlayer player) {
        // 默认实现：先移除再重新应用
    }
}
