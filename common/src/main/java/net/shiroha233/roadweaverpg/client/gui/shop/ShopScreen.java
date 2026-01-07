package net.shiroha233.roadweaverpg.client.gui.shop;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.shop.ShopCategory;
import net.shiroha233.roadweaverpg.shop.ShopItem;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 商店界面 - 半透明黑色背景 + 物品格子 + 分类
 */
public class ShopScreen extends Screen {
    
    private static final int GUI_SIZE_RATIO = 85;
    private static final int TITLE_HEIGHT = 20;
    private static final int CATEGORY_HEIGHT = 24;
    private static final int ITEM_SIZE = 32;
    private static final int ITEM_SPACING = 4;
    private static final int PADDING = 12;
    private static final int COIN_COLOR = 0xFFFFD700;
    
    private final List<ShopItem> allItems;
    private final BiConsumer<ResourceLocation, Integer> purchaseHandler;
    
    private int playerCoins;
    private ShopCategory selectedCategory;
    private ShopItem hoveredItem = null;
    private int scrollOffset = 0;
    private int guiLeft, guiTop, guiWidth, guiHeight;
    
    public ShopScreen(int entityId, List<ShopItem> items, int playerCoins,
                      BiConsumer<ResourceLocation, Integer> purchaseHandler) {
        super(Component.translatable("gui.roadweaver_rpg.shop.title"));
        this.allItems = items;
        this.playerCoins = playerCoins;
        this.purchaseHandler = purchaseHandler;
        this.selectedCategory = findFirstNonEmptyCategory();
    }
    
    private ShopCategory findFirstNonEmptyCategory() {
        for (ShopCategory cat : ShopCategory.values()) {
            if (allItems.stream().anyMatch(i -> i.category() == cat)) {
                return cat;
            }
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
        renderBackground(graphics);
        renderMainPanel(graphics);
        renderTitle(graphics);
        renderCoinDisplay(graphics);
        renderCategoryTabs(graphics, mouseX, mouseY);
        renderItemGrid(graphics, mouseX, mouseY);
        
        if (hoveredItem != null) {
            renderItemTooltip(graphics, mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    
    private void renderMainPanel(GuiGraphics graphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xCC000000);
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + 1, 0xFF333333);
        graphics.fill(guiLeft, guiTop + guiHeight - 1, guiLeft + guiWidth, guiTop + guiHeight, 0xFF333333);
        graphics.fill(guiLeft, guiTop, guiLeft + 1, guiTop + guiHeight, 0xFF333333);
        graphics.fill(guiLeft + guiWidth - 1, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFF333333);
    }
    
    private void renderTitle(GuiGraphics graphics) {
        int titleY = guiTop - TITLE_HEIGHT - 5;
        int titleWidth = font.width(title) + 40;
        int titleX = guiLeft + guiWidth / 2 - titleWidth / 2;
        
        graphics.fill(titleX, titleY, titleX + titleWidth, titleY + TITLE_HEIGHT, 0xDD000000);
        graphics.fill(titleX, titleY, titleX + titleWidth, titleY + 1, 0xFF444444);
        graphics.fill(titleX, titleY + TITLE_HEIGHT - 1, titleX + titleWidth, titleY + TITLE_HEIGHT, 0xFF444444);
        
        graphics.drawCenteredString(font, title, guiLeft + guiWidth / 2, titleY + 6, 0xFFFFFFFF);
    }
    
    private void renderCoinDisplay(GuiGraphics graphics) {
        String coinText = "§6" + playerCoins;
        int coinTextWidth = font.width(coinText);
        int iconSize = 16;
        int totalWidth = coinTextWidth + iconSize + 4;
        
        int coinX = guiLeft + guiWidth - totalWidth - PADDING;
        int coinY = guiTop + PADDING;
        
        graphics.fill(coinX - 4, coinY - 2, coinX + totalWidth, coinY + 12, 0x80000000);
        graphics.drawString(font, coinText, coinX, coinY, COIN_COLOR, false);
        
        if (net.shiroha233.roadweaverpg.item.ModItems.COIN != null) {
            ItemStack coinStack = new ItemStack(net.shiroha233.roadweaverpg.item.ModItems.COIN.get());
            graphics.renderItem(coinStack, coinX + coinTextWidth + 4, coinY - 2);
        }

        int repLevel = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        String repText = "§bLv." + repLevel + " §7Guild Reputation";
        int repWidth = font.width(repText);
        int repX = guiLeft + guiWidth - repWidth - PADDING;
        int repY = coinY + 16;

        graphics.fill(repX - 4, repY - 2, repX + repWidth + 4, repY + 10, 0x80000000);
        graphics.drawString(font, repText, repX, repY, 0xFFFFFFFF, false);
    }
    
    private void renderCategoryTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabY = guiTop + PADDING;
        int tabX = guiLeft + PADDING;
        int tabWidth = 0;
        
        for (ShopCategory cat : ShopCategory.values()) {
            List<ShopItem> catItems = getItemsForCategory(cat);
            if (catItems.isEmpty()) continue;
            
            String catName = cat.getDisplayName().getString();
            int catWidth = font.width(catName) + 16;
            
            boolean hovered = mouseX >= tabX + tabWidth && mouseX < tabX + tabWidth + catWidth
                    && mouseY >= tabY && mouseY < tabY + CATEGORY_HEIGHT;
            boolean selected = cat == selectedCategory;
            
            int bgColor = selected ? (cat.getColor() & 0xFFFFFF) | 0xCC000000 : 
                         (hovered ? 0x60FFFFFF : 0x40000000);
            
            graphics.fill(tabX + tabWidth, tabY, tabX + tabWidth + catWidth, tabY + CATEGORY_HEIGHT, bgColor);
            
            if (selected) {
                graphics.fill(tabX + tabWidth, tabY + CATEGORY_HEIGHT - 2, 
                        tabX + tabWidth + catWidth, tabY + CATEGORY_HEIGHT, cat.getColor());
            }
            
            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFEEEEEE : 0xFFAAAAAA);
            graphics.drawString(font, catName, tabX + tabWidth + 8, tabY + 8, textColor, false);
            
            tabWidth += catWidth + 2;
        }
    }
    
