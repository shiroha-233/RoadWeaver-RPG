package net.shiroha233.roadweaverpg.client.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * 渐变渲染器 - 专门处理GUI渐变效果
 * 职责：提供可复用的渐变渲染方法
 */
public class GradientRenderer {
    
    /**
     * 绘制水平渐变（从左到右）
     * @param graphics GuiGraphics
     * @param startX 起始X坐标
     * @param endX 结束X坐标
     * @param height 高度
     * @param startAlpha 起始透明度（0-255）
     * @param endAlpha 结束透明度（0-255）
     */
    public static void renderHorizontalGradient(GuiGraphics graphics, int startX, int endX, 
                                                 int height, int startAlpha, int endAlpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        // 绘制四边形，左边透明，右边半透明
        buffer.vertex(matrix, startX, 0, 0).color(0, 0, 0, startAlpha).endVertex();
        buffer.vertex(matrix, startX, height, 0).color(0, 0, 0, startAlpha).endVertex();
        buffer.vertex(matrix, endX, height, 0).color(0, 0, 0, endAlpha).endVertex();
        buffer.vertex(matrix, endX, 0, 0).color(0, 0, 0, endAlpha).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * 绘制带动画的水平渐变
     */
    public static void renderAnimatedHorizontalGradient(GuiGraphics graphics, int startX, int endX, 
                                                         int height, int maxAlpha, float progress) {
        int currentAlpha = (int)(maxAlpha * progress);
        renderHorizontalGradient(graphics, startX, endX, height, 0, currentAlpha);
    }
    
    private GradientRenderer() {}
}
