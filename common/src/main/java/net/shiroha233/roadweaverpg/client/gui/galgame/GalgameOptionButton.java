package net.shiroha233.roadweaverpg.client.gui.galgame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Galgame风格选项按钮
 * 职责：提供Galgame对话界面专用的选项按钮样式
 * 特性：悬停动画、渐变背景、金色边框
 */
public class GalgameOptionButton extends Button {
    
    private float hoverProgress = 0f;
    private static final float HOVER_SPEED = 0.15f;
    
    public GalgameOptionButton(int x, int y, int width, int height, 
                                Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateHoverAnimation();
        
        Minecraft mc = Minecraft.getInstance();
        int x = getX();
        int y = getY();
        
        // 悬停时的滑动效果
        int slideOffset = (int)(hoverProgress * 5);
        x += slideOffset;
        
        renderBackground(graphics, x, y);
        renderBorder(graphics, x, y);
        renderText(graphics, mc, x, y);
    }
    
    private void updateHoverAnimation() {
        if (isHovered) {
            hoverProgress = Math.min(1f, hoverProgress + HOVER_SPEED);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - HOVER_SPEED);
        }
    }
    
    private void renderBackground(GuiGraphics graphics, int x, int y) {
        // 背景透明度随悬停变化
        int bgAlpha = (int)Mth.lerp(hoverProgress, 100, 160);
        int bgColor = bgAlpha << 24;
        
        graphics.fill(x, y, x + width, y + height, bgColor);
    }
    
    private void renderBorder(GuiGraphics graphics, int x, int y) {
        int goldColor = GalgameDialogConfig.SPEAKER_NAME_COLOR;
        int r = (goldColor >> 16) & 0xFF;
        int g = (goldColor >> 8) & 0xFF;
        int b = goldColor & 0xFF;
        
        // 边框透明度随悬停变化
        int borderAlpha = (int)Mth.lerp(hoverProgress, 80, 200);
        int borderColor = (borderAlpha << 24) | (r << 16) | (g << 8) | b;
        
        // 左边框（金色强调）
        graphics.fill(x, y, x + 2, y + height, borderColor);
        
        // 其他边框（较淡）
        int lightBorderAlpha = borderAlpha / 2;
        int lightBorderColor = (lightBorderAlpha << 24) | 0xFFFFFF;
        
        graphics.fill(x, y, x + width, y + 1, lightBorderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, lightBorderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, lightBorderColor);
    }
    
    private void renderText(GuiGraphics graphics, Minecraft mc, int x, int y) {
        // 文本颜色随悬停变化
        int textColor;
        if (isHovered) {
            textColor = GalgameDialogConfig.SPEAKER_NAME_COLOR;
        } else {
            textColor = 0xFFFFFF;
        }
        
        int textX = x + 10;
        int textY = y + (height - 8) / 2;
        
        graphics.drawString(mc.font, getMessage(), textX, textY, textColor, false);
        
        // 悬停时显示箭头
        if (hoverProgress > 0.5f) {
            int arrowAlpha = (int)(255 * (hoverProgress - 0.5f) * 2);
            int arrowColor = (arrowAlpha << 24) | (GalgameDialogConfig.SPEAKER_NAME_COLOR & 0x00FFFFFF);
            int arrowX = x + width - 15;
            graphics.drawString(mc.font, ">", arrowX, textY, arrowColor, false);
        }
    }
    
    @Override
    public void playDownSound(SoundManager handler) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
        }
    }
}
