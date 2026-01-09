package net.shiroha233.roadweaverpg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.stats.PlayerStats;

/**
 * 客户端玩家属性缓存
 * 从玩家实体读取基础属性，并缓存RPG扩展属性
 * 移除了元素伤害和元素抗性（参考项目不需要）
 */
public final class ClientStatsCache {
    
    private static final PlayerStats cachedStats = new PlayerStats();
    
    private ClientStatsCache() {}
    
    /**
     * 从玩家实体更新属性缓存
     */
    public static void updateFromPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        Player player = mc.player;
        
        // 基础属性从玩家实体读取
        cachedStats.setMaxHealth(player.getMaxHealth());
        cachedStats.setCurrentHealth(player.getHealth());
        cachedStats.setAttack(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        cachedStats.setDefense(player.getArmorValue());
        cachedStats.setMoveSpeed(player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1000);
        cachedStats.setAttackSpeed(player.getAttributeValue(Attributes.ATTACK_SPEED) * 100);
        
        // 护甲韧性作为魔法防御的基础
        cachedStats.setMagicDefense(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }
    
    /**
     * 从服务端同步的数据更新缓存
     */
    public static void updateFromServer(PlayerStats serverStats) {
        // 保留从玩家实体读取的基础属性，更新RPG扩展属性
        cachedStats.setMaxMana(serverStats.getMaxMana());
        cachedStats.setCurrentMana(serverStats.getCurrentMana());
        cachedStats.setMagicAttack(serverStats.getMagicAttack());
        
        cachedStats.setCritRate(serverStats.getCritRate());
        cachedStats.setCritDamage(serverStats.getCritDamage());
        cachedStats.setHitRate(serverStats.getHitRate());
        cachedStats.setDodgeRate(serverStats.getDodgeRate());
        cachedStats.setHealthRegen(serverStats.getHealthRegen());
        cachedStats.setManaRegen(serverStats.getManaRegen());
        
        // 特殊属性
        cachedStats.setLifeSteal(serverStats.getLifeSteal());
        cachedStats.setManaSteal(serverStats.getManaSteal());
        cachedStats.setCooldownReduction(serverStats.getCooldownReduction());
        cachedStats.setExpBonus(serverStats.getExpBonus());
        cachedStats.setDropBonus(serverStats.getDropBonus());
    }
    
    public static PlayerStats getStats() {
        return cachedStats;
    }
}
