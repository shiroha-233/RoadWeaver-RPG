package net.shiroha233.roadweaverpg.client.gui.dialog;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.client.gui.render.GradientRenderer;
import net.shiroha233.roadweaverpg.network.packet.ui.DialogResponsePacket;

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
    
    public NPCDialogScreen(int entityId, BiConsumer<Integer, DialogResponsePacket.DialogOption> responseHandler) {
        this(entityId, responseHandler, createDefaultOptions());
    }
    
    public NPCDialogScreen(int entityId, BiConsumer<Integer, DialogResponsePacket.DialogOption> responseHandler, 
                           List<DialogOption> options) {
        super(Component.translatable("gui.roadweaver_rpg.dialog.title"));
        this.entityId = entityId;
        this.responseHandler = responseHandler;
        this.cameraController = new CameraController();
        this.dialogOptions = options;
    }
    
    private static List<DialogOption> createDefaultOptions() {
        List<DialogOption> options = new ArrayList<>();
        options.add(DialogOption.showQuests());
        options.add(DialogOption.completeQuest());
        options.add(DialogOption.retrieveScroll());
        options.add(DialogOption.viewReputation());
        options.add(DialogOption.cancel());
        return options;
    }
    
    @Override
    protected void init() {
        super.init();
        
        if (minecraft != null && minecraft.level != null) {
            var entity = minecraft.level.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                targetEntity = living;
                cameraController.setupCamera(minecraft.player, targetEntity);
            }
        }
        
        createDialogButtons();
    }
    
    private void createDialogButtons() {
        int buttonX = this.width - DialogConfig.RIGHT_PANEL_WIDTH + 30;
        int startY = this.height / 2 - (dialogOptions.size() * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING)) / 2;
        
        for (int i = 0; i < dialogOptions.size(); i++) {
            DialogOption option = dialogOptions.get(i);
            int buttonY = startY + i * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING);
            
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
    
    private void handleOptionSelected(DialogOption option) {
        if (option.isSecondary() && option.getText().getString().contains("离开")) {
            onClose();
            return;
        }
        
        responseHandler.accept(entityId, option.getAction());
        onClose();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (animationProgress < 1f) {
            animationProgress = Math.min(1f, animationProgress + DialogConfig.ANIMATION_SPEED);
        }
        
        renderGradientOverlay(graphics);
        renderTitle(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderGradientOverlay(GuiGraphics graphics) {
        int gradientStartX = (int)(this.width * DialogConfig.GRADIENT_START_RATIO);
        int gradientEndX = this.width;
        
        GradientRenderer.renderAnimatedHorizontalGradient(
                graphics, gradientStartX, gradientEndX, this.height,
                DialogConfig.GRADIENT_MAX_ALPHA, animationProgress
        );
    }
    
    private void renderTitle(GuiGraphics graphics) {
        int startY = this.height / 2 - (dialogOptions.size() * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING)) / 2;
        int titleY = startY - 40;
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
