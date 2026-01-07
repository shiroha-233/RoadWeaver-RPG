package net.shiroha233.roadweaverpg.client.gui.dialog;

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
    
    private static final int PRIMARY_COLOR = 0xFFD700;
    private static final int SECONDARY_COLOR = 0x888888;
    
    private float hoverProgress = 0f;
    private static final float HOVER_SPEED = 0.2f;
    
    private boolean isSecondary = false;
    
    private static final int ACCENT_WIDTH = 3;
    
    public DialogOptionButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    public DialogOptionButton setSecondary(boolean secondary) {
        this.isSecondary = secondary;
        return this;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateHoverAnimation();
        
        Minecraft mc = Minecraft.getInstance();
        int x = getX();
        int y = getY();
        
        int slideOffset = (int)(hoverProgress * 8);
        x += slideOffset;
        
        renderBackground(graphics, x, y);
        renderAccentBar(graphics, x, y);
        renderBorder(graphics, x, y);
        renderText(graphics, mc, x, y);
        
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
        int bgAlpha = (int)Mth.lerp(hoverProgress, 48, 80);
        int bgColor = isHovered ? (bgAlpha << 24 | 0xFFFFFF) : (bgAlpha << 24);
        
        graphics.fill(x, y, x + width, y + height, bgColor);
    }
    
    private void renderAccentBar(GuiGraphics graphics, int x, int y) {
        int accentColor = isSecondary ? SECONDARY_COLOR : PRIMARY_COLOR;
        
        int alpha = (int)Mth.lerp(hoverProgress, 180, 255);
        int finalColor = (alpha << 24) | (accentColor & 0x00FFFFFF);
        
        graphics.fill(x, y, x + ACCENT_WIDTH, y + height, finalColor);
    }
    
    private void renderBorder(GuiGraphics graphics, int x, int y) {
        int borderAlpha = (int)Mth.lerp(hoverProgress, 40, 100);
        int borderColor = (borderAlpha << 24) | 0xFFFFFF;
        
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
    
    private void renderText(GuiGraphics graphics, Minecraft mc, int x, int y) {
        int textColor = isSecondary ? SECONDARY_COLOR : 0xFFFFFF;
        
        if (isHovered && !isSecondary) {
            textColor = PRIMARY_COLOR;
        }
        
        int textX = x + ACCENT_WIDTH + 10;
        int textY = y + (height - 8) / 2;
        
        graphics.drawString(mc.font, getMessage(), textX, textY, textColor, false);
    }
    
    private void renderArrow(GuiGraphics graphics, Minecraft mc, int x, int y) {
        int arrowColor = isSecondary ? SECONDARY_COLOR : PRIMARY_COLOR;
        int arrowX = x + width - 15;
        int arrowY = y + (height - 8) / 2;
        
        graphics.drawString(mc.font, ">", arrowX, arrowY, arrowColor, false);
    }
    
    @Override
    public void playDownSound(SoundManager handler) {
        Minecraft.getInstance().player.playSound(
                SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f
        );
    }
}
