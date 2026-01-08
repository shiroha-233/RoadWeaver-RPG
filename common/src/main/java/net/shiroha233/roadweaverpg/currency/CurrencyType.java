package net.shiroha233.roadweaverpg.currency;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 货币类型枚举
 * 定义了游戏中的多种货币及其兑换率
 * 
 * 兑换规则：64进制
 * - 64铜币 = 1银币
 * - 64银币 = 1金币
 * - 64金币 = 1绿宝石币
 * - 64绿宝石币 = 1钻石币
 */
public enum CurrencyType {
    COPPER("copper_coin", 1L, 0xB87333),           // 铜币，基础单位
    SILVER("silver_coin", 64L, 0xC0C0C0),          // 银币 = 64铜币
    GOLD("gold_coin", 4096L, 0xFFD700),            // 金币 = 64银币 = 4096铜币
    EMERALD("emerald_coin", 262144L, 0x50C878),    // 绿宝石币 = 64金币
    DIAMOND("diamond_coin", 16777216L, 0x00FFFF);  // 钻石币 = 64绿宝石币
    
    private final String id;
    private final long valueInCopper;  // 以铜币为基准的价值
    private final int color;           // 显示颜色
    
    CurrencyType(String id, long valueInCopper, int color) {
        this.id = id;
        this.valueInCopper = valueInCopper;
        this.color = color;
    }
    
    public String getId() {
        return id;
    }
    
    public ResourceLocation getResourceLocation() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, id);
    }
    
    public long getValueInCopper() {
        return valueInCopper;
    }
    
    public int getColor() {
        return color;
    }
    
    /**
     * 获取兑换率（相对于下一级货币）
     */
    public static final int EXCHANGE_RATE = 64;
    
    /**
     * 根据ID获取货币类型
     */
    public static CurrencyType fromId(String id) {
        for (CurrencyType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return COPPER;
    }
    
    /**
     * 获取翻译键
     */
    public String getTranslationKey() {
        return "item.roadweaver_rpg." + id;
    }
}