    private void renderItemGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int gridX = guiLeft + PADDING;
        int gridY = guiTop + PADDING + CATEGORY_HEIGHT + 8;
        int gridWidth = guiWidth - PADDING * 2;
        int gridHeight = guiHeight - PADDING * 2 - CATEGORY_HEIGHT - 8;
        
        graphics.fill(gridX, gridY, gridX + gridWidth, gridY + gridHeight, 0x40000000);
        
        List<ShopItem> items = getItemsForCategory(selectedCategory);
        int cols = (gridWidth - 8) / (ITEM_SIZE + ITEM_SPACING);
        if (cols < 1) cols = 1;
        
        graphics.enableScissor(gridX, gridY, gridX + gridWidth, gridY + gridHeight);
        
        hoveredItem = null;
        
        for (int i = 0; i < items.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            
            int itemX = gridX + 4 + col * (ITEM_SIZE + ITEM_SPACING);
            int itemY = gridY + 4 + row * (ITEM_SIZE + ITEM_SPACING) - scrollOffset;
            
            if (itemY + ITEM_SIZE >= gridY && itemY < gridY + gridHeight) {
                ShopItem item = items.get(i);
                boolean hovered = mouseX >= itemX && mouseX < itemX + ITEM_SIZE
                        && mouseY >= itemY && mouseY < itemY + ITEM_SIZE;
                
                if (hovered) hoveredItem = item;
                renderItemSlot(graphics, itemX, itemY, item, hovered);
            }
        }
        
        graphics.disableScissor();
        
