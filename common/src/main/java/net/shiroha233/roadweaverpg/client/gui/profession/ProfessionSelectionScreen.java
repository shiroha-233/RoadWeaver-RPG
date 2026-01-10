package net.shiroha233.roadweaverpg.client.gui.profession;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.client.ClientProfessionCache;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.profession.ProfessionDefinition;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 职业选择界面
 * 用于新玩家注册冒险家时选择初始职业
 * 
 * 设计原理：
 * - 左侧显示职业列表
 * - 右侧显示选中职业的详细信息
 * - 底部确认按钮
 */
public class ProfessionSelectionScreen extends Screen {
    
    // 布局常量
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 400;
    private static final int LIST_WIDTH = 180;
    private static final int ITEM_HEIGHT = 40;
    private static final int MARGIN = 15;
    private static final int BUTTON_HEIGHT = 30;
    
    // 颜色常量
    private static final int COLOR_BG = 0xE0101020;
    private static final int COLOR_LIST_BG = 0x60000000;
    private static final int COLOR_ITEM_NORMAL = 0x40FFFFFF;
    private static final int COLOR_ITEM_HOVER = 0x60FFFFFF;
    private static final int COLOR_ITEM_SELECTED = 0x80FFD700;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DESC = 0xFFAAAAAA;
    private static final int COLOR_STAT_NAME = 0xFFBBBBBB;
    private static final int COLOR_STAT_VALUE = 0xFF44FF44;
    private static final int COLOR_BUTTON = 0x80FFD700;
    private static final int COLOR_BUTTON_HOVER = 0xC0FFD700;
    private static final int COLOR_BUTTON_DISABLED = 0x40888888;
    
    // 职业列表
    private final List<ProfessionDefinition> professions = new ArrayList<>();
    private ProfessionDefinition selectedProfession = null;
    private int scrollOffset = 0;
    
    // 回调
    private static Consumer<ResourceLocation> onSelectProfession;
    
    public ProfessionSelectionScreen() {
        super(Component.translatable("gui.roadweaver_rpg.profession_selection.title"));
    }
    
    public static void setOnSelectProfession(Consumer<ResourceLocation> callback) {
        onSelectProfession = callback;
    }
    
    @Override
    protected void init() {
        super.init();
        loadProfessions();
    }
    
