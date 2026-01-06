package net.shiroha233.roadweaverpg.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * 对话选项按钮 - 美观的自定义样式
 * 特性：悬停动画、渐变背景、圆角效果
 */
public class DialogOptionButton extends Button {
    
    // 颜色常量
    private static final int PRIMARY_COLOR = 0xFFD700;      // 金色
    private static final int SECONDARY_COLOR = 0x888888;    // 灰色
    
    // 动画状态
    private float hoverProgress = 0f;
    private static final float HOVER_SPEED = 0.2f;
    
    // 按钮类型
    private boolean isSecondary = false;
    
    // 左侧装饰条宽度
    private static final int ACCENT_WIDTH = 3;
    
    public DialogOptionButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    /**
     * 设置为次要按钮样式（如取消按钮）
     */
    public DialogOptionButton setSecondary(boolean secondary) {
        this.isSecondary = secondary;
        return this;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 更新悬停动画
        updateHoverAnimation();
        
        Minecraft mc = Minecraft.getInstance();
        int x = getX();
        int y = getY();
        
        // 计算动画偏移（悬停时向右滑动）
        int slideOffset = (int)(hoverProgress * 8);
        x += slideOffset;
        
        // 绘制背景
        renderBackground(graphics, x, y);
        
        // 绘制左侧装饰条
        renderAccentBar(graphics, x, y);
        
        // 绘制边框
        renderBorder(graphics, x, y);
        
        // 绘制文字
        renderText(graphics, mc, x, y);
        
        // 绘制悬停箭头指示
        if (hoverProgress > 0.5f) {
            renderArrow(graphics, mc, x, y);
        }
    }
    
    private void updateHoverAnimation() {
        if (isHovered) {
            hoverProgress = Math.min(1f, hoverProgress + HOVER_SPEED);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - HOVER_SPEED);
        }
    }
    
    private void renderBackground(GuiGraphics graphics, int x, int y) {
        // 背景颜色随悬停变化
        int bgAlpha = (int)Mth.lerp(hoverProgress, 48, 80);
        int bgColor = isHovered ? (bgAlpha << 24 | 0xFFFFFF) : (bgAlpha << 24);
        
        graphics.fill(x, y, x + width, y + height, bgColor);
    }
    
    private void renderAccentBar(GuiGraphics graphics, int x, int y) {
        int accentColor = isSecondary ? SECONDARY_COLOR : PRIMARY_COLOR;
        
        // 悬停时装饰条变亮
        int alpha = (int)Mth.lerp(hoverProgress, 180, 255);
        int finalColor = (alpha << 24) | (accentColor & 0x00FFFFFF);
        
        graphics.fill(x, y, x + ACCENT_WIDTH, y + height, finalColor);
    }
    
    private void renderBorder(GuiGraphics graphics, int x, int y) {
        int borderAlpha = (int)Mth.lerp(hoverProgress, 40, 100);
        int borderColor = (borderAlpha << 24) | 0xFFFFFF;
        
        // 上边框
        graphics.fill(x, y, x + width, y + 1, borderColor);
        // 下边框
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        // 右边框
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
    
    private void renderText(GuiGraphics graphics, Minecraft mc, int x, int y) {
        int textColor = isSecondary ? SECONDARY_COLOR : 0xFFFFFF;
        
        // 悬停时文字变亮
        if (isHovered && !isSecondary) {
            textColor = PRIMARY_COLOR;
        }
        
        // 文字位置（左对齐，留出装饰条空间）
        int textX = x + ACCENT_WIDTH + 10;
        int textY = y + (height - 8) / 2;
        
        graphics.drawString(mc.font, getMessage(), textX, textY, textColor, false);
    }
    
    private void renderArrow(GuiGraphics graphics, Minecraft mc, int x, int y) {
        int arrowColor = isSecondary ? SECONDARY_COLOR : PRIMARY_COLOR;
        int arrowX = x + width - 15;
        int arrowY = y + (height - 8) / 2;
        
        // 简单的箭头符号 ">"
        graphics.drawString(mc.font, ">", arrowX, arrowY, arrowColor, false);
    }
    
    @Override
    public void playDownSound(SoundManager handler) {
        // 播放自定义点击音效
        Minecraft.getInstance().player.playSound(
                SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f
        );
    }
}
