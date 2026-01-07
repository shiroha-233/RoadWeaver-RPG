package net.shiroha233.roadweaverpg.client.gui.galgame;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Galgame风格渐变渲染器
 * 职责：提供Galgame对话界面专用的渐变效果
 */
public final class GalgameGradientRenderer {
    
    private GalgameGradientRenderer() {}
    
    /**
     * 渲染底部对话框渐变背景
     * 从上到下渐变，上方透明，下方半透明黑色
     */
    public static void renderDialogBoxGradient(GuiGraphics graphics, int width, int height, 
                                                int boxHeight, float animProgress) {
        int boxTop = height - boxHeight;
        int currentAlpha = (int)(GalgameDialogConfig.DIALOG_BOX_MAX_ALPHA * animProgress);
        int startAlpha = (int)(GalgameDialogConfig.DIALOG_BOX_START_ALPHA * animProgress);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        // 根据配置计算渐变区域
        int gradientHeight = (int)(boxHeight * GalgameDialogConfig.BOTTOM_GRADIENT_FADE_RATIO);
        
        // 上半部分渐变（透明到半透明）
        buffer.vertex(matrix, 0, boxTop, 0).color(0, 0, 0, 0).endVertex();
        buffer.vertex(matrix, 0, boxTop + gradientHeight, 0).color(0, 0, 0, startAlpha).endVertex();
        buffer.vertex(matrix, width, boxTop + gradientHeight, 0).color(0, 0, 0, startAlpha).endVertex();
        buffer.vertex(matrix, width, boxTop, 0).color(0, 0, 0, 0).endVertex();
        
        // 下半部分实色
        buffer.vertex(matrix, 0, boxTop + gradientHeight, 0).color(0, 0, 0, startAlpha).endVertex();
        buffer.vertex(matrix, 0, height, 0).color(0, 0, 0, currentAlpha).endVertex();
        buffer.vertex(matrix, width, height, 0).color(0, 0, 0, currentAlpha).endVertex();
        buffer.vertex(matrix, width, boxTop + gradientHeight, 0).color(0, 0, 0, startAlpha).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染顶部渐变背景（与底部对话框对称）
     * 从下到上渐变，下方透明，上方半透明黑色
     */
    public static void renderTopGradient(GuiGraphics graphics, int width, int topHeight, 
                                          float animProgress) {
        int currentAlpha = (int)(GalgameDialogConfig.TOP_GRADIENT_MAX_ALPHA * animProgress);
        int midAlpha = (int)(GalgameDialogConfig.DIALOG_BOX_START_ALPHA * 0.6f * animProgress);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        // 根据配置计算渐变区域
        int gradientHeight = (int)(topHeight * GalgameDialogConfig.TOP_GRADIENT_FADE_RATIO);
        int solidHeight = topHeight - gradientHeight;
        
        // 上半部分实色
        buffer.vertex(matrix, 0, 0, 0).color(0, 0, 0, currentAlpha).endVertex();
        buffer.vertex(matrix, 0, solidHeight, 0).color(0, 0, 0, midAlpha).endVertex();
        buffer.vertex(matrix, width, solidHeight, 0).color(0, 0, 0, midAlpha).endVertex();
        buffer.vertex(matrix, width, 0, 0).color(0, 0, 0, currentAlpha).endVertex();
        
        // 下半部分渐变（半透明到透明）
        buffer.vertex(matrix, 0, solidHeight, 0).color(0, 0, 0, midAlpha).endVertex();
        buffer.vertex(matrix, 0, topHeight, 0).color(0, 0, 0, 0).endVertex();
        buffer.vertex(matrix, width, topHeight, 0).color(0, 0, 0, 0).endVertex();
        buffer.vertex(matrix, width, solidHeight, 0).color(0, 0, 0, midAlpha).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染中央分割线（完整分隔左右区域）
     * 从顶部渐变区域底部延伸到底部对话框顶部
     */
    public static void renderCenterDivider(GuiGraphics graphics, int x, int topY, int bottomY, 
                                            float animProgress) {
        int alpha = (int)(GalgameDialogConfig.DIVIDER_ALPHA * animProgress);
        int lineWidth = GalgameDialogConfig.DIVIDER_WIDTH;
        int halfWidth = lineWidth / 2;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        int height = bottomY - topY;
        int fadeZone = height / 6; // 渐变区域
        
        // 顶部渐变区（透明到实色）
        buffer.vertex(matrix, x - halfWidth, topY, 0).color(255, 255, 255, 0).endVertex();
        buffer.vertex(matrix, x - halfWidth, topY + fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        buffer.vertex(matrix, x + halfWidth, topY + fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        buffer.vertex(matrix, x + halfWidth, topY, 0).color(255, 255, 255, 0).endVertex();
        
        // 中间实色区
        buffer.vertex(matrix, x - halfWidth, topY + fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        buffer.vertex(matrix, x - halfWidth, bottomY - fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        buffer.vertex(matrix, x + halfWidth, bottomY - fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        buffer.vertex(matrix, x + halfWidth, topY + fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        
        // 底部渐变区（实色到透明）
        buffer.vertex(matrix, x - halfWidth, bottomY - fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        buffer.vertex(matrix, x - halfWidth, bottomY, 0).color(255, 255, 255, 0).endVertex();
        buffer.vertex(matrix, x + halfWidth, bottomY, 0).color(255, 255, 255, 0).endVertex();
        buffer.vertex(matrix, x + halfWidth, bottomY - fadeZone, 0).color(255, 255, 255, alpha).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染角色展示区域的分割线（保留兼容性）
     */
    public static void renderCharacterDivider(GuiGraphics graphics, int x, int topY, int bottomY, 
                                               float animProgress) {
        renderCenterDivider(graphics, x, topY, bottomY, animProgress);
    }
    
    /**
     * 渲染角色高亮边框
     * 当角色说话时显示的边框效果
     */
    public static void renderSpeakerHighlight(GuiGraphics graphics, int x, int y, 
                                               int width, int height, float animProgress) {
        int alpha = (int)(100 * animProgress);
        int goldColor = GalgameDialogConfig.SPEAKER_NAME_COLOR;
        int r = (goldColor >> 16) & 0xFF;
        int g = (goldColor >> 8) & 0xFF;
        int b = goldColor & 0xFF;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        int borderWidth = 2;
        
        // 顶部边框
        buffer.vertex(matrix, x, y, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x, y + borderWidth, 0).color(r, g, b, alpha / 2).endVertex();
        buffer.vertex(matrix, x + width, y + borderWidth, 0).color(r, g, b, alpha / 2).endVertex();
        buffer.vertex(matrix, x + width, y, 0).color(r, g, b, alpha).endVertex();
        
        // 底部边框
        buffer.vertex(matrix, x, y + height - borderWidth, 0).color(r, g, b, alpha / 2).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(r, g, b, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height - borderWidth, 0).color(r, g, b, alpha / 2).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * 渲染名称标签背景
     */
    public static void renderNameTagBackground(GuiGraphics graphics, int x, int y, 
                                                int width, int height, float animProgress) {
        int alpha = (int)(180 * animProgress);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        // 主背景
        buffer.vertex(matrix, x, y, 0).color(0, 0, 0, alpha).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(0, 0, 0, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(0, 0, 0, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, 0).color(0, 0, 0, alpha).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        
        // 金色底边
        int goldColor = GalgameDialogConfig.SPEAKER_NAME_COLOR;
        int r = (goldColor >> 16) & 0xFF;
        int g = (goldColor >> 8) & 0xFF;
        int b = goldColor & 0xFF;
        int borderAlpha = (int)(255 * animProgress);
        
        buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        buffer.vertex(matrix, x, y + height - 2, 0).color(r, g, b, borderAlpha).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(r, g, b, borderAlpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(r, g, b, borderAlpha).endVertex();
        buffer.vertex(matrix, x + width, y + height - 2, 0).color(r, g, b, borderAlpha).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
}
