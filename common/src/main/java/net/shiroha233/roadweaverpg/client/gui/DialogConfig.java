package net.shiroha233.roadweaverpg.client.gui;

/**
 * 对话界面配置 - 集中管理UI常量
 * 职责：提供可配置的UI参数
 */
public class DialogConfig {
    
    // 布局参数
    public static final int BUTTON_WIDTH = 180;
    public static final int BUTTON_HEIGHT = 28;
    public static final int BUTTON_SPACING = 8;
    public static final int RIGHT_PANEL_WIDTH = 260;
    
    // 渐变参数
    public static final float GRADIENT_START_RATIO = 0.2f;  // 渐变起始位置（屏幕宽度的20%）
    public static final int GRADIENT_MAX_ALPHA = 200;       // 最大透明度
    
    // 动画参数
    public static final float ANIMATION_SPEED = 0.08f;
    
    // 颜色
    public static final int TITLE_COLOR = 0xFFD700;  // 金色
    
    private DialogConfig() {}
}
