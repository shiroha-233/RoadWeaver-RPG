package net.shiroha233.roadweaverpg.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.client.gui.render.GradientRenderer;
import net.shiroha233.roadweaverpg.network.packet.DialogResponsePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * NPC 对话界面 - 沉浸式设计
 * 职责：管理对话界面的布局和交互逻辑
 * 直接使用游戏世界作为背景，右侧覆盖渐变遮罩和对话选项
 */
public class NPCDialogScreen extends Screen {
    
    private final int entityId;
    private final BiConsumer<Integer, DialogResponsePacket.DialogOption> responseHandler;
    private final CameraController cameraController;
    private final List<DialogOption> dialogOptions;
    
    private LivingEntity targetEntity;
    private float animationProgress = 0f;
    
    /**
     * 构造函数 - 使用默认对话选项
     */
    public NPCDialogScreen(int entityId, BiConsumer<Integer, DialogResponsePacket.DialogOption> responseHandler) {
        this(entityId, responseHandler, createDefaultOptions());
    }
    
    /**
     * 构造函数 - 自定义对话选项（可扩展）
     */
    public NPCDialogScreen(int entityId, BiConsumer<Integer, DialogResponsePacket.DialogOption> responseHandler, 
                           List<DialogOption> options) {
        super(Component.translatable("gui.roadweaver_rpg.dialog.title"));
        this.entityId = entityId;
        this.responseHandler = responseHandler;
        this.cameraController = new CameraController();
        this.dialogOptions = options;
    }
    
    /**
     * 创建默认对话选项
     */
    private static List<DialogOption> createDefaultOptions() {
        List<DialogOption> options = new ArrayList<>();
        options.add(DialogOption.whoAreYou());
        options.add(DialogOption.showQuests());
        options.add(DialogOption.completeQuest());
        options.add(DialogOption.retrieveScroll()); // 添加找回委托书选项
        options.add(DialogOption.viewReputation()); // 添加查看声望选项
        options.add(DialogOption.cancel());
        return options;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 获取目标实体并设置视角
        if (minecraft != null && minecraft.level != null) {
            var entity = minecraft.level.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                targetEntity = living;
                cameraController.setupCamera(minecraft.player, targetEntity);
            }
        }
        
        // 动态创建按钮
        createDialogButtons();
    }
    
    /**
     * 根据对话选项动态创建按钮
     */
    private void createDialogButtons() {
        int buttonX = this.width - DialogConfig.RIGHT_PANEL_WIDTH + 30;
        int startY = this.height / 2 - (dialogOptions.size() * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING)) / 2;
        
        for (int i = 0; i < dialogOptions.size(); i++) {
            DialogOption option = dialogOptions.get(i);
            int buttonY = startY + i * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING);
            
            // 最后一个选项（通常是取消）增加间距
            if (i == dialogOptions.size() - 1) {
                buttonY += 10;
            }
            
            DialogOptionButton button = new DialogOptionButton(
                    buttonX, buttonY,
                    DialogConfig.BUTTON_WIDTH, DialogConfig.BUTTON_HEIGHT,
                    option.getText(),
                    btn -> handleOptionSelected(option)
            );
            
            if (option.isSecondary()) {
                button.setSecondary(true);
            }
            
            addRenderableWidget(button);
        }
    }
    
    /**
     * 处理选项选择
     */
    private void handleOptionSelected(DialogOption option) {
        // 如果是取消选项，直接关闭
        if (option.isSecondary() && option.getText().getString().contains("离开")) {
            onClose();
            return;
        }
        
        responseHandler.accept(entityId, option.getAction());
        onClose();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 更新动画
        if (animationProgress < 1f) {
            animationProgress = Math.min(1f, animationProgress + DialogConfig.ANIMATION_SPEED);
        }
        
        // 绘制渐变遮罩
        renderGradientOverlay(graphics);
        
        // 绘制标题
        renderTitle(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * 绘制渐变遮罩
     */
    private void renderGradientOverlay(GuiGraphics graphics) {
        int gradientStartX = (int)(this.width * DialogConfig.GRADIENT_START_RATIO);
        int gradientEndX = this.width;
        
        GradientRenderer.renderAnimatedHorizontalGradient(
                graphics, gradientStartX, gradientEndX, this.height,
                DialogConfig.GRADIENT_MAX_ALPHA, animationProgress
        );
    }
    
    /**
     * 绘制标题
     */
    private void renderTitle(GuiGraphics graphics) {
        // 计算按钮列表的起始 Y 坐标
        int startY = this.height / 2 - (dialogOptions.size() * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING)) / 2;
        
        // 将标题放在按钮列表上方 40 像素处
        int titleY = startY - 40;
        
        // 计算右侧面板的中心 X 坐标
        int panelCenterX = this.width - DialogConfig.RIGHT_PANEL_WIDTH / 2;
        
        int alpha = (int)(255 * animationProgress);
        int color = (alpha << 24) | DialogConfig.TITLE_COLOR;
        
        graphics.drawCenteredString(font, this.title, panelCenterX, titleY, color);
    }
    
    @Override
    public void onClose() {
        cameraController.restoreCamera();
        super.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
