package net.shiroha233.roadweaverpg.client.gui.character;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.client.ClientStatAllocationCache;
import net.shiroha233.roadweaverpg.client.ClientStatsCache;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.function.Consumer;

/**
 * 属性标签页渲染器
 * 
 * 设计原则：
 * - 只从ClientStatsCache读取属性值，不做任何计算
 * - 职业/技能点等系统负责修改属性，这里只负责显示
 */
public class StatsTabRenderer {
    
    private static final int STAT_LINE_HEIGHT = 24;
    private static final int BUTTON_SIZE = 16;
    private static final int VALUE_WIDTH = 70;
    
    private static final int COLOR_STAT_NAME = 0xFFBBBBBB;
    private static final int COLOR_STAT_VALUE = 0xFFFFFFFF;
    private static final int COLOR_STAT_DISABLED = 0xFF666666;
    private static final int COLOR_PLUS_NORMAL = 0x8044FF44;
    private static final int COLOR_PLUS_HOVER = 0xC044FF44;
    private static final int COLOR_MINUS_NORMAL = 0x80FF4444;
    private static final int COLOR_MINUS_HOVER = 0xC0FF4444;
    private static final int COLOR_BUTTON_DISABLED = 0x40888888;
    private static final int COLOR_SKILL_POINTS = 0xFF44FF44;
    
    private final Font font;
    private StatType hoveredStatType = null;
    
    public StatsTabRenderer(Font font) {
        this.font = font;
    }
    
    public StatType getHoveredStatType() {
        return hoveredStatType;
    }
    
    /**
     * 渲染属性列表
     */
    public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY,
                       StatType.StatCategory category, float scrollOffset) {
        int currentY = y;
        int availablePoints = ClientStatAllocationCache.getAvailablePoints();
        boolean hasMagicMod = MagicModCompatRegistry.hasAnyMagicMod();
        
        hoveredStatType = null;
        
        for (StatType type : StatType.values()) {
            if (type.getCategory() != category) continue;
            
            boolean isMagicDisabled = type.requiresMagicMod() && !hasMagicMod;
            
            // 直接从缓存读取值，不做任何计算
            double value = ClientStatsCache.getValue(type);
            String valueStr = formatStatValue(type, value);
            
            String name = Component.translatable(type.getTranslationKey()).getString();
            int nameWidth = font.width(name);
            
            boolean nameHovered = mouseX >= x && mouseX < x + nameWidth + 60 &&
                                  mouseY >= currentY && mouseY < currentY + STAT_LINE_HEIGHT;
            if (nameHovered) {
                hoveredStatType = type;
            }
            
            int nameColor = isMagicDisabled ? COLOR_STAT_DISABLED : (nameHovered ? 0xFFFFFFFF : COLOR_STAT_NAME);
            graphics.drawString(font, name, x, currentY + 4, nameColor, false);
            
            int rightEdge = x + width;
            int valueX = rightEdge - VALUE_WIDTH;
            
            // 可加点属性显示按钮
            if (type.isAllocatable() && !isMagicDisabled) {
                int allocatedPoints = ClientStatAllocationCache.getAllocatedPoints(type);
                
                if (allocatedPoints > 0) {
                    String pointsStr = "(+" + allocatedPoints + ")";
                    graphics.drawString(font, pointsStr, x + font.width(name) + 5, currentY + 4, COLOR_SKILL_POINTS, false);
                }
                
                renderButtons(graphics, valueX, currentY, mouseX, mouseY, allocatedPoints, availablePoints);
            }
            
            int valueColor = isMagicDisabled ? COLOR_STAT_DISABLED : (value > 0 ? COLOR_STAT_VALUE : 0xFF888888);
            graphics.drawString(font, valueStr, rightEdge - font.width(valueStr), currentY + 4, valueColor, false);
            
            graphics.fill(x, currentY + 18, x + width, currentY + 19, 0x20FFFFFF);
            currentY += STAT_LINE_HEIGHT;
        }
    }
    
    private void renderButtons(GuiGraphics graphics, int valueX, int currentY, int mouseX, int mouseY,
                                int allocatedPoints, int availablePoints) {
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
    
    /**
     * 处理按钮点击
     */
    public boolean handleClick(double mouseX, double mouseY, int statsX, int statsY, int statsWidth,
                                StatType.StatCategory category, float scrollOffset,
                                Consumer<StatType> onAllocate, Consumer<StatType> onDeallocate) {
        int currentY = statsY + 10 - (int)scrollOffset;
        int rightEdge = statsX + 10 + statsWidth - 20;
        int valueX = rightEdge - VALUE_WIDTH;
        
        int availablePoints = ClientStatAllocationCache.getAvailablePoints();
        boolean hasMagicMod = MagicModCompatRegistry.hasAnyMagicMod();
        
        for (StatType type : StatType.values()) {
            if (type.getCategory() != category) continue;
            
            boolean isMagicDisabled = type.requiresMagicMod() && !hasMagicMod;
            
            if (type.isAllocatable() && !isMagicDisabled) {
                int allocatedPoints = ClientStatAllocationCache.getAllocatedPoints(type);
                int buttonY = currentY + 2;
                int minusX = valueX - BUTTON_SIZE - 4;
                int plusX = minusX - BUTTON_SIZE - 2;
                
                if (mouseX >= plusX && mouseX < plusX + BUTTON_SIZE &&
                    mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                    if (availablePoints > 0 && onAllocate != null) {
                        onAllocate.accept(type);
                    }
                    return true;
                }
                
                if (mouseX >= minusX && mouseX < minusX + BUTTON_SIZE &&
                    mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                    if (allocatedPoints > 0 && onDeallocate != null) {
                        onDeallocate.accept(type);
                    }
                    return true;
                }
            }
            
            currentY += STAT_LINE_HEIGHT;
        }
        
        return false;
    }
    
    /**
     * 计算内容高度
     */
    public int calculateContentHeight(StatType.StatCategory category) {
        int count = 0;
        for (StatType type : StatType.values()) {
            if (type.getCategory() == category) count++;
        }
        return count * STAT_LINE_HEIGHT + 20;
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
               type == StatType.ATTACK_COOLDOWN || type == StatType.MOVE_SPEED ||
               type == StatType.LIFE_STEAL || type == StatType.MANA_STEAL ||
               type == StatType.COOLDOWN_REDUCTION || type == StatType.EXP_BONUS ||
               type == StatType.DROP_BONUS;
    }
}
