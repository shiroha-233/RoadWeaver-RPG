package net.shiroha233.roadweaverpg.client.gui.shop;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.client.gui.dialog.CameraController;
import net.shiroha233.roadweaverpg.client.gui.dialog.DialogConfig;
import net.shiroha233.roadweaverpg.client.gui.dialog.DialogOptionButton;
import net.shiroha233.roadweaverpg.client.gui.render.GradientRenderer;
import net.shiroha233.roadweaverpg.network.packet.shop.ShopDialogResponsePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 商店NPC对话界面 - 与公会女仆对话界面风格一致
 */
public class ShopDialogScreen extends Screen {
    
    private final int entityId;
    private final BiConsumer<Integer, ShopDialogResponsePacket.ShopDialogOption> responseHandler;
    private final CameraController cameraController;
    private final List<ShopDialogOption> dialogOptions;
    
    private LivingEntity targetEntity;
    private float animationProgress = 0f;
    
    public ShopDialogScreen(int entityId, 
                            BiConsumer<Integer, ShopDialogResponsePacket.ShopDialogOption> responseHandler) {
        super(Component.translatable("gui.roadweaver_rpg.shop_dialog.title"));
        this.entityId = entityId;
        this.responseHandler = responseHandler;
        this.cameraController = new CameraController();
        this.dialogOptions = createDefaultOptions();
    }
    
    private static List<ShopDialogOption> createDefaultOptions() {
        List<ShopDialogOption> options = new ArrayList<>();
        options.add(new ShopDialogOption(
                Component.translatable("gui.roadweaver_rpg.shop_dialog.open_shop"),
                ShopDialogResponsePacket.ShopDialogOption.OPEN_SHOP,
                false
        ));
        options.add(new ShopDialogOption(
                Component.translatable("gui.roadweaver_rpg.dialog.cancel"),
                ShopDialogResponsePacket.ShopDialogOption.CANCEL,
                true
        ));
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
            ShopDialogOption option = dialogOptions.get(i);
            int buttonY = startY + i * (DialogConfig.BUTTON_HEIGHT + DialogConfig.BUTTON_SPACING);
            
            if (i == dialogOptions.size() - 1) {
                buttonY += 10;
            }
            
            DialogOptionButton button = new DialogOptionButton(
                    buttonX, buttonY,
                    DialogConfig.BUTTON_WIDTH, DialogConfig.BUTTON_HEIGHT,
                    option.text(),
                    btn -> handleOptionSelected(option)
            );
            
            if (option.isSecondary()) {
                button.setSecondary(true);
            }
            
            addRenderableWidget(button);
        }
    }
    
    private void handleOptionSelected(ShopDialogOption option) {
        if (option.isSecondary()) {
            onClose();
            return;
        }
        
        responseHandler.accept(entityId, option.action());
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
    
    /**
     * 商店对话选项数据类
     */
    private record ShopDialogOption(
            Component text,
            ShopDialogResponsePacket.ShopDialogOption action,
            boolean isSecondary
    ) {}
}
