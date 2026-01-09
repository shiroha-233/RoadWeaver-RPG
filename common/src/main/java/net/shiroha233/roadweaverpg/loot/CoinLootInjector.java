package net.shiroha233.roadweaverpg.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * 货币战利品注入器 - 向宝箱战利品表注入货币奖励
 * 
 * 设计原理：
 * - 完全数据驱动，从JSON配置加载掉落规则
 * - 支持两种注入方式：LootTable.Builder（Fabric）和直接列表注入（Forge全局修改器）
 * - 支持动态调整掉落概率和数量，无需修改代码
 */
public final class CoinLootInjector {
    
    private CoinLootInjector() {}
    
    /**
     * 向战利品表构建器注入货币池（Fabric方式）
     */
    public static void injectCoinPool(LootTable.Builder tableBuilder) {
        CoinLootConfig config = CoinLootConfigManager.getInstance().getConfig();
        
        for (CoinLootConfig.CoinDropRule rule : config.getCoins()) {
            tableBuilder.withPool(createCoinPool(rule));
        }
    }
    
    /**
     * 向物品列表直接注入货币（Forge全局修改器方式）
     */
    public static void injectCoinPoolToList(ObjectArrayList<ItemStack> loot, LootContext context) {
        CoinLootConfig config = CoinLootConfigManager.getInstance().getConfig();
        config.generateDrops(loot, context.getRandom());
    }
    
    /**
     * 根据规则创建战利品池
     */
    private static LootPool.Builder createCoinPool(CoinLootConfig.CoinDropRule rule) {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(1, 1))
                .when(LootItemRandomChanceCondition.randomChance((float) rule.probability))
                .add(LootItem.lootTableItem(rule.getItem())
                        .apply(SetItemCountFunction.setCount(
                                UniformGenerator.between(rule.countMin, rule.countMax))));
    }
}