        int totalRows = (items.size() + cols - 1) / cols;
        int totalHeight = totalRows * (ITEM_SIZE + ITEM_SPACING);
        if (totalHeight > gridHeight) {
            renderScrollbar(graphics, gridX + gridWidth - 4, gridY, gridHeight, 
                    scrollOffset, totalHeight - gridHeight);
        }
    }

    
    private void renderItemSlot(GuiGraphics graphics, int x, int y, ShopItem item, boolean hovered) {
        boolean canAfford = playerCoins >= item.price();
        int playerRep = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        boolean levelMet = playerRep >= item.requiredLevel();
        
        int bgColor = hovered ? 0x80FFFFFF : 0x60333333;
        if (!canAfford || !levelMet) bgColor = (bgColor & 0xFF000000) | 0x442222;
        
        graphics.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, bgColor);
        
        int borderColor = hovered ? 0xFFFFFFFF : 0xFF555555;
        if (!canAfford || !levelMet) borderColor = 0xFFAA4444;
        graphics.fill(x, y, x + ITEM_SIZE, y + 1, borderColor);
        graphics.fill(x, y + ITEM_SIZE - 1, x + ITEM_SIZE, y + ITEM_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + ITEM_SIZE, borderColor);
        graphics.fill(x + ITEM_SIZE - 1, y, x + ITEM_SIZE, y + ITEM_SIZE, borderColor);
        
        ItemStack stack = item.createItemStack();
        graphics.renderItem(stack, x + (ITEM_SIZE - 16) / 2, y + 4);
        
        String priceStr = String.valueOf(item.price());
        int priceColor = canAfford ? COIN_COLOR : 0xFFFF4444;
        graphics.drawString(font, priceStr, x + (ITEM_SIZE - font.width(priceStr)) / 2, 
                y + ITEM_SIZE - 10, priceColor, false);

        if (!levelMet) {
            graphics.drawString(font, "§cL." + item.requiredLevel(), x + 2, y + 2, 0xFFFF4444, true);
        }
    }
    
    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int offset, int maxOffset) {
        if (maxOffset <= 0) return;
        
        graphics.fill(x, y, x + 3, y + height, 0x40FFFFFF);
        
        int thumbHeight = Math.max(20, height * height / (height + maxOffset));
        int thumbY = y + (int)((height - thumbHeight) * ((float)offset / maxOffset));
        
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xAAFFFFFF);
    }
    
    private void renderItemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        
        tooltip.add(hoveredItem.getDisplayName());
        tooltip.add(Component.literal("数量: " + hoveredItem.count()).withStyle(s -> s.withColor(0xAAAAAA)));
        
        int priceColor = playerCoins >= hoveredItem.price() ? 0xFFD700 : 0xFF4444;
        tooltip.add(Component.literal("价格: " + hoveredItem.price() + " 金币").withStyle(s -> s.withColor(priceColor)));
        
        int playerRep = net.shiroha233.roadweaverpg.client.ClientReputationCache.getPlayerLevel("roadweaver_rpg:guild");
        if (hoveredItem.requiredLevel() > 0) {
            int repColor = playerRep >= hoveredItem.requiredLevel() ? 0xAAAAFF : 0xFF5555;
            tooltip.add(Component.literal("需要声望等级: " + hoveredItem.requiredLevel())
                    .withStyle(s -> s.withColor(repColor)));
        }
        
        tooltip.add(Component.literal(""));
        if (playerRep < hoveredItem.requiredLevel()) {
            tooltip.add(Component.literal("声望不足").withStyle(s -> s.withColor(0xFF5555).withItalic(true)));
        } else if (playerCoins >= hoveredItem.price()) {
            tooltip.add(Component.literal("点击购买").withStyle(s -> s.withColor(0x55FF55).withItalic(true)));
        } else {
            tooltip.add(Component.literal("金币不足").withStyle(s -> s.withColor(0xFF5555).withItalic(true)));
        }
        
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }
    
    private List<ShopItem> getItemsForCategory(ShopCategory category) {
        return allItems.stream()
                .filter(item -> item.category() == category)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tabY = guiTop + PADDING;
            int tabX = guiLeft + PADDING;
            int tabWidth = 0;
            
            for (ShopCategory cat : ShopCategory.values()) {
                List<ShopItem> catItems = getItemsForCategory(cat);
                if (catItems.isEmpty()) continue;
                
                String catName = cat.getDisplayName().getString();
                int catWidth = font.width(catName) + 16;
                
                if (mouseX >= tabX + tabWidth && mouseX < tabX + tabWidth + catWidth
                        && mouseY >= tabY && mouseY < tabY + CATEGORY_HEIGHT) {
                    selectedCategory = cat;
                    scrollOffset = 0;
                    return true;
                }
                tabWidth += catWidth + 2;
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
        int gridHeight = guiHeight - PADDING * 2 - CATEGORY_HEIGHT - 8;
        int gridWidth = guiWidth - PADDING * 2;
        int cols = (gridWidth - 8) / (ITEM_SIZE + ITEM_SPACING);
        if (cols < 1) cols = 1;
        
        List<ShopItem> items = getItemsForCategory(selectedCategory);
        int totalRows = (items.size() + cols - 1) / cols;
        int totalHeight = totalRows * (ITEM_SIZE + ITEM_SPACING);
        int maxOffset = Math.max(0, totalHeight - gridHeight);
        
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
