package net.shiroha233.roadweaverpg.client.gui.character;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.client.ClientAdventureCache;
import net.shiroha233.roadweaverpg.client.ClientStatAllocationCache;
import net.shiroha233.roadweaverpg.client.ClientStatsCache;
import net.shiroha233.roadweaverpg.client.ClientWalletCache;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.List;
import java.util.function.Consumer;

/**
 * 角色界面 - 支持技能点加点的RPG属性面板
 * 布局：左侧分类标签 + 中间玩家模型 + 右侧属性数值
 */
public class CharacterScreen extends Screen {
    
    // 布局常量
    private static final int MARGIN = 20;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 28;
    private static final int TAB_SPACING = 4;
    private static final int PANEL_RADIUS = 12;
    
    // 颜色常量
    private static final int COLOR_BG = 0xA0000000;
    private static final int COLOR_PANEL_BG = 0x60101020;
    private static final int COLOR_TAB_NORMAL = 0x40FFFFFF;
    private static final int COLOR_TAB_HOVER = 0x60FFFFFF;
    private static final int COLOR_TAB_SELECTED = 0x80FFD700;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_SKILL_POINTS = 0xFF44FF44;
    
    // 滚动相关
    private float scrollOffset = 0;
    private float maxScroll = 0;
    
    // 标签页类型
    private enum TabType {
        BASIC("基础"),
        COMBAT("战斗"),
        SPECIAL("特殊"),
        PROFESSION("职业");
        
        private final String displayName;
        TabType(String name) { this.displayName = name; }
        public String getDisplayName() { return displayName; }
    }
    
    // 当前选中的标签页
    private int selectedTab = 0;
    private final List<TabType> tabs = List.of(
            TabType.BASIC, TabType.COMBAT, TabType.SPECIAL, TabType.PROFESSION
    );
    
    // 渲染器
    private ProfessionTabRenderer professionTabRenderer;
    private StatsTabRenderer statsTabRenderer;
    
    // 回调（静态，由平台特定代码设置）
    private static Consumer<StatType> staticOnAllocatePoint;
    private static Consumer<StatType> staticOnDeallocatePoint;
    private static Runnable staticOnResetAllocation;
    
    public CharacterScreen() {
        super(Component.translatable("gui.roadweaver_rpg.character.title"));
    }
    
    public static void setOnAllocatePoint(Consumer<StatType> callback) {
        staticOnAllocatePoint = callback;
    }
    
    public static void setOnDeallocatePoint(Consumer<StatType> callback) {
        staticOnDeallocatePoint = callback;
    }
    
    public static void setOnResetAllocation(Runnable callback) {
        staticOnResetAllocation = callback;
    }
    
    @Override
    protected void init() {
        super.init();
        professionTabRenderer = new ProfessionTabRenderer(font);
        statsTabRenderer = new StatsTabRenderer(font);
    }
    