    private void loadProfessions() {
        professions.clear();
        Collection<ProfessionDefinition> all = ClientProfessionCache.getAllProfessions();
        
        // 只显示默认职业（新玩家可选）
        for (ProfessionDefinition prof : all) {
            if (prof.isDefaultProfession()) {
                professions.add(prof);
            }
        }
        
        // 如果没有默认职业，显示所有职业
        if (professions.isEmpty()) {
            professions.addAll(all);
        }
        
        // 默认选中第一个
        if (!professions.isEmpty() && selectedProfession == null) {
            selectedProfession = professions.get(0);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        
        // 主面板背景
        GuiRenderer.drawRoundedRect(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 12, COLOR_BG);
        
        // 标题
        Component title = Component.translatable("gui.roadweaver_rpg.profession_selection.title");
        graphics.drawCenteredString(font, title, width / 2, panelY + 10, COLOR_TITLE);
        
        // 左侧职业列表
        renderProfessionList(graphics, panelX + MARGIN, panelY + 35, mouseX, mouseY);
        
        // 右侧详情面板
        renderProfessionDetails(graphics, panelX + LIST_WIDTH + MARGIN * 2, panelY + 35, 
                PANEL_WIDTH - LIST_WIDTH - MARGIN * 3, PANEL_HEIGHT - 85);
        
        // 底部按钮
        renderConfirmButton(graphics, panelX, panelY + PANEL_HEIGHT - 45, mouseX, mouseY);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    
    private void renderProfessionList(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int listHeight = PANEL_HEIGHT - 85;
        
        // 列表背景
        GuiRenderer.drawRoundedRect(graphics, x, y, LIST_WIDTH, listHeight, 8, COLOR_LIST_BG);
        
        // 启用裁剪
        graphics.enableScissor(x, y, x + LIST_WIDTH, y + listHeight);
        
        int currentY = y + 5 - scrollOffset;
        for (ProfessionDefinition prof : professions) {
            if (currentY + ITEM_HEIGHT > y && currentY < y + listHeight) {
                boolean selected = prof == selectedProfession;
                boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH &&
                                  mouseY >= currentY && mouseY < currentY + ITEM_HEIGHT;
                
                int bgColor = selected ? COLOR_ITEM_SELECTED : (hovered ? COLOR_ITEM_HOVER : COLOR_ITEM_NORMAL);
                GuiRenderer.drawRoundedRect(graphics, x + 3, currentY, LIST_WIDTH - 6, ITEM_HEIGHT - 4, 6, bgColor);
                
                // 职业名称
                graphics.drawString(font, prof.getName(), x + 10, currentY + 8, COLOR_TEXT, false);
                
                // 职业标签
                if (!prof.getTags().isEmpty()) {
                    String tag = prof.getTags().iterator().next();
                    graphics.drawString(font, "[" + tag + "]", x + 10, currentY + 22, COLOR_DESC, false);
                }
            }
            currentY += ITEM_HEIGHT;
        }
        
        graphics.disableScissor();
    }
    
    private void renderProfessionDetails(GuiGraphics graphics, int x, int y, int detailWidth, int detailHeight) {
        // 详情背景
        GuiRenderer.drawRoundedRect(graphics, x, y, detailWidth, detailHeight, 8, COLOR_LIST_BG);
        
        if (selectedProfession == null) {
            graphics.drawCenteredString(font, 
                    Component.translatable("gui.roadweaver_rpg.profession_selection.select_hint"),
                    x + detailWidth / 2, y + detailHeight / 2, COLOR_DESC);
            return;
        }
        
        int contentX = x + MARGIN;
        int contentY = y + MARGIN;
        int contentWidth = detailWidth - MARGIN * 2;
        
        // 职业名称
        graphics.drawString(font, selectedProfession.getName(), contentX, contentY, COLOR_TITLE, true);
        contentY += 20;
        
        // 职业描述
        Component desc = selectedProfession.getDescription();
        if (desc != null && !desc.getString().isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(desc, contentWidth);
            for (var line : lines) {
                graphics.drawString(font, line, contentX, contentY, COLOR_DESC, false);
                contentY += 12;
            }
        }
        contentY += 10;
        
        // 分隔线
        graphics.fill(contentX, contentY, contentX + contentWidth, contentY + 1, 0x40FFFFFF);
        contentY += 10;
        
        // 基础属性标题
        graphics.drawString(font, Component.translatable("gui.roadweaver_rpg.profession.base_stats"), 
                contentX, contentY, COLOR_TEXT, false);
        contentY += 15;
        
        // 显示基础属性
        for (StatType type : StatType.values()) {
            double baseValue = selectedProfession.getBaseStat(type);
            if (baseValue != getDefaultStatValue(type)) {
                String statName = Component.translatable(type.getTranslationKey()).getString();
                String valueStr = formatStatValue(type, baseValue);
                
                graphics.drawString(font, statName + ":", contentX + 10, contentY, COLOR_STAT_NAME, false);
                graphics.drawString(font, valueStr, contentX + contentWidth - font.width(valueStr) - 10, 
                        contentY, COLOR_STAT_VALUE, false);
                contentY += 14;
            }
        }
        
        contentY += 10;
        
        // 成长属性标题
        graphics.drawString(font, Component.translatable("gui.roadweaver_rpg.profession.growth_stats"), 
                contentX, contentY, COLOR_TEXT, false);
        contentY += 15;
        
        // 显示成长属性
        for (StatType type : StatType.values()) {
            double growth = selectedProfession.getGrowth(type);
            if (growth > 0) {
                String statName = Component.translatable(type.getTranslationKey()).getString();
                String valueStr = "+" + formatStatValue(type, growth) + "/级";
                
                graphics.drawString(font, statName + ":", contentX + 10, contentY, COLOR_STAT_NAME, false);
                graphics.drawString(font, valueStr, contentX + contentWidth - font.width(valueStr) - 10, 
                        contentY, COLOR_STAT_VALUE, false);
                contentY += 14;
            }
        }
    }
    
    private void renderConfirmButton(GuiGraphics graphics, int panelX, int y, int mouseX, int mouseY) {
        int buttonWidth = 120;
        int buttonX = panelX + (PANEL_WIDTH - buttonWidth) / 2;
        
        boolean canConfirm = selectedProfession != null;
        boolean hovered = mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                          mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        
        int bgColor = canConfirm ? (hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON) : COLOR_BUTTON_DISABLED;
        GuiRenderer.drawRoundedRect(graphics, buttonX, y, buttonWidth, BUTTON_HEIGHT, 6, bgColor);
        
        Component text = Component.translatable("gui.roadweaver_rpg.profession_selection.confirm");
        int textColor = canConfirm ? COLOR_TEXT : 0xFF888888;
        graphics.drawCenteredString(font, text, buttonX + buttonWidth / 2, y + (BUTTON_HEIGHT - 8) / 2, textColor);
    }
    
    private double getDefaultStatValue(StatType type) {
        return switch (type) {
            case MAX_HEALTH -> 20.0;
            case ATTACK -> 1.0;
            case MOVE_SPEED -> 100.0;
            case MAX_MANA -> 100.0;
            case MAGIC_ATTACK, MANA_REGEN -> 1.0;
            default -> 0.0;
        };
    }
    
    private String formatStatValue(StatType type, double value) {
        if (type == StatType.MOVE_SPEED || type == StatType.CRIT_RATE || type == StatType.CRIT_DAMAGE) {
            return String.format("%.1f%%", value);
        }
        if (value == Math.floor(value)) {
            return String.format("%.0f", value);
        }
        return String.format("%.1f", value);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        
        // 职业列表点击
        int listX = panelX + MARGIN;
        int listY = panelY + 35;
        int listHeight = PANEL_HEIGHT - 85;
        
        if (mouseX >= listX && mouseX < listX + LIST_WIDTH &&
            mouseY >= listY && mouseY < listY + listHeight) {
            
            int clickY = (int) mouseY - listY + scrollOffset - 5;
            int index = clickY / ITEM_HEIGHT;
            
            if (index >= 0 && index < professions.size()) {
                selectedProfession = professions.get(index);
                return true;
            }
        }
        
        // 确认按钮点击
        int buttonWidth = 120;
        int buttonX = panelX + (PANEL_WIDTH - buttonWidth) / 2;
        int buttonY = panelY + PANEL_HEIGHT - 45;
        
        if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
            mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
            
            if (selectedProfession != null && onSelectProfession != null) {
                onSelectProfession.accept(selectedProfession.getId());
                onClose();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, professions.size() * ITEM_HEIGHT - (PANEL_HEIGHT - 85) + 10);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 20));
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
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
