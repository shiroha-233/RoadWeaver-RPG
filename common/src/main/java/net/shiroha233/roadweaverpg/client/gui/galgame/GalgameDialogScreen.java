package net.shiroha233.roadweaverpg.client.gui.galgame;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Galgame风格对话界面
 * 职责：管理Galgame风格的NPC对话显示
 * 特性：底部对话框、左右角色展示（实体渲染）、打字机效果、选项按钮
 * 布局：屏幕分为三部分 - 左侧NPC视口、右侧玩家视口、底部对话框
 */
public class GalgameDialogScreen extends Screen {
    
    private final int npcEntityId;
    private final DialogTextRenderer textRenderer;
    
    // 对话数据
    private final List<DialogEntry> dialogEntries = new ArrayList<>();
    private int currentEntryIndex = 0;
    
    // 选项回调
    private Consumer<Integer> optionCallback;
    private List<DialogOptionData> currentOptions = new ArrayList<>();
    private boolean showingOptions = false;
    
    // 动画状态
    private float animationProgress = 0f;
    private float continueHintAlpha = 0f;
    private boolean continueHintIncreasing = true;
    
    // 实体引用
    private LivingEntity npcEntity;
    private Player playerEntity;
    
    // 当前说话者（用于高亮效果）
    private boolean isNpcSpeaking = true;
    
    public GalgameDialogScreen(int npcEntityId) {
        super(Component.empty());
        this.npcEntityId = npcEntityId;
        this.textRenderer = new DialogTextRenderer();
    }
    
    @Override
    protected void init() {
        super.init();
        
        if (minecraft != null && minecraft.level != null && minecraft.player != null) {
            var entity = minecraft.level.getEntity(npcEntityId);
            if (entity instanceof LivingEntity living) {
                npcEntity = living;
                playerEntity = minecraft.player;
                // 不再需要控制玩家相机，因为我们使用独立的实体渲染
            }
        }
        
        // 初始化第一条对话
        if (!dialogEntries.isEmpty()) {
            showEntry(0);
        }
    }
    
    /**
     * 添加对话条目
     */
    public GalgameDialogScreen addDialog(Component speaker, Component text, boolean isNpc) {
        dialogEntries.add(new DialogEntry(speaker, text, isNpc));
        return this;
    }
    
    /**
     * 设置选项和回调
     */
    public GalgameDialogScreen setOptions(List<DialogOptionData> options, Consumer<Integer> callback) {
        this.currentOptions = options;
        this.optionCallback = callback;
        return this;
    }
    
    /**
     * 显示指定索引的对话
     */
    private void showEntry(int index) {
        if (index < 0 || index >= dialogEntries.size()) return;
        
        currentEntryIndex = index;
        DialogEntry entry = dialogEntries.get(index);
        
        // 计算文本区域宽度
        int textWidth = this.width - GalgameDialogConfig.DIALOG_PADDING * 4;
        textRenderer.setText(entry.text(), font, textWidth);
        
        // 更新当前说话者状态
        isNpcSpeaking = entry.isNpc();
    }
    
    /**
     * 前进到下一条对话或显示选项
     */
    private void advanceDialog() {
        // 如果文本未完成，先完成文本
        if (!textRenderer.isComplete()) {
            textRenderer.skipToEnd();
            return;
        }
        
        // 前进到下一条
        if (currentEntryIndex < dialogEntries.size() - 1) {
            showEntry(currentEntryIndex + 1);
        } else {
            // 对话结束，显示选项或关闭
            if (!currentOptions.isEmpty()) {
                showingOptions = true;
                createOptionButtons();
            } else {
                onClose();
            }
        }
    }
    
