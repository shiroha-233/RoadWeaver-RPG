package net.shiroha233.roadweaverpg.client.gui.shop;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.currency.CurrencyType;
import net.shiroha233.roadweaverpg.item.ModItems;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商店右上角状态面板：多币种余额 + 声望等级
 */
public final class ShopStatusPanel {

    private static final int COLOR_PANEL_BG = 0x50101020;
    private static final int COLOR_OUTLINE = 0x15FFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFB0B0B0;

    private static final int PADDING_X = 10;
    private static final int SEGMENT_GAP = 10;
    private static final int AFTER_COINS_GAP = 12;
    private static final int SEPARATOR_GAP = 12;

    private static final EnumMap<CurrencyType, ItemStack> ICON_CACHE = new EnumMap<>(CurrencyType.class);

    private ShopStatusPanel() {
    }

    public static int measureWidth(Font font, long copperCoins, int repLevel) {
        Map<CurrencyType, Long> breakdown = breakdownAll(copperCoins);

        int w = PADDING_X;

        boolean first = true;
        for (Map.Entry<CurrencyType, Long> entry : breakdown.entrySet()) {
            if (!first) {
                w += SEGMENT_GAP;
            }
            first = false;

            w += 18;

            String value = String.valueOf(entry.getValue());
            w += font.width(value);
        }

        w += AFTER_COINS_GAP;
        w += 1;
        w += SEPARATOR_GAP;

        w += font.width("声望") + 4 + font.width("Lv." + repLevel);

        w += PADDING_X;
        return w;
    }

    public static void render(GuiGraphics graphics, Font font, int x, int y, int w, int h, long copperCoins, int repLevel) {
        Map<CurrencyType, Long> breakdown = breakdownAll(copperCoins);

        GuiRenderer.drawRoundedRect(graphics, x, y, w, h, 12, COLOR_PANEL_BG);
        graphics.renderOutline(x, y, w, h, COLOR_OUTLINE);

        int currentX = x + PADDING_X;
        int centerY = y + h / 2;

        boolean first = true;
        for (Map.Entry<CurrencyType, Long> entry : breakdown.entrySet()) {
            if (!first) {
                currentX += SEGMENT_GAP;
            }
            first = false;

            ItemStack icon = getCoinIcon(entry.getKey());
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, currentX, centerY - 8);
            }
            currentX += 18;

            String value = String.valueOf(entry.getValue());
            int color = 0xFF000000 | entry.getKey().getColor();
            graphics.drawString(font, value, currentX, centerY - 4, color, false);
            currentX += font.width(value);
        }

        currentX += AFTER_COINS_GAP;

        graphics.fill(currentX, centerY - 6, currentX + 1, centerY + 6, 0x30FFFFFF);
        currentX += SEPARATOR_GAP;

        graphics.drawString(font, "声望", currentX, centerY - 4, COLOR_TEXT_SECONDARY, false);
        currentX += font.width("声望") + 4;

        String lvlStr = "Lv." + repLevel;
        graphics.drawString(font, lvlStr, currentX, centerY - 4, 0xFF55FF55, false);
    }

    private static Map<CurrencyType, Long> breakdownAll(long copperCoins) {
        Map<CurrencyType, Long> result = new LinkedHashMap<>();

        long remaining = Math.max(0, copperCoins);
        for (CurrencyType type : new CurrencyType[]{
                CurrencyType.DIAMOND,
                CurrencyType.EMERALD,
                CurrencyType.GOLD,
                CurrencyType.SILVER,
                CurrencyType.COPPER
        }) {
            long count = remaining / type.getValueInCopper();
            result.put(type, count);
            remaining %= type.getValueInCopper();
        }
        return result;
    }

    private static ItemStack getCoinIcon(CurrencyType type) {
        ItemStack cached = ICON_CACHE.get(type);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        Item item = switch (type) {
            case COPPER -> ModItems.COPPER_COIN != null ? ModItems.COPPER_COIN.get() : null;
            case SILVER -> ModItems.SILVER_COIN != null ? ModItems.SILVER_COIN.get() : null;
            case GOLD -> ModItems.GOLD_COIN != null ? ModItems.GOLD_COIN.get() : null;
            case EMERALD -> ModItems.EMERALD_COIN != null ? ModItems.EMERALD_COIN.get() : null;
            case DIAMOND -> ModItems.DIAMOND_COIN != null ? ModItems.DIAMOND_COIN.get() : null;
        };

        ItemStack stack = item != null ? new ItemStack(item) : ItemStack.EMPTY;
        ICON_CACHE.put(type, stack);
        return stack;
    }
}
