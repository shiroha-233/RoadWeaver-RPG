package net.shiroha233.roadweaverpg.forge.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.shiroha233.roadweaverpg.loot.CoinLootInjector;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Forge全局货币战利品修改器
 * 向所有宝箱类型的战利品表注入货币奖励
 * 
 * 设计原理：
 * - 使用Forge全局战利品修改器系统，在战利品生成时动态注入货币
 * - 支持所有宝箱类型（chest、treasure、reward等）
 * - 通过JSON配置文件控制注入行为
 * - 参考TouhouLittleMaid的AdditionLootModifier实现
 */
public class CoinGlobalLootModifier extends LootModifier {
    public static final Supplier<Codec<CoinGlobalLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(instance -> codecStart(instance).and(instance.group(
                    ResourceLocation.CODEC.fieldOf("parameter_set_name").forGetter(m -> m.parameterSetName),
                    ResourceLocation.CODEC.optionalFieldOf("loot_table_id").forGetter(m -> Optional.ofNullable(m.lootTableId))
            )).apply(instance, CoinGlobalLootModifier::new)));

    private final ResourceLocation parameterSetName;
    private final ResourceLocation lootTableId;

    public CoinGlobalLootModifier(LootItemCondition[] conditionsIn, ResourceLocation parameterSetName, Optional<ResourceLocation> lootTableId) {
        super(conditionsIn);
        this.parameterSetName = parameterSetName;
        this.lootTableId = lootTableId.orElse(null);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // 检查参数集是否匹配（例如chest、entity等）
        ResourceLocation currentTableId = context.getQueriedLootTableId();
        LootTable currentTable = context.getResolver().getLootTable(currentTableId);
        
        if (!Objects.equals(currentTable.getParamSet(), LootContextParamSets.get(parameterSetName))) {
            return generatedLoot;
        }
        
        // 如果指定了特定的战利品表ID，检查是否匹配
        if (lootTableId != null && !currentTableId.equals(lootTableId)) {
            return generatedLoot;
        }
        
        // 向战利品表注入货币
        CoinLootInjector.injectCoinPoolToList(generatedLoot, context);
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
