package net.shiroha233.roadweaverpg.loot;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.ArrayList;
import java.util.List;

/**
 * 货币战利品配置 - 从JSON加载掉落规则
 * 
 * 设计原理：
 * - 完全数据驱动，无需修改Java代码
 * - 支持动态调整掉落概率和数量
 * - 支持添加新的货币类型
 */
public class CoinLootConfig {
    
    private boolean enabled;
    private final List<CoinDropRule> coins = new ArrayList<>();
    
    public static CoinLootConfig fromJson(JsonObject json) {
        CoinLootConfig config = new CoinLootConfig();
        config.enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
        
        if (json.has("coins") && json.get("coins").isJsonArray()) {
            json.getAsJsonArray("coins").forEach(elem -> {
                if (elem.isJsonObject()) {
                    config.coins.add(CoinDropRule.fromJson(elem.getAsJsonObject()));
                }
            });
        }
        
        return config;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public List<CoinDropRule> getCoins() {
        return coins;
    }
    
    /**
     * 根据配置生成掉落物品
     */
    public void generateDrops(List<ItemStack> loot, RandomSource random) {
        if (!enabled) return;
        
        for (CoinDropRule rule : coins) {
            if (random.nextFloat() < rule.probability) {
                int count = random.nextIntBetweenInclusive(rule.countMin, rule.countMax);
                ItemStack stack = new ItemStack(rule.getItem(), count);
                loot.add(stack);
            }
        }
    }
    
    /**
     * 货币掉落规则
     */
    public static class CoinDropRule {
        public String type;
        public String item;
        public double probability;
        public int countMin;
        public int countMax;
        
        private net.minecraft.world.item.Item cachedItem;
        
        public static CoinDropRule fromJson(JsonObject json) {
            CoinDropRule rule = new CoinDropRule();
            rule.type = json.get("type").getAsString();
            rule.item = json.get("item").getAsString();
            rule.probability = json.get("probability").getAsDouble();
            rule.countMin = json.get("count_min").getAsInt();
            rule.countMax = json.get("count_max").getAsInt();
            
            // 验证概率范围
            if (rule.probability < 0 || rule.probability > 1) {
                RoadWeaverRPG.LOGGER.warn("Invalid probability for coin type {}: {}", 
                        rule.type, rule.probability);
                rule.probability = Math.max(0, Math.min(1, rule.probability));
            }
            
            // 验证数量范围
            if (rule.countMin > rule.countMax) {
                int temp = rule.countMin;
                rule.countMin = rule.countMax;
                rule.countMax = temp;
            }
            
            return rule;
        }
        
        public net.minecraft.world.item.Item getItem() {
            if (cachedItem == null) {
                ResourceLocation id = new ResourceLocation(item);
                cachedItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                if (cachedItem == null) {
                    RoadWeaverRPG.LOGGER.warn("Unknown item for coin type {}: {}", type, item);
                    cachedItem = net.minecraft.world.item.Items.AIR;
                }
            }
            return cachedItem;
        }
    }
}
