package net.shiroha233.roadweaverpg.client.gui.shop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.shop.ShopCategory;
import net.shiroha233.roadweaverpg.shop.ShopItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 商店界面 - 半透明黑色背景 + 物品格子 + 分类
 */
public class ShopScreen extends Screen {
    
    private static final int GUI_SIZE_RATIO = 85;
    private static final int HEADER_HEIGHT = 38; // 稍微增高头部
    private static final int CHIP_HEIGHT = 24;   // 增高标签
    private static final int ITEM_SIZE = 36;     // 增大物品格子
    private static final int ITEM_SPACING = 8;   // 增加间距
    private static final int GRID_INNER_PADDING = 12;
    private static final int PADDING = 20;       // 增加整体内边距
    private static final int COIN_COLOR = 0xFFFFD700;
    private static final int COLOR_TEXT_SECONDARY = 0xFFB0B0B0;
    
    private final Map<ShopCategory, List<ShopItem>> itemsByCategory;
    private final List<ShopCategory> availableCategories;
    private final BiConsumer<ResourceLocation, Integer> purchaseHandler;
    
    private int playerCoins;
    private ShopCategory selectedCategory;
    private ShopItem hoveredItem = null;
    private int scrollOffset = 0;
    private int guiLeft, guiTop, guiWidth, guiHeight;
    
    public ShopScreen(int entityId, List<ShopItem> items, int playerCoins,
                      BiConsumer<ResourceLocation, Integer> purchaseHandler) {
        super(Component.translatable("gui.roadweaver_rpg.shop.title"));
        this.playerCoins = playerCoins;
        this.purchaseHandler = purchaseHandler;
        this.itemsByCategory = buildItemsByCategory(items);
        this.availableCategories = buildAvailableCategories();
        this.selectedCategory = findFirstNonEmptyCategory();
    }
    
    private Map<ShopCategory, List<ShopItem>> buildItemsByCategory(List<ShopItem> items) {
        Map<ShopCategory, List<ShopItem>> map = new EnumMap<>(ShopCategory.class);
        for (ShopCategory cat : ShopCategory.values()) {
            map.put(cat, new ArrayList<>());
        }
        for (ShopItem item : items) {
            map.get(item.category()).add(item);
        }
        return map;
    }
    
    private List<ShopCategory> buildAvailableCategories() {
        List<ShopCategory> result = new ArrayList<>();
        for (ShopCategory cat : ShopCategory.values()) {
            if (!getItemsForCategory(cat).isEmpty()) {
                result.add(cat);
            }
        }
        return result;
    }
    
    private ShopCategory findFirstNonEmptyCategory() {
        for (ShopCategory cat : availableCategories) {
            return cat;
        }
        return ShopCategory.MATERIALS;
    }
    
