package net.shiroha233.roadweaverpg.client;

import net.shiroha233.roadweaverpg.config.RoadWeaverConfig;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端钱包数据缓存
 * 用于UI显示和金币获取提示
 */
public final class ClientWalletCache {
    
    private static final AtomicLong walletCoins = new AtomicLong(0);
    
    // 金币获取提示相关
    private static long lastAddedAmount = 0;
    private static long displayStartTime = 0;
    
    private ClientWalletCache() {}
    
    public static long getCoins() {
        return walletCoins.get();
    }
    
    public static void setCoins(long coins) {
        walletCoins.set(coins);
    }
    
    /**
     * 更新金币并触发显示提示
     */
    public static void updateCoins(long coins, long addedAmount) {
        walletCoins.set(coins);
        if (addedAmount > 0 && RoadWeaverConfig.get().wallet.showCoinNotification) {
            lastAddedAmount = addedAmount;
            displayStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 获取配置的显示时长
     */
    private static long getDisplayDuration() {
        return RoadWeaverConfig.get().wallet.notificationDuration;
    }
    
    /**
     * 检查是否应该显示金币获取提示
     */
    public static boolean shouldShowCoinNotification() {
        if (lastAddedAmount <= 0) return false;
        if (!RoadWeaverConfig.get().wallet.showCoinNotification) return false;
        return System.currentTimeMillis() - displayStartTime < getDisplayDuration();
    }
    
    /**
     * 获取提示显示进度 (0.0 ~ 1.0)
     */
    public static float getNotificationProgress() {
        if (!shouldShowCoinNotification()) return 0f;
        long elapsed = System.currentTimeMillis() - displayStartTime;
        return 1f - (float) elapsed / getDisplayDuration();
    }
    
    public static long getLastAddedAmount() {
        return lastAddedAmount;
    }
    
    public static void clearNotification() {
        lastAddedAmount = 0;
    }
}
