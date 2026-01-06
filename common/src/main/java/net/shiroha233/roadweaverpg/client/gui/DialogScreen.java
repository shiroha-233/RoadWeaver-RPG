package net.shiroha233.roadweaverpg.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.network.packet.DialogResponsePacket.DialogOption;

import java.util.function.BiConsumer;

/**
 * NPC 对话选项界面
 */
public class DialogScreen extends Screen {
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5;
    
    private final int entityId;
    private final BiConsumer<Integer, DialogOption> responseHandler;
    
    public DialogScreen(int entityId, BiConsumer<Integer, DialogOption> responseHandler) {
        super(Component.translatable("gui.roadweaver_rpg.dialog.title"));
        this.entityId = entityId;
        this.responseHandler = responseHandler;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;
        
        // 选项1：你是？
        addRenderableWidget(Button.builder(
                Component.translatable("gui.roadweaver_rpg.dialog.who_are_you"),
                btn -> selectOption(DialogOption.WHO_ARE_YOU)
        ).bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        // 选项2：有什么委托吗？
        addRenderableWidget(Button.builder(
                Component.translatable("gui.roadweaver_rpg.dialog.show_quests"),
                btn -> selectOption(DialogOption.SHOW_QUESTS)
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + BUTTON_SPACING, 
                BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        // 选项3：委托完成
        addRenderableWidget(Button.builder(
                Component.translatable("gui.roadweaver_rpg.dialog.complete_quest"),
                btn -> selectOption(DialogOption.COMPLETE_QUEST)
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, 
                BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        // 取消按钮
        addRenderableWidget(Button.builder(
                Component.translatable("gui.roadweaver_rpg.dialog.cancel"),
                btn -> onClose()
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3 + 10, 
                BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }
    
    private void selectOption(DialogOption option) {
        responseHandler.accept(entityId, option);
        onClose();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        // 绘制标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
