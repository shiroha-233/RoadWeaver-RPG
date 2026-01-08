package net.shiroha233.roadweaverpg.client.gui.interaction;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.client.gui.galgame.GalgameDialogConfig;
import net.shiroha233.roadweaverpg.client.gui.galgame.GalgameEntityRenderer;
import net.shiroha233.roadweaverpg.client.gui.galgame.GalgameGradientRenderer;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * NPC交互菜单界面
 * 职责：显示NPC的所有可用交互入口
 * 特性：Galgame风格背景 + 交互按钮列表
 */
public class NPCInteractionScreen extends Screen {
    
    private final int npcEntityId;
    private final List<NPCInteractionEntry> entries;
    private final Consumer<NPCInteractionEntry> onEntrySelected;
    
    // 实体引用
    private LivingEntity npcEntity;
    private Player playerEntity;
    
    // 动画状态
    private float animationProgress = 0f;
    
    // 按钮配置
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_SPACING = 4;
    private static final int BUTTON_RIGHT_MARGIN = 30;
    
    public NPCInteractionScreen(int npcEntityId, List<NPCInteractionEntry> entries, 
                                 Consumer<NPCInteractionEntry> onEntrySelected) {
        super(Component.translatable("gui.roadweaver_rpg.interaction.title"));
        this.npcEntityId = npcEntityId;
        this.entries = new ArrayList<>(entries);
        this.onEntrySelected = onEntrySelected;
    }
    
    @Override
    protected void init() {
        super.init();
        
        if (minecraft != null && minecraft.level != null && minecraft.player != null) {
            var entity = minecraft.level.getEntity(npcEntityId);
            if (entity instanceof LivingEntity living) {
                npcEntity = living;
                playerEntity = minecraft.player;
            }
        }
        
        createInteractionButtons();
    }
    
    /**
     * 创建交互按钮
     */
    private void createInteractionButtons() {
        int boxHeight = (int)(this.height * GalgameDialogConfig.DIALOG_BOX_HEIGHT_RATIO);
        int boxTop = this.height - boxHeight;
        
        int buttonX = this.width - BUTTON_RIGHT_MARGIN - BUTTON_WIDTH;
        int totalHeight = entries.size() * (BUTTON_HEIGHT + BUTTON_SPACING) - BUTTON_SPACING;
        int startY = boxTop + (boxHeight - totalHeight) / 2;
        
        for (int i = 0; i < entries.size(); i++) {
            NPCInteractionEntry entry = entries.get(i);
            int buttonY = startY + i * (BUTTON_HEIGHT + BUTTON_SPACING);
            
            InteractionEntryButton button = new InteractionEntryButton(
                    buttonX, buttonY,
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    entry,
                    btn -> handleEntrySelected(entry)
            );
            addRenderableWidget(button);
        }
    }
    
