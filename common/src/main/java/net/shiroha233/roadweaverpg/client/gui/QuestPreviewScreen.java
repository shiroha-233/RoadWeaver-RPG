package net.shiroha233.roadweaverpg.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;

import java.util.List;
import java.util.function.Consumer;

/**
 * 委托预览界面 - 使用卷轴PNG背景（与QuestProgressScreen一致）
 * 用于在委托看板中查看委托详情，包含接取按钮
 */
public class QuestPreviewScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "textures/gui/quest_scroll_bg.png");
    
    private static final int TEX_WIDTH = 320;
    private static final int TEX_HEIGHT = 320;
    private static final int GUI_WIDTH = 430;
    private static final int GUI_HEIGHT = 430;
    
    private static final int CONTENT_LEFT = 120;
    private static final int CONTENT_TOP = 90;
    private static final int CONTENT_RIGHT = 120;
    private static final int CONTENT_BOTTOM = 100;
    
    // 文字颜色
    private static final int COLOR_TITLE = 0xFF3D2817;
    private static final int COLOR_DESC = 0xFF5C4033;
    private static final int COLOR_SECTION = 0xFF4A3728;
    private static final int COLOR_ITEM = 0xFF6B5D52;
    private static final int COLOR_TIME = 0xFFB8860B;
    private static final int COLOR_REPEAT = 0xFF2E8B8B;
    
    private final Screen parent;
    private final QuestDefinition quest;
    private final Consumer<ResourceLocation> acceptHandler;
    
    private int guiLeft, guiTop;
    private int contentWidth;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    
    public QuestPreviewScreen(Screen parent, QuestDefinition quest, Consumer<ResourceLocation> acceptHandler) {
        super(quest.getDisplayTitle());
        this.parent = parent;
        this.quest = quest;
        this.acceptHandler = acceptHandler;
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        contentWidth = GUI_WIDTH - CONTENT_LEFT - CONTENT_RIGHT;
        
        int buttonY = guiTop + GUI_HEIGHT - CONTENT_BOTTOM + 20;
        int buttonWidth = 80;
        int buttonSpacing = 20;
        int totalWidth = buttonWidth * 2 + buttonSpacing;
        int startX = guiLeft + (GUI_WIDTH - totalWidth) / 2;
        
        // 接取按钮
        addRenderableWidget(Button.builder(
                Component.translatable("gui.roadweaver_rpg.quest_board.accept"),
                btn -> {
                    acceptHandler.accept(quest.getId());
                    onClose();
                })
                .bounds(startX, buttonY, buttonWidth, 20)
                .build());
        
        // 返回按钮
        addRenderableWidget(Button.builder(
                Component.translatable("gui.roadweaver_rpg.quest_detail.close"),
                btn -> onClose())
                .bounds(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, 20)
                .build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderScrollBackground(graphics);
        renderContent(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderScrollBackground(GuiGraphics graphics) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 
                0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
        RenderSystem.disableBlend();
    }
    
    private void renderContent(GuiGraphics graphics) {
        int x = guiLeft + CONTENT_LEFT;
        int y = guiTop + CONTENT_TOP - scrollOffset;
        int contentHeight = GUI_HEIGHT - CONTENT_TOP - CONTENT_BOTTOM;
        
        // 启用裁剪
        graphics.enableScissor(guiLeft + CONTENT_LEFT - 10, guiTop + CONTENT_TOP, 
                guiLeft + GUI_WIDTH - CONTENT_RIGHT + 10, guiTop + GUI_HEIGHT - CONTENT_BOTTOM);
        
        int startY = y;
        
        // 标题
        String titleStr = quest.getTitle().getString();
        graphics.drawCenteredString(font, titleStr, guiLeft + GUI_WIDTH / 2, y, COLOR_TITLE);
        y += 16;
        
        // 等级标签
        Component rankText = Component.literal("[" + quest.getRank().getDisplayName() + "级委托]")
                .withStyle(quest.getRank().getColor());
        int rankWidth = font.width(rankText);
        graphics.drawString(font, rankText, guiLeft + (GUI_WIDTH - rankWidth) / 2, y, 0xFFFFFF, false);
        y += 16;
        
        // 分隔线
        renderSeparator(graphics, x, y);
        y += 10;
        
        // 描述
        String desc = quest.getDescription().getString();
        List<String> descLines = wrapText(desc, contentWidth);
        for (String line : descLines) {
            graphics.drawString(font, line, x, y, COLOR_DESC, false);
            y += 11;
        }
        y += 10;
        
        // 目标
        String objectivesTitle = "◆ " + Component.translatable("gui.roadweaver_rpg.quest_board.objectives").getString();
        graphics.drawString(font, objectivesTitle, x, y, COLOR_SECTION, false);
        y += 14;
        
        for (QuestObjective obj : quest.getObjectives()) {
            String objText = "○ " + obj.getDescription().getString();
            graphics.drawString(font, objText, x + 8, y, COLOR_ITEM, false);
            y += 12;
        }
        y += 10;
        
        // 奖励
        String rewardsTitle = "◆ " + Component.translatable("gui.roadweaver_rpg.quest_board.rewards").getString();
        graphics.drawString(font, rewardsTitle, x, y, COLOR_SECTION, false);
        y += 14;
        
        for (QuestReward reward : quest.getRewards()) {
            String rewardText = "★ " + reward.getDescription().getString();
            graphics.drawString(font, rewardText, x + 8, y, COLOR_ITEM, false);
            y += 12;
        }
        y += 10;
        
        // 时间限制
        if (quest.hasTimeLimit()) {
            int minutes = quest.getTimeLimit() / 60;
            String timeStr = "⏱ " + Component.translatable("gui.roadweaver_rpg.quest_board.time_limit", 
                    Component.translatable("gui.roadweaver_rpg.time.minutes", minutes)).getString();
            graphics.drawString(font, timeStr, x, y, COLOR_TIME, false);
            y += 14;
        }
        
        // 可重复标记
        if (quest.isRepeatable()) {
            String repeatStr = "↻ " + Component.translatable("gui.roadweaver_rpg.quest_board.repeatable").getString();
            graphics.drawString(font, repeatStr, x, y, COLOR_REPEAT, false);
            y += 14;
        }
        
        // 计算最大滚动偏移
        maxScrollOffset = Math.max(0, (y + scrollOffset) - startY - contentHeight + 20);
        
        graphics.disableScissor();
    }
    
    private void renderSeparator(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < contentWidth; i += 4) {
            graphics.fill(x + i, y, x + i + 2, y + 1, 0x40000000);
        }
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        return font.getSplitter().splitLines(text, maxWidth, net.minecraft.network.chat.Style.EMPTY)
                .stream()
                .map(formattedText -> formattedText.getString())
                .toList();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScrollOffset > 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScrollOffset, scrollOffset - delta * 15));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
