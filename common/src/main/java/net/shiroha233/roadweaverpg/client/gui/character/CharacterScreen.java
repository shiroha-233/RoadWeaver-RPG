package net.shiroha233.roadweaverpg.client.gui.character;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.client.ClientAdventureCache;
import net.shiroha233.roadweaverpg.client.ClientStatAllocationCache;
import net.shiroha233.roadweaverpg.client.ClientStatsCache;
import net.shiroha233.roadweaverpg.client.ClientWalletCache;
import net.shiroha233.roadweaverpg.client.StatDetailCalculator;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.currency.CurrencyUtils;
import net.shiroha233.roadweaverpg.stats.PlayerStats;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.List;
import java.util.function.Consumer;

/**
 * 角色界面 - 支持技能点加点的RPG属性面板
 * 布局：左侧分类标签 + 中间玩家模型 + 右侧属性数值（带加减按钮）
 */
public class CharacterScreen extends Screen {
    
    // 布局常量
    private static final int MARGIN = 20;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 28;
    private static final int TAB_SPACING = 4;
    private static final int STAT_LINE_HEIGHT = 24;
    private static final int PANEL_RADIUS = 12;
    private static final int BUTTON_SIZE = 16;
    private static final int VALUE_WIDTH = 70;
    
    // 颜色常量
    private static final int COLOR_BG = 0xA0000000;
    private static final int COLOR_PANEL_BG = 0x60101020;
    private static final int COLOR_TAB_NORMAL = 0x40FFFFFF;
    private static final int COLOR_TAB_HOVER = 0x60FFFFFF;
    private static final int COLOR_TAB_SELECTED = 0x80FFD700;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_STAT_NAME = 0xFFBBBBBB;
    private static final int COLOR_STAT_VALUE = 0xFFFFFFFF;
    private static final int COLOR_PLUS_NORMAL = 0x8044FF44;
    private static final int COLOR_PLUS_HOVER = 0xC044FF44;
    private static final int COLOR_MINUS_NORMAL = 0x80FF4444;
    private static final int COLOR_MINUS_HOVER = 0xC0FF4444;
    private static final int COLOR_BUTTON_DISABLED = 0x40888888;
    private static final int COLOR_SKILL_POINTS = 0xFF44FF44;
    
    // 滚动相关
    private float scrollOffset = 0;
    private float maxScroll = 0;
    
    // 悬停的属性类型（用于tooltip）
    private StatType hoveredStatType = null;
    
    // 当前选中的属性分类
    private int selectedCategory = 0;
    private final List<StatType.StatCategory> categories = List.of(
            StatType.StatCategory.BASIC,
            StatType.StatCategory.COMBAT,
            StatType.StatCategory.SPECIAL
    );
    
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
        
