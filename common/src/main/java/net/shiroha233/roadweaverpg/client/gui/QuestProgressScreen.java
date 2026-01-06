package net.shiroha233.roadweaverpg.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.ObjectiveProgress;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;

import java.util.List;

/**
 * 委托进度查看界面 - 使用卷轴PNG背景
 */
public class QuestProgressScreen extends Screen {
    
    // 背景纹理
    private static final ResourceLocation BACKGROUND = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "textures/gui/quest_scroll_bg.png");
    
    // 纹理尺寸
    private static final int TEX_WIDTH = 320;
    private static final int TEX_HEIGHT = 320;
    
    // 显示尺寸（放大以覆盖内容）
    private static final int GUI_WIDTH = 430;
    private static final int GUI_HEIGHT = 430;
    
    // 内容区域边距（相对于卷轴边框，按比例调整）
    private static final int CONTENT_LEFT = 120;
    private static final int CONTENT_TOP = 90;
    private static final int CONTENT_RIGHT = 120;
    
    // 文字颜色（适配羊皮纸背景）
    private static final int COLOR_TITLE = 0xFF3D2817;      // 深棕色标题
    private static final int COLOR_DESC = 0xFF5C4033;       // 中棕色描述
    private static final int COLOR_OBJECTIVE = 0xFF4A3728;  // 目标文字
    private static final int COLOR_PROGRESS = 0xFF2E8B57;   // 进度数字（绿色）
    private static final int COLOR_COMPLETED = 0xFF228B22;  // 已完成（深绿）
    private static final int COLOR_TIME = 0xFFB8860B;       // 时间（金色）
    private static final int COLOR_HINT = 0xFF6B8E23;       // 提示（橄榄绿）
    
    private final QuestDefinition definition;
    private final QuestInstance instance;
    
    private int guiLeft, guiTop;
    private int contentWidth;
    
    public QuestProgressScreen(QuestDefinition definition, QuestInstance instance) {
        super(definition.getDisplayTitle());
        this.definition = definition;
        this.instance = instance;
    }
    
    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        contentWidth = GUI_WIDTH - CONTENT_LEFT - CONTENT_RIGHT;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderScrollBackground(graphics);
        renderContent(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    /** 渲染卷轴背景 */
    private void renderScrollBackground(GuiGraphics graphics) {
        RenderSystem.setShaderTexture(0, BACKGROUND);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        graphics.blit(BACKGROUND, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 
                0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
        
        RenderSystem.disableBlend();
    }
    
    /** 渲染内容 */
    private void renderContent(GuiGraphics graphics) {
        int x = guiLeft + CONTENT_LEFT;
        int y = guiTop + CONTENT_TOP;
        
        // 标题（居中）
        String titleStr = definition.getTitle().getString();
        graphics.drawCenteredString(font, titleStr, guiLeft + GUI_WIDTH / 2, y, COLOR_TITLE);
        y += 16;
        
        // 状态（右对齐，不显示等级）
        Component stateText = getStateText();
        int stateWidth = font.width(stateText);
        graphics.drawString(font, stateText, guiLeft + GUI_WIDTH - CONTENT_RIGHT - stateWidth, y, 
                0xFFFFFF, false);
        y += 16;
        
        // 分隔线
        for (int i = 0; i < contentWidth; i += 4) {
            graphics.fill(x + i, y, x + i + 2, y + 1, 0x40000000);
        }
        y += 10;
        
        // 描述
        String desc = definition.getDescription().getString();
        List<String> descLines = wrapText(desc, contentWidth);
        for (String line : descLines) {
            graphics.drawString(font, line, x, y, COLOR_DESC, false);
            y += 11;
        }
        y += 10;
        
        // 目标标题
        String objectivesTitle = "◆ " + Component.translatable("gui.roadweaver_rpg.quest_progress.objectives").getString();
        graphics.drawString(font, objectivesTitle, x, y, COLOR_TITLE, false);
        y += 14;
        
        // 目标列表
        List<QuestObjective> objectives = definition.getObjectives();
        for (QuestObjective obj : objectives) {
            ObjectiveProgress progress = instance.getObjectiveProgress(obj.getId());
            y = renderObjective(graphics, x, y, obj, progress);
        }
        y += 10;
        
        // 剩余时间
        if (definition.hasTimeLimit()) {
            int remaining = instance.getRemainingTime();
            String timeStr;
            if (remaining > 0) {
                int minutes = remaining / 60;
                int seconds = remaining % 60;
                timeStr = "⏱ " + Component.translatable("gui.roadweaver_rpg.quest_progress.time_remaining", 
                        String.format("%d:%02d", minutes, seconds)).getString();
            } else {
                timeStr = "⏱ " + Component.translatable("gui.roadweaver_rpg.quest_progress.expired").getString();
            }
            graphics.drawString(font, timeStr, x, y, COLOR_TIME, false);
            y += 14;
        }
        
        // 完成提示
        if (instance.getState() == QuestState.COMPLETED) {
            y += 6;
            String hint = "✦ " + Component.translatable("gui.roadweaver_rpg.quest_progress.turn_in_hint").getString() + " ✦";
            int hintWidth = font.width(hint);
            graphics.drawString(font, hint, guiLeft + (GUI_WIDTH - hintWidth) / 2, y, COLOR_HINT, false);
        }
    }
    
    /** 渲染单个目标 */
    private int renderObjective(GuiGraphics graphics, int x, int y, 
                                 QuestObjective objective, ObjectiveProgress progress) {
        boolean completed = progress != null && progress.isCompleted();
        int current = progress != null ? progress.getCurrentProgress() : 0;
        int required = objective.getRequiredAmount();
        
        // 状态标记
        String marker = completed ? "✓" : "○";
        int markerColor = completed ? COLOR_COMPLETED : COLOR_OBJECTIVE;
        graphics.drawString(font, marker, x, y, markerColor, false);
        
        // 目标描述
        String objText = objective.getDescription().getString();
        int textColor = completed ? COLOR_COMPLETED : COLOR_OBJECTIVE;
        graphics.drawString(font, objText, x + 12, y, textColor, false);
        
        // 进度
        String progressStr = "(" + current + "/" + required + ")";
        int progressColor = completed ? COLOR_COMPLETED : COLOR_PROGRESS;
        int progressWidth = font.width(progressStr);
        graphics.drawString(font, progressStr, 
                guiLeft + GUI_WIDTH - CONTENT_RIGHT - progressWidth, y, progressColor, false);
        
        return y + 12;
    }
    
    /** 获取状态文本 */
    private Component getStateText() {
        return switch (instance.getState()) {
            case IN_PROGRESS -> Component.translatable("gui.roadweaver_rpg.quest_progress.in_progress").withStyle(style -> style.withColor(0xDAA520));
            case COMPLETED -> Component.translatable("gui.roadweaver_rpg.quest_progress.completed").withStyle(style -> style.withColor(0x228B22));
            case EXPIRED -> Component.translatable("gui.roadweaver_rpg.quest_progress.expired").withStyle(style -> style.withColor(0xCD5C5C));
            case FAILED -> Component.translatable("gui.roadweaver_rpg.quest_progress.failed").withStyle(style -> style.withColor(0xCD5C5C));
            case TURNED_IN -> Component.translatable("gui.roadweaver_rpg.quest_progress.turned_in").withStyle(style -> style.withColor(0x808080));
            default -> Component.literal(instance.getState().getSerializedName());
        };
    }
    
    /** 文字换行 */
    private List<String> wrapText(String text, int maxWidth) {
        return font.getSplitter().splitLines(text, maxWidth, net.minecraft.network.chat.Style.EMPTY)
                .stream()
                .map(formattedText -> formattedText.getString())
                .toList();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