    /**
     * 创建选项按钮
     */
    private void createOptionButtons() {
        clearWidgets();
        
        int boxHeight = (int)(this.height * GalgameDialogConfig.DIALOG_BOX_HEIGHT_RATIO);
        int boxTop = this.height - boxHeight;
        
        int buttonX = this.width - GalgameDialogConfig.OPTION_RIGHT_MARGIN 
                - GalgameDialogConfig.OPTION_BUTTON_WIDTH;
        int startY = boxTop + GalgameDialogConfig.DIALOG_PADDING;
        
        for (int i = 0; i < currentOptions.size(); i++) {
            DialogOptionData option = currentOptions.get(i);
            int buttonY = startY + i * (GalgameDialogConfig.OPTION_BUTTON_HEIGHT 
                    + GalgameDialogConfig.OPTION_BUTTON_SPACING);
            
            final int optionIndex = i;
            GalgameOptionButton button = new GalgameOptionButton(
                    buttonX, buttonY,
                    GalgameDialogConfig.OPTION_BUTTON_WIDTH,
                    GalgameDialogConfig.OPTION_BUTTON_HEIGHT,
                    option.text(),
                    btn -> handleOptionSelected(optionIndex)
            );
            addRenderableWidget(button);
        }
    }
    
    private void handleOptionSelected(int index) {
        if (optionCallback != null) {
            optionCallback.accept(index);
        }
        onClose();
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 更新动画进度
        if (animationProgress < 1f) {
            animationProgress = Math.min(1f, animationProgress + GalgameDialogConfig.FADE_IN_SPEED);
        }
        
        // 更新打字机效果
        textRenderer.tick();
        
        // 更新继续提示闪烁
        updateContinueHint();
    }
    