    @Override
    protected void init() {
        super.init();
        guiWidth = width * GUI_SIZE_RATIO / 100;
        guiHeight = height * GUI_SIZE_RATIO / 100;
        guiLeft = (width - guiWidth) / 2;
        guiTop = (height - guiHeight) / 2;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderMainPanel(graphics);
        renderHeader(graphics, mouseX, mouseY);
        renderItemGrid(graphics, mouseX, mouseY);
        
        if (hoveredItem != null) {
            renderItemTooltip(graphics, mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    
    private void renderMainPanel(GuiGraphics graphics) {
        renderBackground(graphics); 
        
        // 主面板背景 - 更现代的半透明深色玻璃质感
        // 使用更低的 Alpha (0xB0 -> ~70%)，增加通透感
        // 圆角半径增大到 16
        GuiRenderer.drawRoundedRect(graphics, guiLeft, guiTop, guiWidth, guiHeight, 16, 0xB0101218);
        
        // 叠加微弱的渐变，增加质感
        GuiRenderer.drawVerticalGradient(graphics, guiLeft + 2, guiTop + 2, guiWidth - 4, guiHeight - 4, 0x15FFFFFF, 0x05000000);
        
        // 外边框 - 极细发光感
        graphics.renderOutline(guiLeft, guiTop, guiWidth, guiHeight, 0x20FFFFFF);
    }
    
    // 移除独立的 renderTitle，标题可以在头部左侧显示，或者直接省略，因为Header很明显
    
    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        HeaderLayout header = computeHeaderLayout();
        Rect statusRect = computeStatusPanelRect(header);
        
        // 头部区域不再画明显的深色背景框，而是保持通透，或者仅画一条分割线
        // 这里选择画一个极淡的背景条
        // GuiRenderer.drawRoundedRect(graphics, header.headerX, header.headerY, header.headerW, header.headerH, 12, 0x20000000);
        
        renderStatusPanel(graphics, statusRect);
        renderCategoryChips(graphics, header, statusRect.x, mouseX, mouseY);
    }
    
    private void renderStatusPanel(GuiGraphics graphics, Rect statusRect) {
        int repLevel = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        
        String coinValue = String.valueOf(playerCoins);
        
        // 状态面板背景 - 胶囊状
        GuiRenderer.drawRoundedRect(graphics, statusRect.x, statusRect.y, statusRect.w, statusRect.h, 12, 0x40000000);
        graphics.renderOutline(statusRect.x, statusRect.y, statusRect.w, statusRect.h, 0x15FFFFFF);
        
        int padding = 10;
        int currentX = statusRect.x + padding;
        int centerY = statusRect.y + statusRect.h / 2;
        
        // 1. 金币图标和数值
        if (net.shiroha233.roadweaverpg.item.ModItems.COIN != null) {
            ItemStack coinStack = new ItemStack(net.shiroha233.roadweaverpg.item.ModItems.COIN.get());
            graphics.renderItem(coinStack, currentX, centerY - 8);
            currentX += 18;
        }
        
        graphics.drawString(font, coinValue, currentX, centerY - 4, COIN_COLOR, false);
        currentX += font.width(coinValue) + 12;
        
        // 分隔符 - 垂直细线
        graphics.fill(currentX, centerY - 6, currentX + 1, centerY + 6, 0x30FFFFFF);
        currentX += 12;
        
        // 2. 声望信息
        graphics.drawString(font, "声望", currentX, centerY - 4, COLOR_TEXT_SECONDARY, false);
        currentX += font.width("声望") + 4;
        
        String lvlStr = "Lv." + repLevel;
        graphics.drawString(font, lvlStr, currentX, centerY - 4, 0xFF55FF55, false);
    }
    
    private void renderCategoryChips(GuiGraphics graphics, HeaderLayout header, int statusLeftX, int mouseX, int mouseY) {
        int headerTextY = header.headerY + (header.headerH - CHIP_HEIGHT) / 2;
        
        int x = header.headerX + 4; // 起始位置微调
        int maxX = statusLeftX - 10;
        
        graphics.enableScissor(header.headerX, header.headerY, statusLeftX, header.headerY + header.headerH);
        
        for (ShopCategory cat : availableCategories) {
            String name = cat.getDisplayName().getString();
            int textWidth = font.width(name);
            int chipW = textWidth + 20; // 左右各10padding
            
            if (x + chipW > maxX) break;
            
            boolean hovered = mouseX >= x && mouseX < x + chipW && mouseY >= headerTextY && mouseY < headerTextY + CHIP_HEIGHT;
            boolean selected = cat == selectedCategory;
            
            // 优化颜色逻辑：确保未选中状态也清晰可见
            int categoryColor = cat.getColor();
            
            int bg;
            int borderColor;
            int textColor;
            
            if (selected) {
                // 选中：高亮背景，白色文字，亮边框
                bg = (categoryColor & 0x00FFFFFF) | 0xCC000000; // 80% alpha
                borderColor = (categoryColor & 0x00FFFFFF) | 0xFF000000;
                textColor = 0xFFFFFFFF;
            } else {
                // 未选中：深色背景，灰色文字，暗边框
                // 悬停时稍微提亮背景
                bg = hovered ? 0x60404040 : 0x40202020; 
                borderColor = hovered ? 0x40FFFFFF : 0x20FFFFFF;
                textColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
            }
            
            // 绘制胶囊状标签 (圆角半径 = 高度的一半 = 12)
            GuiRenderer.drawRoundedRect(graphics, x, headerTextY, chipW, CHIP_HEIGHT, 12, bg);
            graphics.renderOutline(x, headerTextY, chipW, CHIP_HEIGHT, borderColor);
            
            // 居中绘制文字
            graphics.drawCenteredString(font, name, x + chipW / 2, headerTextY + 8, textColor);
            
            x += chipW + 8; // 增加间距
        }
        
        graphics.disableScissor();
    }
    
    private void renderItemGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        GridLayout layout = computeGridLayout();
        
        // 物品网格背景 - 更通透
        GuiRenderer.drawRoundedRect(graphics, layout.gridX, layout.gridY, layout.gridW, layout.gridH, 12, 0x25000000);
        // graphics.renderOutline(layout.gridX, layout.gridY, layout.gridW, layout.gridH, 0x10FFFFFF); // 去除网格边框，更简洁
        
        List<ShopItem> items = getItemsForCategory(selectedCategory);
        int cols = Math.max(1, (layout.innerW + ITEM_SPACING) / (ITEM_SIZE + ITEM_SPACING));
        
        graphics.enableScissor(layout.gridX, layout.gridY, layout.gridX + layout.gridW, layout.gridY + layout.gridH);
        
        hoveredItem = null;
        
        int totalRows = (items.size() + cols - 1) / cols;
        int totalHeight = totalRows * (ITEM_SIZE + ITEM_SPACING) - ITEM_SPACING;
        int maxOffset = Math.max(0, totalHeight - layout.innerH);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset));
        
