package net.shiroha233.roadweaverpg.client.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * GUI渲染工具类 - 提供精美的UI绘制方法
 */
public class GuiRenderer {
    
    /**
     * 绘制带圆角的矩形 (改进版：真正的圆角)
     */
    public static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, 
                                        int radius, int color) {
        // 限制半径大小
        int r = Math.min(radius, Math.min(width / 2, height / 2));
        if (r <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        // 1. 绘制中心主体（十字形）
        // 中心横条 (从左边距到右边距，高度为 height - 2*r)
        graphics.fill(x, y + r, x + width, y + height - r, color);
        // 中心竖条 (从上边距到下边距，宽度为 width - 2*r)
        graphics.fill(x + r, y, x + width - r, y + height, color);

        // 2. 绘制四个圆角
        drawCorner(graphics, x, y, r, 0, color);                 // 左上
        drawCorner(graphics, x + width - r, y, r, 1, color);     // 右上
        drawCorner(graphics, x, y + height - r, r, 2, color);    // 左下
        drawCorner(graphics, x + width - r, y + height - r, r, 3, color); // 右下
    }

    /**
     * 绘制单个圆角
     * type: 0=左上, 1=右上, 2=左下, 3=右下
     */
    private static void drawCorner(GuiGraphics graphics, int x, int y, int r, int type, int color) {
        // 使用简单的扫描线算法逼近圆角
        // 为了性能，我们只画像素条
        for (int i = 0; i < r; i++) {
            // 计算当前行/列的长度
            // 也就是圆的方程 x^2 + y^2 = r^2
            // 这里的 i 是距离圆心的垂直/水平距离
            int limit = (int) Math.sqrt(r * r - (r - i - 1) * (r - i - 1));
            // limit 是从圆心算起的长度，我们需要转换成绘制长度
            
            // 优化：直接画矩形条
            // 左上角 (0): 从上往下扫
            if (type == 0) {
                graphics.fill(x + r - limit, y + i, x + r, y + i + 1, color);
            }
            // 右上角 (1): 从上往下扫
            else if (type == 1) {
                graphics.fill(x, y + i, x + limit, y + i + 1, color);
            }
            // 左下角 (2): 从下往上扫
            else if (type == 2) {
                graphics.fill(x + r - limit, y + r - 1 - i, x + r, y + r - i, color);
            }
            // 右下角 (3): 从下往上扫
            else if (type == 3) {
                graphics.fill(x, y + r - 1 - i, x + limit, y + r - i, color);
            }
        }
    }
    
    /**
     * 绘制带边框的面板
     */
    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                  int bgColor, int borderColor, int borderWidth) {
        // 背景
        graphics.fill(x, y, x + width, y + height, bgColor);
        // 边框
        graphics.fill(x, y, x + width, y + borderWidth, borderColor); // 上
        graphics.fill(x, y + height - borderWidth, x + width, y + height, borderColor); // 下
        graphics.fill(x, y, x + borderWidth, y + height, borderColor); // 左
        graphics.fill(x + width - borderWidth, y, x + width, y + height, borderColor); // 右
    }
    
    /**
     * 绘制木质风格面板
     */
    public static void drawWoodenPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        // 外边框（深棕色）
        int darkBrown = 0xFF3D2817;
        int mediumBrown = 0xFF5C3D2E;
        int lightBrown = 0xFF8B5A2B;
        int innerBg = 0xFFD4C4A8;
        
        // 外层深色边框
        graphics.fill(x, y, x + width, y + height, darkBrown);
        // 中层边框
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, mediumBrown);
        // 内层边框
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, lightBrown);
        // 内部背景（羊皮纸色）
        graphics.fill(x + 6, y + 6, x + width - 6, y + height - 6, innerBg);
        
        // 添加木纹装饰线
        for (int i = 0; i < 3; i++) {
            int lineY = y + 8 + i * 4;
            graphics.fill(x + 6, lineY, x + width - 6, lineY + 1, 0x20000000);
        }
    }
    
    /**
     * 绘制委托书卡片
     */
    public static void drawQuestCard(GuiGraphics graphics, int x, int y, int width, int height,
                                      int rankColor, boolean hovered, boolean selected) {
        // 卡片背景
        int bgColor = selected ? 0xFFE8DCC8 : (hovered ? 0xFFF5EBD8 : 0xFFEDE4D3);
        graphics.fill(x, y, x + width, y + height, bgColor);
        
        // 左侧等级颜色条
        graphics.fill(x, y, x + 4, y + height, rankColor | 0xFF000000);
        
        // 边框
        int borderColor = hovered ? 0xFF8B4513 : 0xFFAA9988;
        graphics.renderOutline(x, y, width, height, borderColor);
        
        // 选中高亮
        if (selected) {
            graphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFFFFD700);
        }
    }
    
    /**
     * 绘制垂直渐变
     */
    public static void drawVerticalGradient(GuiGraphics graphics, int x, int y, int width, int height,
                                             int colorTop, int colorBottom) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        Matrix4f matrix = graphics.pose().last().pose();
        
        float aT = ((colorTop >> 24) & 0xFF) / 255f;
        float rT = ((colorTop >> 16) & 0xFF) / 255f;
        float gT = ((colorTop >> 8) & 0xFF) / 255f;
        float bT = (colorTop & 0xFF) / 255f;
        
        float aB = ((colorBottom >> 24) & 0xFF) / 255f;
        float rB = ((colorBottom >> 16) & 0xFF) / 255f;
        float gB = ((colorBottom >> 8) & 0xFF) / 255f;
        float bB = (colorBottom & 0xFF) / 255f;
        
        buffer.vertex(matrix, x, y, 0).color(rT, gT, bT, aT).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(rB, gB, bB, aB).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(rB, gB, bB, aB).endVertex();
        buffer.vertex(matrix, x + width, y, 0).color(rT, gT, bT, aT).endVertex();
        
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    
    /**
     * 绘制装饰性分隔线
     */
    public static void drawSeparator(GuiGraphics graphics, int x, int y, int width, int color) {
        int centerX = x + width / 2;
        // 中间粗线
        graphics.fill(centerX - 20, y, centerX + 20, y + 2, color);
        // 两侧细线
        graphics.fill(x, y + 1, centerX - 25, y + 1, color & 0x80FFFFFF);
        graphics.fill(centerX + 25, y + 1, x + width, y + 1, color & 0x80FFFFFF);
    }
    
    private GuiRenderer() {}
}