    private void updateContinueHint() {
        if (textRenderer.isComplete() && !showingOptions) {
            if (continueHintIncreasing) {
                continueHintAlpha += GalgameDialogConfig.CONTINUE_HINT_BLINK_SPEED;
                if (continueHintAlpha >= 1f) {
                    continueHintAlpha = 1f;
                    continueHintIncreasing = false;
                }
            } else {
                continueHintAlpha -= GalgameDialogConfig.CONTINUE_HINT_BLINK_SPEED;
                if (continueHintAlpha <= 0.3f) {
                    continueHintAlpha = 0.3f;
                    continueHintIncreasing = true;
                }
            }
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. 渲染左右两侧的实体视口（最底层）
        renderEntityViewports(graphics, partialTick);
        
        // 2. 渲染顶部渐变（覆盖在实体上）
        renderTopGradient(graphics);
        
        // 3. 渲染底部对话框背景（覆盖在实体上）
        renderDialogBox(graphics);
        
        // 4. 渲染中央分割线
        renderCharacterDivider(graphics);
        
        // 5. 重置深度，确保后续UI在最上层
        RenderSystem.clear(256, Minecraft.ON_OSX); // 清除深度缓冲
        
        // 6. 渲染对话内容（最上层）
        if (!dialogEntries.isEmpty() && currentEntryIndex < dialogEntries.size()) {
            renderDialogContent(graphics);
        }
        
        // 7. 渲染继续提示
        if (textRenderer.isComplete() && !showingOptions) {
            renderContinueHint(graphics);
        }
        
        // 8. 渲染选项按钮（最上层）
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * 渲染左右两侧的实体视口
     * 左侧：NPC正面视图
     * 右侧：玩家正面视图
     */
    private void renderEntityViewports(GuiGraphics graphics, float partialTick) {
        // 实体视口占据整个屏幕（从顶部到底部）
        int viewportHeight = this.height;
        int viewportY = this.height; // 实体底部位置
        
        int halfWidth = this.width / 2;
        
        // 计算实体缩放
        float baseScale = GalgameEntityRenderer.calculateEntityScale(npcEntity, viewportHeight) 
                * GalgameDialogConfig.ENTITY_SCALE_FACTOR;
        
        // 说话者高亮缩放
        float npcScale = isNpcSpeaking ? baseScale * GalgameDialogConfig.SPEAKER_SCALE_BOOST : baseScale;
        float playerScale = !isNpcSpeaking ? baseScale * GalgameDialogConfig.SPEAKER_SCALE_BOOST : baseScale;
        
        // 渲染NPC（左侧，面向右）
        if (npcEntity != null) {
            int npcX = halfWidth / 2;
            int npcY = viewportY - (int)(viewportHeight * GalgameDialogConfig.ENTITY_Y_OFFSET_RATIO);
            GalgameEntityRenderer.renderEntityFront(npcEntity, npcX, npcY, npcScale, 
                    GalgameDialogConfig.NPC_FACING_ANGLE, partialTick);
        }
        
        // 渲染玩家（右侧，面向左）
        if (playerEntity != null) {
            int playerX = halfWidth + halfWidth / 2;
            int playerY = viewportY - (int)(viewportHeight * GalgameDialogConfig.ENTITY_Y_OFFSET_RATIO);
            GalgameEntityRenderer.renderEntityFront(playerEntity, playerX, playerY, playerScale, 
                    GalgameDialogConfig.PLAYER_FACING_ANGLE, partialTick);
        }
    }
    
    /**
     * 渲染顶部渐变
     */
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
    
    private void renderDialogContent(GuiGraphics graphics) {
        DialogEntry entry = dialogEntries.get(currentEntryIndex);
        int boxHeight = (int)(this.height * GalgameDialogConfig.DIALOG_BOX_HEIGHT_RATIO);
        int boxTop = this.height - boxHeight;
        
        // 渲染说话者名称
        int nameX = GalgameDialogConfig.DIALOG_PADDING;
        int nameY = boxTop + GalgameDialogConfig.DIALOG_PADDING;
        
        String speakerName = entry.speaker().getString();
        int nameWidth = font.width(speakerName) + 16;
        int nameHeight = 18;
        
        GalgameGradientRenderer.renderNameTagBackground(
                graphics, nameX, nameY, nameWidth, nameHeight, animationProgress);
        
        int alpha = (int)(255 * animationProgress);
        int nameColor = (alpha << 24) | (GalgameDialogConfig.SPEAKER_NAME_COLOR & 0x00FFFFFF);
        graphics.drawString(font, speakerName, nameX + 8, nameY + 5, nameColor, false);
        
        // 渲染对话文本
        int textX = GalgameDialogConfig.DIALOG_PADDING;
        int textY = nameY + nameHeight + GalgameDialogConfig.NAME_TEXT_SPACING;
        int textColor = (alpha << 24) | (GalgameDialogConfig.DIALOG_TEXT_COLOR & 0x00FFFFFF);
        
        textRenderer.renderWithShadow(graphics, font, textX, textY, textColor);
    }
    
    private void renderContinueHint(GuiGraphics graphics) {
        int hintX = this.width - GalgameDialogConfig.DIALOG_PADDING - 10;
        int hintY = this.height - GalgameDialogConfig.DIALOG_PADDING - 10;
        
        int alpha = (int)(255 * continueHintAlpha * animationProgress);
        int color = (alpha << 24) | (GalgameDialogConfig.SPEAKER_NAME_COLOR & 0x00FFFFFF);
        
        graphics.drawString(font, GalgameDialogConfig.CONTINUE_HINT, hintX, hintY, color, false);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // 点击推进对话
        if (button == 0 && !showingOptions) {
            advanceDialog();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC关闭
        if (keyCode == 256) {
            onClose();
            return true;
        }
        
        // 空格/回车推进对话
        if ((keyCode == 32 || keyCode == 257) && !showingOptions) {
            advanceDialog();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        // 不再需要恢复相机，因为我们使用独立的实体渲染
        super.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * 对话条目数据
     */
    public record DialogEntry(Component speaker, Component text, boolean isNpc) {}
    
    /**
     * 对话选项数据
     */
    public record DialogOptionData(Component text, String actionId) {}
}
