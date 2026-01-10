package net.shiroha233.roadweaverpg.client.gui.character;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.client.ClientProfessionCache;
import net.shiroha233.roadweaverpg.profession.ProfessionDefinition;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.ArrayList;
import java.util.List;

/**
 * 职业标签页渲染器
 * 负责在角色界面中渲染职业详情
 */
public class ProfessionTabRenderer {
    
    private static final int STAT_LINE_HEIGHT = 24;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_STAT_NAME = 0xFFBBBBBB;
    private static final int COLOR_STAT_VALUE = 0xFFFFFFFF;
    
    private final Font font;
    
    public ProfessionTabRenderer(Font font) {
        this.font = font;
    }
    
    /**
     * 渲染职业标签页内容
     */
    public void render(GuiGraphics graphics, int x, int y, int width) {
        ProfessionDefinition profDef = ClientProfessionCache.getPlayerProfessionDef();
        int currentY = y;
        
        if (profDef == null) {
            renderNoProfession(graphics, x, currentY);
            return;
        }
        
        currentY = renderProfessionHeader(graphics, x, currentY, width, profDef);
        currentY = renderBaseStats(graphics, x, currentY, width, profDef);
        currentY = renderGrowthStats(graphics, x, currentY, width, profDef);
        renderUnlockedStats(graphics, x, currentY, width, profDef);
    }
    
    /**
     * 渲染未选择职业提示
     */
    private void renderNoProfession(GuiGraphics graphics, int x, int y) {
        String noProf = Component.translatable("gui.roadweaver_rpg.profession.no_profession").getString();
        graphics.drawString(font, noProf, x, y, 0xFFAAAAAA, false);
        
        String hint = Component.translatable("gui.roadweaver_rpg.profession.select_hint").getString();
        graphics.drawString(font, hint, x, y + STAT_LINE_HEIGHT, 0xFF888888, false);
    }
    
    /**
     * 渲染职业名称和描述
     */
    private int renderProfessionHeader(GuiGraphics graphics, int x, int y, int width, ProfessionDefinition profDef) {
        int currentY = y;
        
        // 职业名称
        String profName = profDef.getName().getString();
        graphics.drawString(font, profName, x, currentY, COLOR_TITLE, true);
        currentY += STAT_LINE_HEIGHT;
        
        // 职业描述（自动换行）
        String desc = profDef.getDescription().getString();
        List<String> descLines = wrapText(desc, width);
        for (String line : descLines) {
            graphics.drawString(font, line, x, currentY, 0xFFCCCCCC, false);
            currentY += 12;
        }
        currentY += 8;
        
        // 分隔线
        graphics.fill(x, currentY, x + width, currentY + 1, 0x40FFFFFF);
        currentY += 8;
        
        return currentY;
    }
    
    /**
     * 渲染基础属性
     */
    private int renderBaseStats(GuiGraphics graphics, int x, int y, int width, ProfessionDefinition profDef) {
        int currentY = y;
        
        String baseTitle = Component.translatable("gui.roadweaver_rpg.profession.base_stats").getString();
        graphics.drawString(font, baseTitle, x, currentY, COLOR_TITLE, false);
        currentY += STAT_LINE_HEIGHT - 4;
        
        for (var entry : profDef.getBaseStats().entrySet()) {
            StatType type = entry.getKey();
            double value = entry.getValue();
            String statName = Component.translatable(type.getTranslationKey()).getString();
            String valueStr = formatStatValue(type, value);
            
            graphics.drawString(font, "  " + statName, x, currentY, COLOR_STAT_NAME, false);
            graphics.drawString(font, valueStr, x + width - font.width(valueStr), currentY, COLOR_STAT_VALUE, false);
            currentY += 16;
        }
        currentY += 4;
        
        return currentY;
    }
    
    /**
     * 渲染成长属性
     */
    private int renderGrowthStats(GuiGraphics graphics, int x, int y, int width, ProfessionDefinition profDef) {
        if (profDef.getStatGrowth().isEmpty()) return y;
        
        int currentY = y;
        
        // 分隔线
        graphics.fill(x, currentY, x + width, currentY + 1, 0x40FFFFFF);
        currentY += 8;
        
        String growthTitle = Component.translatable("gui.roadweaver_rpg.profession.growth_stats").getString();
        graphics.drawString(font, growthTitle, x, currentY, COLOR_TITLE, false);
        currentY += STAT_LINE_HEIGHT - 4;
        
        for (var entry : profDef.getStatGrowth().entrySet()) {
            StatType type = entry.getKey();
            double value = entry.getValue();
            String statName = Component.translatable(type.getTranslationKey()).getString();
            String valueStr = "+" + formatStatValue(type, value) + "/级";
            
            graphics.drawString(font, "  " + statName, x, currentY, COLOR_STAT_NAME, false);
            graphics.drawString(font, valueStr, x + width - font.width(valueStr), currentY, 0xFF88FF88, false);
            currentY += 16;
        }
        currentY += 4;
        
        return currentY;
    }
    
    /**
     * 渲染可加点属性
     */
    private void renderUnlockedStats(GuiGraphics graphics, int x, int y, int width, ProfessionDefinition profDef) {
        if (profDef.getUnlockedStats().isEmpty()) return;
        
        int currentY = y;
        
        // 分隔线
        graphics.fill(x, currentY, x + width, currentY + 1, 0x40FFFFFF);
        currentY += 8;
        
        String unlockedTitle = Component.translatable("gui.roadweaver_rpg.profession.unlocked_stats").getString();
        graphics.drawString(font, unlockedTitle, x, currentY, COLOR_TITLE, false);
        currentY += STAT_LINE_HEIGHT - 4;
        
        // 拼接可加点属性名称
        StringBuilder sb = new StringBuilder();
        for (StatType type : profDef.getUnlockedStats()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(Component.translatable(type.getTranslationKey()).getString());
        }
        
        List<String> statLines = wrapText(sb.toString(), width - 10);
        for (String line : statLines) {
            graphics.drawString(font, "  " + line, x, currentY, 0xFF88CCFF, false);
            currentY += 12;
        }
    }
    
    /**
     * 计算职业标签页内容高度
     */
    public int calculateContentHeight() {
        ProfessionDefinition profDef = ClientProfessionCache.getPlayerProfessionDef();
        if (profDef == null) return 60;
        
        int height = STAT_LINE_HEIGHT; // 职业名称
        height += 40; // 描述（估算）
        height += 8 + STAT_LINE_HEIGHT; // 基础属性标题
        height += profDef.getBaseStats().size() * 16 + 4;
        
        if (!profDef.getStatGrowth().isEmpty()) {
            height += 8 + STAT_LINE_HEIGHT;
            height += profDef.getStatGrowth().size() * 16 + 4;
        }
        
        if (!profDef.getUnlockedStats().isEmpty()) {
            height += 8 + STAT_LINE_HEIGHT + 24;
        }
        
        return height;
    }
    
    /**
     * 文本自动换行
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            currentLine.append(c);
            if (font.width(currentLine.toString()) > maxWidth) {
                currentLine.deleteCharAt(currentLine.length() - 1);
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentLine.append(c);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * 格式化属性值
     */
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
