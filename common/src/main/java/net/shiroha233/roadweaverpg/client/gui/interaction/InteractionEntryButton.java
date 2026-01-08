package net.shiroha233.roadweaverpg.client.gui.interaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionEntry;

/**
 * 交互入口按钮
 * 职责：显示单个交互入口
 * 特性：图标 + 文字，悬停效果
 */
public class InteractionEntryButton extends Button {
    
    private final NPCInteractionEntry entry;
    
    // 颜色配置
    private static final int BG_COLOR = 0x80000000;
    private static final int BG_HOVER_COLOR = 0xA0333333;
    private static final int BORDER_COLOR = 0x80FFFFFF;
    private static final int BORDER_HOVER_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ICON_COLOR = 0xFFFFCC00;
    
    public InteractionEntryButton(int x, int y, int width, int height, 
                                   NPCInteractionEntry entry, OnPress onPress) {
        super(x, y, width, height, entry.displayName(), onPress, DEFAULT_NARRATION);
        this.entry = entry;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered();
        
        // 背景
        int bgColor = hovered ? BG_HOVER_COLOR : BG_COLOR;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        
        // 边框
        int borderColor = hovered ? BORDER_HOVER_COLOR : BORDER_COLOR;
        // 上边框
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        // 下边框
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        // 左边框
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        // 右边框
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
        
        // 图标
        String icon = getIconChar(entry.iconType());
        int iconX = getX() + 8;
        int iconY = getY() + (height - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, icon, iconX, iconY, ICON_COLOR, false);
        
        // 文字
        int textX = iconX + 16;
        int textY = getY() + (height - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, getMessage(), textX, textY, TEXT_COLOR, false);
        
        // 悬停时显示描述
        if (hovered && !entry.description().getString().isEmpty()) {
            // 可以在这里添加tooltip
        }
    }
    
    /**
     * 获取图标字符
     */
    private String getIconChar(String iconType) {
        return switch (iconType) {
            case NPCInteractionEntry.ICON_CHAT -> "💬";
            case NPCInteractionEntry.ICON_QUEST -> "📜";
            case NPCInteractionEntry.ICON_SHOP -> "🛒";
            case NPCInteractionEntry.ICON_INFO -> "ℹ";
            case NPCInteractionEntry.ICON_REPUTATION -> "⭐";
            default -> "•";
        };
    }
    
    @Override
    public void playDownSound(SoundManager handler) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
        }
    }
}