    private void handleEntrySelected(NPCInteractionEntry entry) {
        if (onEntrySelected != null) {
            onEntrySelected.accept(entry);
        }
        // 不立即关闭界面，等待服务端响应
        // 服务端会发送新的界面包（如对话界面、委托看板等）来覆盖当前界面
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (animationProgress < 1f) {
            animationProgress = Math.min(1f, animationProgress + GalgameDialogConfig.FADE_IN_SPEED);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染实体视口
        renderEntityViewports(graphics, partialTick);
        
        // 2. 渲染顶部渐变
        renderTopGradient(graphics);
        
        // 3. 渲染底部对话框背景
        renderDialogBox(graphics);
        
        // 4. 渲染中央分割线
        renderCharacterDivider(graphics);
        
        // 5. 重置深度
        RenderSystem.clear(256, Minecraft.ON_OSX);
        
        // 6. 渲染NPC名称和提示
        renderNPCInfo(graphics);
        
        // 7. 渲染按钮
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderEntityViewports(GuiGraphics graphics, float partialTick) {
        int viewportHeight = this.height;
        int viewportY = this.height;
        int halfWidth = this.width / 2;
        
        float baseScale = GalgameEntityRenderer.calculateEntityScale(npcEntity, viewportHeight) 
                * GalgameDialogConfig.ENTITY_SCALE_FACTOR;
        
        // NPC始终高亮
        float npcScale = baseScale * GalgameDialogConfig.SPEAKER_SCALE_BOOST;
        float playerScale = baseScale;
        
        if (npcEntity != null) {
            int npcX = halfWidth / 2;
            int npcY = viewportY - (int)(viewportHeight * GalgameDialogConfig.ENTITY_Y_OFFSET_RATIO);
            GalgameEntityRenderer.renderEntityFront(npcEntity, npcX, npcY, npcScale, 
                    GalgameDialogConfig.NPC_FACING_ANGLE, partialTick);
        }
        
        if (playerEntity != null) {
            int playerX = halfWidth + halfWidth / 2;
            int playerY = viewportY - (int)(viewportHeight * GalgameDialogConfig.ENTITY_Y_OFFSET_RATIO);
            GalgameEntityRenderer.renderEntityFront(playerEntity, playerX, playerY, playerScale, 
                    GalgameDialogConfig.PLAYER_FACING_ANGLE, partialTick);
        }
    }
    
    private void renderTopGradient(GuiGraphics graphics) {
        int topHeight = (int)(this.height * GalgameDialogConfig.TOP_GRADIENT_HEIGHT_RATIO);
        GalgameGradientRenderer.renderTopGradient(graphics, this.width, topHeight, animationProgress);
    }
    
    private void renderDialogBox(GuiGraphics graphics) {
        int boxHeight = (int)(this.height * GalgameDialogConfig.DIALOG_BOX_HEIGHT_RATIO);
        GalgameGradientRenderer.renderDialogBoxGradient(
                graphics, this.width, this.height, boxHeight, animationProgress);
    }
    
    private void renderCharacterDivider(GuiGraphics graphics) {
        int boxHeight = (int)(this.height * GalgameDialogConfig.DIALOG_BOX_HEIGHT_RATIO);
        int topHeight = (int)(this.height * GalgameDialogConfig.TOP_GRADIENT_HEIGHT_RATIO);
        int dividerX = this.width / 2;
        int topY = topHeight;
        int bottomY = this.height - boxHeight;
        
        GalgameGradientRenderer.renderCenterDivider(
                graphics, dividerX, topY, bottomY, animationProgress);
    }
    
    private void renderNPCInfo(GuiGraphics graphics) {
        int boxHeight = (int)(this.height * GalgameDialogConfig.DIALOG_BOX_HEIGHT_RATIO);
        int boxTop = this.height - boxHeight;
        
        // 渲染NPC名称
        int nameX = GalgameDialogConfig.DIALOG_PADDING;
        int nameY = boxTop + GalgameDialogConfig.DIALOG_PADDING;
        
        String npcName = npcEntity != null ? npcEntity.getDisplayName().getString() : "NPC";
        int nameWidth = font.width(npcName) + 16;
        int nameHeight = 18;
        
        GalgameGradientRenderer.renderNameTagBackground(
                graphics, nameX, nameY, nameWidth, nameHeight, animationProgress);
        
        int alpha = (int)(255 * animationProgress);
        int nameColor = (alpha << 24) | (GalgameDialogConfig.SPEAKER_NAME_COLOR & 0x00FFFFFF);
        graphics.drawString(font, npcName, nameX + 8, nameY + 5, nameColor, false);
        
        // 渲染提示文本
        int hintY = nameY + nameHeight + 10;
        Component hint = Component.translatable("gui.roadweaver_rpg.interaction.hint");
        int hintColor = (alpha << 24) | (GalgameDialogConfig.DIALOG_TEXT_COLOR & 0x00FFFFFF);
        graphics.drawString(font, hint, nameX, hintY, hintColor, false);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
