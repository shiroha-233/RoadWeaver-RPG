package net.shiroha233.roadweaverpg.client.gui.galgame;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话文本渲染器
 * 职责：处理Galgame风格的文本显示，包括打字机效果和自动换行
 */
public class DialogTextRenderer {
    
    private String fullText = "";
    private int displayedCharCount = 0;
    private float charAccumulator = 0f;
    private boolean isComplete = false;
    
    // 缓存的换行文本
    private List<String> wrappedLines = new ArrayList<>();
    
    /**
     * 设置要显示的文本
     */
    public void setText(String text, Font font, int maxWidth) {
        this.fullText = text;
        this.displayedCharCount = 0;
        this.charAccumulator = 0f;
        this.isComplete = false;
        this.wrappedLines = wrapText(text, font, maxWidth);
    }
    
    /**
     * 设置要显示的文本（Component版本）
     */
    public void setText(Component text, Font font, int maxWidth) {
        setText(text.getString(), font, maxWidth);
    }
    
    /**
     * 更新打字机效果
     * @return 是否有新字符显示
     */
    public boolean tick() {
        if (isComplete) return false;
        
        charAccumulator += GalgameDialogConfig.TYPEWRITER_SPEED;
        int newCharCount = (int) charAccumulator;
        
        if (newCharCount > displayedCharCount) {
            displayedCharCount = Math.min(newCharCount, fullText.length());
            
            if (displayedCharCount >= fullText.length()) {
                isComplete = true;
            }
            return true;
        }
        return false;
    }
    
    /**
     * 立即显示全部文本
     */
    public void skipToEnd() {
        displayedCharCount = fullText.length();
        charAccumulator = fullText.length();
        isComplete = true;
    }
    
    /**
     * 渲染文本
     */
    public void render(GuiGraphics graphics, Font font, int x, int y, int color) {
        if (wrappedLines.isEmpty()) return;
        
        int remainingChars = displayedCharCount;
        int currentY = y;
        
        for (String line : wrappedLines) {
            if (remainingChars <= 0) break;
            
            String displayLine;
            if (remainingChars >= line.length()) {
                displayLine = line;
                remainingChars -= line.length();
            } else {
                displayLine = line.substring(0, remainingChars);
                remainingChars = 0;
            }
            
            graphics.drawString(font, displayLine, x, currentY, color, false);
            currentY += GalgameDialogConfig.TEXT_LINE_HEIGHT;
        }
    }
    
    /**
     * 渲染带阴影的文本
     */
    public void renderWithShadow(GuiGraphics graphics, Font font, int x, int y, int color) {
        if (wrappedLines.isEmpty()) return;
        
        int remainingChars = displayedCharCount;
        int currentY = y;
        
        for (String line : wrappedLines) {
            if (remainingChars <= 0) break;
            
            String displayLine;
            if (remainingChars >= line.length()) {
                displayLine = line;
                remainingChars -= line.length();
            } else {
                displayLine = line.substring(0, remainingChars);
                remainingChars = 0;
            }
            
            // 绘制阴影
            graphics.drawString(font, displayLine, x + 1, currentY + 1, 0x000000, false);
            // 绘制文本
            graphics.drawString(font, displayLine, x, currentY, color, false);
            currentY += GalgameDialogConfig.TEXT_LINE_HEIGHT;
        }
    }
    
    /**
     * 文本自动换行
     */
    private List<String> wrapText(String text, Font font, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        
        StringBuilder currentLine = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            // 处理换行符
            if (c == '\n') {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                continue;
            }
            
            currentLine.append(c);
            
            // 检查是否超出宽度
            if (font.width(currentLine.toString()) > maxWidth) {
                // 回退一个字符
                currentLine.deleteCharAt(currentLine.length() - 1);
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentLine.append(c);
            }
        }
        
        // 添加最后一行
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * 获取渲染后的总高度
     */
    public int getRenderedHeight() {
        return wrappedLines.size() * GalgameDialogConfig.TEXT_LINE_HEIGHT;
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    public void reset() {
        fullText = "";
        displayedCharCount = 0;
        charAccumulator = 0f;
        isComplete = false;
        wrappedLines.clear();
    }
}
