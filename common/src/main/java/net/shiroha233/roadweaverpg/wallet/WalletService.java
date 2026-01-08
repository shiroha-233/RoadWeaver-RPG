package net.shiroha233.roadweaverpg.wallet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.item.CurrencyItem;

/**
 * 钱包服务 - 管理玩家货币的存取操作
 * 
 * 设计原理：
 * - 钱包内部统一以铜币为单位存储（最小单位）
 * - 支持多种货币类型，自动转换为铜币价值
 * - 提供统一的货币操作接口
 */
public final class WalletService {
    
    private WalletService() {}
    
    /**
     * 获取玩家钱包铜币总值
     */
    public static long getCoins(ServerPlayer player) {
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        return data.getWalletCoins();
    }
    
    /**
     * 添加铜币到玩家钱包
     * @return 添加后的总铜币数
     */
    public static long addCoins(ServerPlayer player, long copperAmount) {
        if (copperAmount <= 0) return getCoins(player);
        
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        long result = data.addWalletCoins(copperAmount);
        QuestDataAccessor.getInstance().markDirty(player);
        
        RoadWeaverRPG.LOGGER.debug("Added {} copper to {}, total: {}", 
                copperAmount, player.getName().getString(), result);
        return result;
    }
    
    /**
     * 从玩家钱包扣除铜币
     * @return 是否扣除成功
     */
    public static boolean removeCoins(ServerPlayer player, long copperAmount) {
        if (copperAmount <= 0) return true;
        
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        boolean success = data.removeWalletCoins(copperAmount);
        
        if (success) {
            QuestDataAccessor.getInstance().markDirty(player);
            RoadWeaverRPG.LOGGER.debug("Removed {} copper from {}", 
                    copperAmount, player.getName().getString());
        }
        return success;
    }
    
    /**
     * 将背包中的所有货币物品存入钱包
     * @return 存入的铜币总值
     */
    public static long depositCoinsFromInventory(ServerPlayer player) {
        long totalCopper = 0;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof CurrencyItem currencyItem) {
                long copperValue = CurrencyUtils.calculateCopperValue(
                        currencyItem.getCurrencyType(), stack.getCount());
                totalCopper += copperValue;
                stack.setCount(0);
            }
        }
        
        if (totalCopper > 0) {
            addCoins(player, totalCopper);
        }
        return totalCopper;
    }
    
    /**
     * 将指定货币物品堆存入钱包
     * @return 存入的铜币总值
     */
    public static long depositCurrencyStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CurrencyItem currencyItem)) {
            return 0;
        }
        
        long copperValue = CurrencyUtils.calculateCopperValue(
                currencyItem.getCurrencyType(), stack.getCount());
        stack.setCount(0);
        addCoins(player, copperValue);
        return copperValue;
    }
}