    @Override
    public void tick() {
        super.tick();
        ClientStatsCache.updateFromPlayer();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ClientStatsCache.updateFromPlayer();
        graphics.fill(0, 0, width, height, COLOR_BG);
        
        int panelWidth = Math.min(680, width - 40);
        int panelHeight = Math.min(380, height - 40);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        
        GuiRenderer.drawRoundedRect(graphics, panelX, panelY, panelWidth, panelHeight, PANEL_RADIUS, COLOR_PANEL_BG);
        
        renderHeader(graphics, panelX, panelY - 30, panelWidth);
        renderCategoryTabs(graphics, panelX + MARGIN, panelY + MARGIN, mouseX, mouseY);
        renderPlayerModel(graphics, panelX + TAB_WIDTH + MARGIN * 2 + 80, panelY + panelHeight / 2 + 20, mouseX, mouseY);
        
        int statsX = panelX + TAB_WIDTH + MARGIN * 2 + 160;
        int statsY = panelY + MARGIN;
        int statsWidth = panelWidth - TAB_WIDTH - MARGIN * 3 - 160;
        int statsHeight = panelHeight - MARGIN * 2;
        renderStatsPanel(graphics, statsX, statsY, statsWidth, statsHeight, mouseX, mouseY);
        
        renderSkillPointsInfo(graphics, panelX, panelY + panelHeight + 5, panelWidth, mouseX, mouseY);
        
        // 渲染tooltip
        StatType hoveredType = statsTabRenderer != null ? statsTabRenderer.getHoveredStatType() : null;
        if (hoveredType != null) {
            graphics.renderComponentTooltip(font, statsTabRenderer.getStatTooltip(hoveredType), mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderHeader(GuiGraphics graphics, int x, int y, int panelWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        String playerName = mc.player.getName().getString();
        graphics.drawString(font, playerName, x, y, COLOR_TITLE, true);
        
        int advLevel = ClientAdventureCache.getPlayerLevel();
        graphics.drawString(font, "Lv." + advLevel, x + font.width(playerName) + 10, y, 0xFFFFFFFF, true);
        
        long coins = ClientWalletCache.getCoins();
        String coinText = CurrencyUtils.formatCurrencyCompact(coins);
        graphics.drawString(font, coinText, x + panelWidth - font.width(coinText), y, 0xFFFFD700, true);
    }
    
    private void renderCategoryTabs(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < tabs.size(); i++) {
            int tabY = y + i * (TAB_HEIGHT + TAB_SPACING);
            boolean selected = i == selectedTab;
            boolean hovered = mouseX >= x && mouseX < x + TAB_WIDTH && 
                             mouseY >= tabY && mouseY < tabY + TAB_HEIGHT;
            
            int bgColor = selected ? COLOR_TAB_SELECTED : (hovered ? COLOR_TAB_HOVER : COLOR_TAB_NORMAL);
            GuiRenderer.drawRoundedRect(graphics, x, tabY, TAB_WIDTH, TAB_HEIGHT, 6, bgColor);
            
            String name = tabs.get(i).getDisplayName();
            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            graphics.drawString(font, name, x + (TAB_WIDTH - font.width(name)) / 2, 
                    tabY + (TAB_HEIGHT - 8) / 2, textColor, false);
        }
    }
    
    private void renderPlayerModel(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, x, y, 55, (float)(x) - mouseX, (float)(y - 80) - mouseY, mc.player);
    }
    
    private void renderStatsPanel(GuiGraphics graphics, int x, int y, int areaWidth, int areaHeight,
                                   int mouseX, int mouseY) {
        GuiRenderer.drawRoundedRect(graphics, x, y, areaWidth, areaHeight, 8, 0x40000000);
        graphics.enableScissor(x, y, x + areaWidth, y + areaHeight);
        
        TabType currentTab = tabs.get(selectedTab);
        if (currentTab == TabType.PROFESSION) {
            professionTabRenderer.render(graphics, x + 10, y + 10 - (int)scrollOffset, areaWidth - 20);
            maxScroll = Math.max(0, professionTabRenderer.calculateContentHeight() - areaHeight + 20);
        } else {
            StatType.StatCategory category = tabToCategory(currentTab);
            statsTabRenderer.render(graphics, x + 10, y + 10 - (int)scrollOffset, areaWidth - 20, 
                    mouseX, mouseY, category, scrollOffset);
            maxScroll = Math.max(0, statsTabRenderer.calculateContentHeight(category) - areaHeight + 20);
        }
        
        graphics.disableScissor();
    }
    
    private StatType.StatCategory tabToCategory(TabType tab) {
        return switch (tab) {
            case BASIC -> StatType.StatCategory.BASIC;
            case COMBAT -> StatType.StatCategory.COMBAT;
            case SPECIAL -> StatType.StatCategory.SPECIAL;
            case PROFESSION -> null;
        };
    }
    
    private void renderSkillPointsInfo(GuiGraphics graphics, int x, int y, int panelWidth, int mouseX, int mouseY) {
        int availablePoints = ClientStatAllocationCache.getAvailablePoints();
        int totalAllocated = ClientStatAllocationCache.getTotalAllocatedPoints();
        
        String pointsText = Component.translatable("gui.roadweaver_rpg.skill_points", availablePoints).getString();
        graphics.drawString(font, pointsText, x, y, availablePoints > 0 ? COLOR_SKILL_POINTS : 0xFFAAAAAA, true);
        
        if (totalAllocated > 0) {
            String resetText = Component.translatable("gui.roadweaver_rpg.reset_stats").getString();
            int resetWidth = font.width(resetText) + 10;
            int resetX = x + panelWidth - resetWidth;
            int resetY = y - 2;
            int resetHeight = 14;
            
            boolean resetHovered = mouseX >= resetX && mouseX < resetX + resetWidth &&
                                   mouseY >= resetY && mouseY < resetY + resetHeight;
            
            int resetBgColor = resetHovered ? 0x80FF4444 : 0x40FF4444;
            GuiRenderer.drawRoundedRect(graphics, resetX, resetY, resetWidth, resetHeight, 4, resetBgColor);
            graphics.drawString(font, resetText, resetX + 5, resetY + 3, 0xFFFFFFFF, false);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(680, width - 40);
        int panelHeight = Math.min(380, height - 40);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        
        // 标签页点击
        int tabX = panelX + MARGIN;
        int tabY = panelY + MARGIN;
        for (int i = 0; i < tabs.size(); i++) {
            int currentTabY = tabY + i * (TAB_HEIGHT + TAB_SPACING);
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH && 
                mouseY >= currentTabY && mouseY < currentTabY + TAB_HEIGHT) {
                selectedTab = i;
                scrollOffset = 0;
                return true;
            }
        }
        
        // 属性按钮点击（非职业标签页）
        TabType currentTab = tabs.get(selectedTab);
        if (currentTab != TabType.PROFESSION) {
            int statsX = panelX + TAB_WIDTH + MARGIN * 2 + 160;
            int statsY = panelY + MARGIN;
            int statsWidth = panelWidth - TAB_WIDTH - MARGIN * 3 - 160;
            
            if (statsTabRenderer.handleClick(mouseX, mouseY, statsX, statsY, statsWidth,
                    tabToCategory(currentTab), scrollOffset, staticOnAllocatePoint, staticOnDeallocatePoint)) {
                return true;
            }
        }
        
        // 重置按钮点击
        if (handleResetClick(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean handleResetClick(double mouseX, double mouseY,
                                      int panelX, int panelY, int panelWidth, int panelHeight) {
        int totalAllocated = ClientStatAllocationCache.getTotalAllocatedPoints();
        if (totalAllocated <= 0) return false;
        
        String resetText = Component.translatable("gui.roadweaver_rpg.reset_stats").getString();
        int resetWidth = font.width(resetText) + 10;
        int resetX = panelX + panelWidth - resetWidth;
        int resetY = panelY + panelHeight + 5 - 2;
        int resetHeight = 14;
        
        if (mouseX >= resetX && mouseX < resetX + resetWidth &&
            mouseY >= resetY && mouseY < resetY + resetHeight) {
            if (staticOnResetAllocation != null) {
                staticOnResetAllocation.run();
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = (float) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 15));
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