        GuiRenderer.drawRoundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 
                PANEL_RADIUS, COLOR_PANEL_BG);
        
        renderHeader(graphics, panelX, panelY - 30, panelWidth);
        renderCategoryTabs(graphics, panelX + MARGIN, panelY + MARGIN, mouseX, mouseY);
        
        int modelX = panelX + TAB_WIDTH + MARGIN * 2 + 80;
        int modelY = panelY + panelHeight / 2 + 20;
        renderPlayerModel(graphics, modelX, modelY, mouseX, mouseY);
        
        int statsX = panelX + TAB_WIDTH + MARGIN * 2 + 160;
        int statsY = panelY + MARGIN;
        int statsWidth = panelWidth - TAB_WIDTH - MARGIN * 3 - 160;
        int statsHeight = panelHeight - MARGIN * 2;
        renderStatsPanel(graphics, statsX, statsY, statsWidth, statsHeight, mouseX, mouseY);
        
        renderSkillPointsInfo(graphics, panelX, panelY + panelHeight + 5, panelWidth, mouseX, mouseY);
        
        // 渲染tooltip（在最后渲染以确保在最上层）
        if (hoveredStatType != null) {
            List<Component> tooltip = StatDetailCalculator.getStatDetails(hoveredStatType);
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
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
        for (int i = 0; i < categories.size(); i++) {
            int tabY = y + i * (TAB_HEIGHT + TAB_SPACING);
            boolean selected = i == selectedCategory;
            boolean hovered = mouseX >= x && mouseX < x + TAB_WIDTH && 
                             mouseY >= tabY && mouseY < tabY + TAB_HEIGHT;
            
            int bgColor = selected ? COLOR_TAB_SELECTED : (hovered ? COLOR_TAB_HOVER : COLOR_TAB_NORMAL);
            GuiRenderer.drawRoundedRect(graphics, x, tabY, TAB_WIDTH, TAB_HEIGHT, 6, bgColor);
            
            String name = getCategoryName(categories.get(i));
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
        renderCategoryStats(graphics, x + 10, y + 10 - (int)scrollOffset, areaWidth - 20, mouseX, mouseY);
        graphics.disableScissor();
        
        maxScroll = Math.max(0, calculateContentHeight(categories.get(selectedCategory)) - areaHeight + 20);
    }

    
    private void renderCategoryStats(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
        PlayerStats stats = ClientStatsCache.getStats();
        int currentY = y;
        int availablePoints = ClientStatAllocationCache.getAvailablePoints();
        
        // 重置悬停状态
        hoveredStatType = null;
        
        for (StatType type : StatType.values()) {
            if (type.getCategory() != categories.get(selectedCategory)) continue;
            
            double value = getStatValue(stats, type);
            String valueStr = formatStatValue(type, value);
            
            // 属性名称
            String name = Component.translatable(type.getTranslationKey()).getString();
            int nameWidth = font.width(name);
            
            // 检测属性名称区域悬停（用于tooltip）
            boolean nameHovered = mouseX >= x && mouseX < x + nameWidth + 60 &&
                                  mouseY >= currentY && mouseY < currentY + STAT_LINE_HEIGHT;
            if (nameHovered) {
                hoveredStatType = type;
            }
            
            graphics.drawString(font, name, x, currentY + 4, nameHovered ? 0xFFFFFFFF : COLOR_STAT_NAME, false);
            
            // 计算右侧布局位置
            int rightEdge = x + width;
            int valueX = rightEdge - VALUE_WIDTH;
            
            if (type.isAllocatable()) {
                int allocatedPoints = ClientStatAllocationCache.getAllocatedPoints(type);
                
                // 显示已分配点数
                if (allocatedPoints > 0) {
                    String pointsStr = "(+" + allocatedPoints + ")";
                    graphics.drawString(font, pointsStr, x + font.width(name) + 5, currentY + 4, COLOR_SKILL_POINTS, false);
                }
                
                // 减号按钮
                int minusX = valueX - BUTTON_SIZE - 4;
                int buttonY = currentY + 2;
                boolean canDeallocate = allocatedPoints > 0;
                boolean minusHovered = mouseX >= minusX && mouseX < minusX + BUTTON_SIZE &&
                                       mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
                
                int minusColor = canDeallocate ? (minusHovered ? COLOR_MINUS_HOVER : COLOR_MINUS_NORMAL) : COLOR_BUTTON_DISABLED;
                GuiRenderer.drawRoundedRect(graphics, minusX, buttonY, BUTTON_SIZE, BUTTON_SIZE, 3, minusColor);
                
                int minusTextColor = canDeallocate ? 0xFFFFFFFF : 0xFF888888;
                int cx = minusX + BUTTON_SIZE / 2;
                int cy = buttonY + BUTTON_SIZE / 2;
                graphics.fill(cx - 4, cy - 1, cx + 4, cy + 1, minusTextColor);
                
                // 加号按钮
                int plusX = minusX - BUTTON_SIZE - 2;
                boolean canAllocate = availablePoints > 0;
                boolean plusHovered = mouseX >= plusX && mouseX < plusX + BUTTON_SIZE &&
                                      mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
                
                int plusColor = canAllocate ? (plusHovered ? COLOR_PLUS_HOVER : COLOR_PLUS_NORMAL) : COLOR_BUTTON_DISABLED;
                GuiRenderer.drawRoundedRect(graphics, plusX, buttonY, BUTTON_SIZE, BUTTON_SIZE, 3, plusColor);
                
                int plusTextColor = canAllocate ? 0xFFFFFFFF : 0xFF888888;
                cx = plusX + BUTTON_SIZE / 2;
                graphics.fill(cx - 4, cy - 1, cx + 4, cy + 1, plusTextColor);
                graphics.fill(cx - 1, cy - 4, cx + 1, cy + 4, plusTextColor);
            }
            
            // 数值（右对齐）
            int valueColor = value > 0 ? COLOR_STAT_VALUE : 0xFF888888;
            graphics.drawString(font, valueStr, rightEdge - font.width(valueStr), currentY + 4, valueColor, false);
            
            // 分隔线
            graphics.fill(x, currentY + 18, x + width, currentY + 19, 0x20FFFFFF);
            
            currentY += STAT_LINE_HEIGHT;
        }
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
    
    private double getStatValue(PlayerStats stats, StatType type) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player != null) {
            return switch (type) {
                case MAX_HEALTH -> player.getMaxHealth();
                case ATTACK -> calculateTotalAttack(player);
                case DEFENSE -> player.getArmorValue();
                case MAGIC_DEFENSE -> player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                case ATTACK_SPEED -> player.getAttributeValue(Attributes.ATTACK_SPEED) * 100;
                case MOVE_SPEED -> player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1000;
                case MAX_MANA -> stats.getMaxMana();
                case MAGIC_ATTACK -> calculateMagicAttack();
                case CRIT_RATE -> stats.getCritRate();
                case CRIT_DAMAGE -> stats.getCritDamage();
                case HIT_RATE -> stats.getHitRate();
                case DODGE_RATE -> stats.getDodgeRate();
                case HEALTH_REGEN -> stats.getHealthRegen();
                case MANA_REGEN -> stats.getManaRegen();
                case LIFE_STEAL -> stats.getLifeSteal();
                case MANA_STEAL -> stats.getManaSteal();
                case COOLDOWN_REDUCTION -> stats.getCooldownReduction();
                case EXP_BONUS -> stats.getExpBonus();
                case DROP_BONUS -> stats.getDropBonus();
            };
        }
        
        return 0;
    }
    
    /**
     * 计算总攻击力 = 基础(1) + 武器 + 技能点
     */
    private double calculateTotalAttack(Player player) {
        double base = 1.0;
        double weapon = 0.0;
        double skillBonus = ClientStatAllocationCache.getAllocatedPoints(StatType.ATTACK) * 1.0;
        
        // 从主手物品获取攻击力加成
        var mainHandItem = player.getMainHandItem();
        if (!mainHandItem.isEmpty()) {
            var modifiers = mainHandItem.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            var attackModifiers = modifiers.get(Attributes.ATTACK_DAMAGE);
            for (var modifier : attackModifiers) {
                if (modifier.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) {
                    weapon += modifier.getAmount();
                }
            }
        }
        
        return base + weapon + skillBonus;
    }
    
    /**
     * 计算魔法攻击力 = 基础(1) + 技能点
     */
    private double calculateMagicAttack() {
        double base = 1.0;
        double skillBonus = ClientStatAllocationCache.getAllocatedPoints(StatType.MAGIC_ATTACK) * 1.0;
        return base + skillBonus;
    }
    
    private String formatStatValue(StatType type, double value) {
        if (isPercentageStat(type)) {
            return String.format("%.1f%%", value);
        }
        if (type == StatType.MAX_HEALTH || type == StatType.MAX_MANA || 
            type == StatType.DEFENSE || type == StatType.MAGIC_DEFENSE) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }
    
    private boolean isPercentageStat(StatType type) {
        return type == StatType.CRIT_RATE || type == StatType.CRIT_DAMAGE ||
               type == StatType.HIT_RATE || type == StatType.DODGE_RATE ||
               type == StatType.ATTACK_SPEED || type == StatType.MOVE_SPEED ||
               type == StatType.LIFE_STEAL || type == StatType.MANA_STEAL ||
               type == StatType.COOLDOWN_REDUCTION || type == StatType.EXP_BONUS ||
               type == StatType.DROP_BONUS;
    }
    
    private String getCategoryName(StatType.StatCategory category) {
        return switch (category) {
            case BASIC -> "基础";
            case COMBAT -> "战斗";
            case SPECIAL -> "特殊";
        };
    }
    
    private int calculateContentHeight(StatType.StatCategory category) {
        int count = 0;
        for (StatType type : StatType.values()) {
            if (type.getCategory() == category) count++;
        }
        return count * STAT_LINE_HEIGHT + 20;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min(680, width - 40);
        int panelHeight = Math.min(380, height - 40);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        
        // 分类标签点击
        int tabX = panelX + MARGIN;
        int tabY = panelY + MARGIN;
        for (int i = 0; i < categories.size(); i++) {
            int currentTabY = tabY + i * (TAB_HEIGHT + TAB_SPACING);
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH && 
                mouseY >= currentTabY && mouseY < currentTabY + TAB_HEIGHT) {
                selectedCategory = i;
                scrollOffset = 0;
                return true;
            }
        }
        
        if (handleStatButtonClick(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            return true;
        }
        
        if (handleResetClick(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean handleStatButtonClick(double mouseX, double mouseY, 
                                           int panelX, int panelY, int panelWidth, int panelHeight) {
        int statsX = panelX + TAB_WIDTH + MARGIN * 2 + 160;
        int statsY = panelY + MARGIN;
        int statsWidth = panelWidth - TAB_WIDTH - MARGIN * 3 - 160;
        
        int currentY = statsY + 10 - (int)scrollOffset;
        int rightEdge = statsX + 10 + statsWidth - 20;
        int valueX = rightEdge - VALUE_WIDTH;
        
        int availablePoints = ClientStatAllocationCache.getAvailablePoints();
        
        for (StatType type : StatType.values()) {
            if (type.getCategory() != categories.get(selectedCategory)) continue;
            
            if (type.isAllocatable()) {
                int allocatedPoints = ClientStatAllocationCache.getAllocatedPoints(type);
                int buttonY = currentY + 2;
                int minusX = valueX - BUTTON_SIZE - 4;
                int plusX = minusX - BUTTON_SIZE - 2;
                
                // 加号点击
                if (mouseX >= plusX && mouseX < plusX + BUTTON_SIZE &&
                    mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                    if (availablePoints > 0 && staticOnAllocatePoint != null) {
                        staticOnAllocatePoint.accept(type);
                    }
                    return true;
                }
                
                // 减号点击
                if (mouseX >= minusX && mouseX < minusX + BUTTON_SIZE &&
                    mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                    if (allocatedPoints > 0 && staticOnDeallocatePoint != null) {
                        staticOnDeallocatePoint.accept(type);
                    }
                    return true;
                }
            }
            
            currentY += STAT_LINE_HEIGHT;
        }
        
        return false;
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
