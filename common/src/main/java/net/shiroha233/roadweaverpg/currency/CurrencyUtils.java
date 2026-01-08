package net.shiroha233.roadweaverpg.currency;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 货币工具类 - 处理货币转换和格式化
 */
public final class CurrencyUtils {
    
    private CurrencyUtils() {}
    
    /**
     * 将铜币总值转换为各种货币的数量
     * @param copperValue 铜币总值
     * @return 货币类型 -> 数量的映射（从高到低排序）
     */
    public static Map<CurrencyType, Long> breakdownCurrency(long copperValue) {
        Map<CurrencyType, Long> result = new LinkedHashMap<>();
        
        // 从高到低计算各货币数量
        for (CurrencyType type : new CurrencyType[]{
                CurrencyType.DIAMOND, 
                CurrencyType.EMERALD, 
                CurrencyType.GOLD, 
                CurrencyType.SILVER, 
                CurrencyType.COPPER}) {
            
            long count = copperValue / type.getValueInCopper();
            if (count > 0) {
                result.put(type, count);
                copperValue %= type.getValueInCopper();
            }
        }
        
        return result;
    }
    
    /**
     * 格式化货币显示（简洁版）
     * 例如：1234567 铜币 -> "1钻 3绿 2金"
     */
    public static String formatCurrencyCompact(long copperValue) {
        if (copperValue == 0) return "0铜";
        
        Map<CurrencyType, Long> breakdown = breakdownCurrency(copperValue);
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<CurrencyType, Long> entry : breakdown.entrySet()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(entry.getValue()).append(getCurrencyShortName(entry.getKey()));
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化货币显示（完整版）
     * 例如：1234567 铜币 -> "1钻石币 3绿宝石币 2金币"
     */
    public static String formatCurrencyFull(long copperValue) {
        if (copperValue == 0) return "0铜币";
        
        Map<CurrencyType, Long> breakdown = breakdownCurrency(copperValue);
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<CurrencyType, Long> entry : breakdown.entrySet()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(entry.getValue()).append(getCurrencyFullName(entry.getKey()));
        }
        
        return sb.toString();
    }
    
    /**
     * 获取货币简称
     */
    private static String getCurrencyShortName(CurrencyType type) {
        return switch (type) {
            case COPPER -> "铜";
            case SILVER -> "银";
            case GOLD -> "金";
            case EMERALD -> "绿";
            case DIAMOND -> "钻";
        };
    }
    
    /**
     * 获取货币全称
     */
    private static String getCurrencyFullName(CurrencyType type) {
        return switch (type) {
            case COPPER -> "铜币";
            case SILVER -> "银币";
            case GOLD -> "金币";
            case EMERALD -> "绿宝石币";
            case DIAMOND -> "钻石币";
        };
    }
    
    /**
     * 计算物品堆对应的铜币价值
     */
    public static long calculateCopperValue(CurrencyType type, int count) {
        return type.getValueInCopper() * count;
    }
}
