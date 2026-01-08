package net.shiroha233.roadweaverpg.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.client.ClientWalletCache;
import net.shiroha233.roadweaverpg.currency.CurrencyType;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.item.ModItems;

import java.util.Map;

/**
 * 货币获取提示渲染器 - 在屏幕左下角显示货币获取信息
 */
public final class CoinNotificationRenderer {
    
    private static final int PADDING = 6;
    private static final int ICON_SIZE = 16;
    private static final int ICON_SPACING = 28;
    private static final int LINE_HEIGHT = 14;
    
    private CoinNotificationRenderer() {}
    
    /**
     * 渲染货币获取提示
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!ClientWalletCache.shouldShowCoinNotification()) return;
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        
        float progress = ClientWalletCache.getNotificationProgress();
        long addedCopper = ClientWalletCache.getLastAddedAmount();
        long totalCopper = ClientWalletCache.getCoins();
        
        // 计算透明度
        float alphaF = calculateAlphaFloat(progress);
        int alpha = (int)(alphaF * 255);
        
        // 位置
        int x = PADDING;
        int y = screenHeight - 58;
        
        // 分解钱包总数
        Map<CurrencyType, Long> breakdown = CurrencyUtils.breakdownCurrency(totalCopper);
        int iconCount = breakdown.size();
        int contentWidth = Math.max(font.width("+" + CurrencyUtils.formatCurrencyCompact(addedCopper)), iconCount * ICON_SPACING);
        
        // 背景（自适应宽度）
        int bgColor = (alpha * 180 / 255) << 24;
        graphics.fill(x - 2, y - 2, x + contentWidth + 4, y + LINE_HEIGHT + ICON_SIZE + 4, bgColor);
        
        // 第一行：获得的货币
        String addedText = "+" + CurrencyUtils.formatCurrencyCompact(addedCopper);
        int goldColor = 0xFFD700 | (alpha << 24);
        graphics.drawString(font, addedText, x, y, goldColor, true);
        
        // 第二行：钱包总数（图标+数字）
        int iconY = y + LINE_HEIGHT;
        int iconX = x;
        
        // 设置透明度
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alphaF);
        
        for (Map.Entry<CurrencyType, Long> entry : breakdown.entrySet()) {
            ItemStack coinStack = getCoinStack(entry.getKey());
            if (!coinStack.isEmpty()) {
                // 渲染图标
                graphics.renderItem(coinStack, iconX, iconY);
                
                // 数字显示在图标右侧
                String count = String.valueOf(entry.getValue());
                int countColor = 0xFFFFFF | (alpha << 24);
                graphics.drawString(font, count, iconX + ICON_SIZE + 1, iconY + 4, countColor, true);
                
                iconX += ICON_SPACING;
            }
        }
        
        // 恢复透明度
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    private static ItemStack getCoinStack(CurrencyType type) {
        return switch (type) {
            case COPPER -> new ItemStack(ModItems.COPPER_COIN.get());
            case SILVER -> new ItemStack(ModItems.SILVER_COIN.get());
            case GOLD -> new ItemStack(ModItems.GOLD_COIN.get());
            case EMERALD -> new ItemStack(ModItems.EMERALD_COIN.get());
            case DIAMOND -> new ItemStack(ModItems.DIAMOND_COIN.get());
        };
    }
    
    private static float calculateAlphaFloat(float progress) {
        if (progress > 0.8f) {
            return 1f - (progress - 0.8f) / 0.2f;
        } else if (progress < 0.2f) {
            return progress / 0.2f;
        }
        return 1.0f;
    }
}
