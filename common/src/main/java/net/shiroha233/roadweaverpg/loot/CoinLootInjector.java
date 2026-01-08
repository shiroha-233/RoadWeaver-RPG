package net.shiroha233.roadweaverpg.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.shiroha233.roadweaverpg.item.ModItems;

/**
 * 货币战利品注入器 - 向宝箱战利品表注入货币奖励
 * 
 * 设计原理：
 * - 支持两种注入方式：LootTable.Builder（Fabric）和直接列表注入（Forge全局修改器）
 * - 优先掉落低级货币（铜币、银币）
 * - 偶尔掉落高级货币（金币、绿宝石币）
 */
public final class CoinLootInjector {
    
    private CoinLootInjector() {}
    
    /**
     * 向战利品表构建器注入货币池（Fabric方式）
     */
    public static void injectCoinPool(LootTable.Builder tableBuilder) {
        tableBuilder.withPool(createCopperPool());
        tableBuilder.withPool(createSilverPool());
        tableBuilder.withPool(createGoldPool());
        tableBuilder.withPool(createEmeraldPool());
    }
    
    /**
     * 向物品列表直接注入货币（Forge全局修改器方式）
     */
    public static void injectCoinPoolToList(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource random = context.getRandom();
        
        // 铜币（40%概率）
        if (random.nextFloat() < 0.4f) {
            int count = random.nextIntBetweenInclusive(10, 40);
            loot.add(new ItemStack(ModItems.COPPER_COIN.get(), count));
        }
        
        // 银币（30%概率）
        if (random.nextFloat() < 0.3f) {
            int count = random.nextIntBetweenInclusive(5, 15);
            loot.add(new ItemStack(ModItems.SILVER_COIN.get(), count));
        }
        
        // 金币（15%概率）
        if (random.nextFloat() < 0.15f) {
            int count = random.nextIntBetweenInclusive(2, 8);
            loot.add(new ItemStack(ModItems.GOLD_COIN.get(), count));
        }
        
        // 绿宝石币（5%概率）
        if (random.nextFloat() < 0.05f) {
            int count = random.nextIntBetweenInclusive(1, 3);
            loot.add(new ItemStack(ModItems.EMERALD_COIN.get(), count));
        }
    }
    
    private static LootPool.Builder createCopperPool() {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(1, 1))
                .when(LootItemRandomChanceCondition.randomChance(0.4f))
                .add(LootItem.lootTableItem(ModItems.COPPER_COIN.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(10, 40))));
    }
    
    private static LootPool.Builder createSilverPool() {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(1, 1))
                .when(LootItemRandomChanceCondition.randomChance(0.3f))
                .add(LootItem.lootTableItem(ModItems.SILVER_COIN.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(5, 15))));
    }
    
    private static LootPool.Builder createGoldPool() {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(1, 1))
                .when(LootItemRandomChanceCondition.randomChance(0.15f))
                .add(LootItem.lootTableItem(ModItems.GOLD_COIN.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))));
    }
    
    private static LootPool.Builder createEmeraldPool() {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(1, 1))
                .when(LootItemRandomChanceCondition.randomChance(0.05f))
                .add(LootItem.lootTableItem(ModItems.EMERALD_COIN.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))));
    }
}
