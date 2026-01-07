package net.shiroha233.roadweaverpg.client.gui.galgame;

/**
 * Galgame对话界面配置
 * 职责：集中管理Galgame风格对话UI的所有常量
 */
public final class GalgameDialogConfig {
    
    // ==================== 对话框布局 ====================
    /** 对话框高度占屏幕比例 */
    public static final float DIALOG_BOX_HEIGHT_RATIO = 0.4f;
    /** 顶部渐变高度占屏幕比例（仅用于渐变覆盖，不影响实体渲染） */
    public static final float TOP_GRADIENT_HEIGHT_RATIO = 0.4f;
    /** 顶部渐变内部渐变区域比例（0-1，控制渐变从哪里开始，0=顶部，1=底部） */
    public static final float TOP_GRADIENT_FADE_RATIO = 0.5f;
    /** 底部渐变内部渐变区域比例（0-1，控制渐变从哪里开始，0=顶部，1=底部） */
    public static final float BOTTOM_GRADIENT_FADE_RATIO = 0.33f;
    /** 对话框内边距 */
    public static final int DIALOG_PADDING = 20;
    /** 对话框圆角半径 */
    public static final int DIALOG_CORNER_RADIUS = 8;
    
    // ==================== 角色展示区域 ====================
    /** 角色展示区域分割比例（左侧NPC占比） */
    public static final float CHARACTER_SPLIT_RATIO = 0.5f;
    /** 角色视口垂直偏移（从顶部开始） */
    public static final float CHARACTER_VIEWPORT_TOP_RATIO = 0.05f;
    /** 角色视口高度比例 */
    public static final float CHARACTER_VIEWPORT_HEIGHT_RATIO = 0.67f;
    
    // ==================== 实体渲染参数 ====================
    /** 实体渲染缩放系数 */
    public static final float ENTITY_SCALE_FACTOR = 0.9f;
    /** 实体垂直偏移（相对于视口底部） */
    public static final float ENTITY_Y_OFFSET_RATIO = -0.1f;
    /** NPC面向角度（稍微朝右，面向中间） */
    public static final float NPC_FACING_ANGLE = 155f;
    /** 玩家面向角度（稍微朝左，面向中间） */
    public static final float PLAYER_FACING_ANGLE = -155f;
    /** 说话者高亮时的额外缩放 */
    public static final float SPEAKER_SCALE_BOOST = 1.05f;
    
    // ==================== 渐变效果 ====================
    /** 对话框渐变最大透明度 */
    public static final int DIALOG_BOX_MAX_ALPHA = 220;
    /** 对话框渐变起始透明度 */
    public static final int DIALOG_BOX_START_ALPHA = 180;
    /** 顶部渐变最大透明度 */
    public static final int TOP_GRADIENT_MAX_ALPHA = 200;
    /** 分割线透明度 */
    public static final int DIVIDER_ALPHA = 80;
    /** 分割线宽度 */
    public static final int DIVIDER_WIDTH = 3;
    
    // ==================== 动画参数 ====================
    /** 界面淡入速度 */
    public static final float FADE_IN_SPEED = 0.06f;
    /** 打字机效果速度（每tick显示的字符数） */
    public static final float TYPEWRITER_SPEED = 1.5f;
    /** 角色切换动画时长（ticks） */
    public static final int CHARACTER_SWITCH_DURATION = 10;
    
    // ==================== 文本样式 ====================
    /** 说话者名称颜色 */
    public static final int SPEAKER_NAME_COLOR = 0xFFD700;
    /** 对话文本颜色 */
    public static final int DIALOG_TEXT_COLOR = 0xFFFFFF;
    /** 对话文本行高 */
    public static final int TEXT_LINE_HEIGHT = 12;
    /** 名称与文本间距 */
    public static final int NAME_TEXT_SPACING = 8;
    
    // ==================== 选项按钮 ====================
    /** 选项按钮宽度 */
    public static final int OPTION_BUTTON_WIDTH = 200;
    /** 选项按钮高度 */
    public static final int OPTION_BUTTON_HEIGHT = 24;
    /** 选项按钮间距 */
    public static final int OPTION_BUTTON_SPACING = 6;
    /** 选项区域右边距 */
    public static final int OPTION_RIGHT_MARGIN = 30;
    
    // ==================== 相机参数 ====================
    /** NPC视角水平偏移角度 */
    public static final float NPC_VIEW_YAW_OFFSET = 30f;
    /** 玩家视角水平偏移角度 */
    public static final float PLAYER_VIEW_YAW_OFFSET = -30f;
    /** 视角俯仰角偏移 */
    public static final float VIEW_PITCH_OFFSET = -5f;
    /** 相机距离系数 */
    public static final float CAMERA_DISTANCE_FACTOR = 2.5f;
    
    // ==================== 提示文本 ====================
    /** 继续提示闪烁速度 */
    public static final float CONTINUE_HINT_BLINK_SPEED = 0.05f;
    /** 继续提示文本 */
    public static final String CONTINUE_HINT = "▼";
    
    private GalgameDialogConfig() {}
}