        for (int i = 0; i < items.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            
            int itemX = layout.innerX + col * (ITEM_SIZE + ITEM_SPACING);
            int itemY = layout.innerY + row * (ITEM_SIZE + ITEM_SPACING) - scrollOffset;
            
            if (itemY + ITEM_SIZE >= layout.gridY && itemY < layout.gridY + layout.gridH) {
                ShopItem item = items.get(i);
                boolean hovered = mouseX >= itemX && mouseX < itemX + ITEM_SIZE
                        && mouseY >= itemY && mouseY < itemY + ITEM_SIZE;
                
                if (hovered) hoveredItem = item;
                renderItemSlot(graphics, itemX, itemY, item, hovered);
            }
        }
        
        graphics.disableScissor();
        
        if (maxOffset > 0) {
            renderScrollbar(graphics, layout.gridX + layout.gridW - 6, layout.gridY + 6, layout.gridH - 12, scrollOffset, maxOffset);
        }
    }

    
    private void renderItemSlot(GuiGraphics graphics, int x, int y, ShopItem item, boolean hovered) {
        boolean canAfford = playerCoins >= item.price();
        int playerRep = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        boolean levelMet = playerRep >= item.requiredLevel();
        
        boolean disabled = !canAfford || !levelMet;
        
        // 背景色调整
        int bgColor;
        if (disabled) {
            bgColor = 0x40301010; // 淡红色背景表示不可用
        } else {
            bgColor = hovered ? 0x50FFFFFF : 0x30000000; // 悬停亮白，平时深黑
        }
        
        // 绘制圆角格子 (半径8)
        GuiRenderer.drawRoundedRect(graphics, x, y, ITEM_SIZE, ITEM_SIZE, 8, bgColor);
        
        // 边框 - 选中时发光
        int outline = disabled ? 0x60FF4444 : (hovered ? 0xA0FFFFFF : 0x20FFFFFF);
        graphics.renderOutline(x, y, ITEM_SIZE, ITEM_SIZE, outline);
        
        // 物品图标 - 居中 (36x36 格子, 16x16 图标, 居中是 10,10)
        ItemStack stack = item.createItemStack();
        graphics.renderItem(stack, x + 10, y + 10); 
        
        // 价格显示在右下角
        String priceStr = String.valueOf(item.price());
        int priceColor = canAfford ? COIN_COLOR : 0xFFFF5555;
        
        // 价格背景条 (增强可读性)
        // graphics.pose().pushPose();
        // graphics.pose().translate(0, 0, 150);
        // GuiRenderer.drawRoundedRect(graphics, x + ITEM_SIZE - font.width(priceStr)*0.7f - 4, y + ITEM_SIZE - 9, (int)(font.width(priceStr)*0.7f)+2, 8, 4, 0x80000000);
        // graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(x + ITEM_SIZE - 2, y + ITEM_SIZE - 8, 200);
        graphics.pose().scale(0.7f, 0.7f, 1.0f);
        graphics.drawString(font, priceStr, -font.width(priceStr), 0, priceColor, true); // 开启阴影
        graphics.pose().popPose();

        // 等级限制显示在左上角
        if (!levelMet) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 3, y + 3, 300); 
            graphics.pose().scale(0.7f, 0.7f, 1.0f);
            String lvlText = "L." + item.requiredLevel();
            graphics.drawString(font, lvlText, 0, 0, 0xFFFF5555, true);
            graphics.pose().popPose();
            
            // 红色遮罩
            GuiRenderer.drawRoundedRect(graphics, x, y, ITEM_SIZE, ITEM_SIZE, 8, 0x30FF0000);
        }
        
        // 如果有数量 > 1，显示数量
        if (item.count() > 1) {
             graphics.pose().pushPose();
             graphics.pose().translate(x + ITEM_SIZE - 2, y + ITEM_SIZE - 15, 200);
             graphics.pose().scale(0.7f, 0.7f, 1.0f);
             String countStr = "x" + item.count();
             graphics.drawString(font, countStr, -font.width(countStr), 0, 0xFFDDDDDD, true);
             graphics.pose().popPose();
        }
    }
    
    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int offset, int maxOffset) {
        if (maxOffset <= 0) return;
        
        // 滚动条槽
        GuiRenderer.drawRoundedRect(graphics, x, y, 4, height, 2, 0x10FFFFFF);
        
        int thumbHeight = Math.max(20, height * height / (height + maxOffset));
        int thumbY = y + (int)((height - thumbHeight) * ((float)offset / maxOffset));
        
        // 滚动条滑块 - 亮色
        GuiRenderer.drawRoundedRect(graphics, x, thumbY, 4, thumbHeight, 2, 0x60FFFFFF);
    }
    
    private void renderItemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack stack = hoveredItem.createItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = new ArrayList<>(stack.getTooltipLines(Minecraft.getInstance().player, 
                Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
        
        // 分隔线
        tooltip.add(Component.empty());
        
        // 商店信息
        tooltip.add(Component.literal("商品信息").withStyle(s -> s.withColor(0xFFD700).withBold(true)));
        tooltip.add(Component.literal("  数量: " + hoveredItem.count()).withStyle(s -> s.withColor(0xAAAAAA)));
        
        int priceColor = playerCoins >= hoveredItem.price() ? 0x55FF55 : 0xFF5555;
        tooltip.add(Component.literal("  价格: " + hoveredItem.price() + " 金币").withStyle(s -> s.withColor(priceColor)));
        
        int playerRep = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        if (hoveredItem.requiredLevel() > 0) {
            int repColor = playerRep >= hoveredItem.requiredLevel() ? 0xAAAAFF : 0xFF5555;
            tooltip.add(Component.literal("  需要声望等级: " + hoveredItem.requiredLevel())
                    .withStyle(s -> s.withColor(repColor)));
        }
        
        tooltip.add(Component.empty());
        if (playerRep < hoveredItem.requiredLevel()) {
            tooltip.add(Component.literal("声望不足").withStyle(s -> s.withColor(0xFF5555).withItalic(true)));
        } else if (playerCoins >= hoveredItem.price()) {
            tooltip.add(Component.literal("点击购买").withStyle(s -> s.withColor(0x55FF55).withItalic(true)));
        } else {
            tooltip.add(Component.literal("金币不足").withStyle(s -> s.withColor(0xFF5555).withItalic(true)));
        }
        
        graphics.renderTooltip(font, tooltip, stack.getTooltipImage(), mouseX, mouseY);
    }
    
    private List<ShopItem> getItemsForCategory(ShopCategory category) {
        return itemsByCategory.getOrDefault(category, List.of());
    }
    
    private HeaderLayout computeHeaderLayout() {
        int headerX = guiLeft + PADDING;
        int headerY = guiTop + PADDING;
        int headerW = guiWidth - PADDING * 2;
        int headerH = HEADER_HEIGHT;
        
        return new HeaderLayout(headerX, headerY, headerW, headerH);
    }
    
    private Rect computeStatusPanelRect(HeaderLayout header) {
        int repLevel = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        
        String coinValue = String.valueOf(playerCoins);
        
        // 估算宽度
        int coinWidth = 18 + font.width(coinValue) + 12; // 图标+文字+间距
        int repWidth = font.width("声望") + 4 + font.width("Lv." + repLevel);
        
        int panelW = coinWidth + repWidth + 24; // 总宽度 + padding
        int panelH = header.headerH - 8;
        
        int panelX = header.headerX + header.headerW - panelW - 4;
        int panelY = header.headerY + 4;
        
        return new Rect(panelX, panelY, panelW, panelH);
    }
    
    private GridLayout computeGridLayout() {
        HeaderLayout header = computeHeaderLayout();
        int gridX = guiLeft + PADDING;
        int gridY = header.headerY + header.headerH + 12;
        int gridW = guiWidth - PADDING * 2;
        int gridH = guiHeight - (gridY - guiTop) - PADDING;
        
        int innerX = gridX + GRID_INNER_PADDING;
        int innerY = gridY + GRID_INNER_PADDING;
        int innerW = gridW - GRID_INNER_PADDING * 2;
        int innerH = gridH - GRID_INNER_PADDING * 2;
        
        return new GridLayout(gridX, gridY, gridW, gridH, innerX, innerY, innerW, innerH);
    }
    
    private record HeaderLayout(int headerX, int headerY, int headerW, int headerH) {}
    
    private record Rect(int x, int y, int w, int h) {}
    
    private record GridLayout(int gridX, int gridY, int gridW, int gridH, int innerX, int innerY, int innerW, int innerH) {}
    
    private ShopCategory getCategoryAt(double mouseX, double mouseY) {
        HeaderLayout header = computeHeaderLayout();
        Rect statusRect = computeStatusPanelRect(header);
        
        int chipY = header.headerY + (header.headerH - CHIP_HEIGHT) / 2;
        if (mouseY < chipY || mouseY >= chipY + CHIP_HEIGHT) return null;
        
        int x = header.headerX + 4;
        int maxX = statusRect.x - 10;
        
        for (ShopCategory cat : availableCategories) {
            String name = cat.getDisplayName().getString();
            int chipW = font.width(name) + 20;
            if (x + chipW > maxX) break;
            
            if (mouseX >= x && mouseX < x + chipW) {
                return cat;
            }
            
            x += chipW + 8;
        }
        
        return null;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ShopCategory clickedCategory = getCategoryAt(mouseX, mouseY);
            if (clickedCategory != null) {
                selectedCategory = clickedCategory;
                scrollOffset = 0;
                return true;
            }
            
            if (hoveredItem != null) {
                int playerRep = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
                if (playerCoins >= hoveredItem.price() && playerRep >= hoveredItem.requiredLevel()) {
                    purchaseHandler.accept(hoveredItem.id(), 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        GridLayout layout = computeGridLayout();
        if (mouseX < layout.gridX || mouseX >= layout.gridX + layout.gridW || mouseY < layout.gridY || mouseY >= layout.gridY + layout.gridH) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        
        List<ShopItem> items = getItemsForCategory(selectedCategory);
        int cols = Math.max(1, (layout.innerW + ITEM_SPACING) / (ITEM_SIZE + ITEM_SPACING));
        int totalRows = (items.size() + cols - 1) / cols;
        int totalHeight = totalRows * (ITEM_SIZE + ITEM_SPACING) - ITEM_SPACING;
        int maxOffset = Math.max(0, totalHeight - layout.innerH);
        if (maxOffset <= 0) {
            return true;
        }
        
        scrollOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - delta * 20));
        return true;
    }
    
    public void updateCoins(int coins) {
        this.playerCoins = coins;
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
