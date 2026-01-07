package net.shiroha233.roadweaverpg.client.gui.quest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.client.data.ClientDailyQuestData;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.quest.type.QuestRank;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;

import java.util.*;
import java.util.function.Consumer;

/**
 * 委托看板界面 - 按等级分列竖向排列委托
 */
public class QuestBoardScreen extends Screen {
    
    private static final int GUI_SIZE_RATIO = 85;
    private static final int TITLE_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 24;
    private static final int CARD_HEIGHT = 28;
    private static final int CARD_SPACING = 6;
    private static final int PADDING = 16;
    
    private final Map<QuestRank, List<QuestDefinition>> questsByRank;
    private final Consumer<ResourceLocation> acceptHandler;
    private final Map<QuestRank, Integer> scrollOffsets = new EnumMap<>(QuestRank.class);
    
    private final int playerReputationLevel;
    
    private QuestDefinition hoveredQuest = null;
    private QuestDefinition selectedQuest = null;
    
    private int guiLeft, guiTop, guiWidth, guiHeight;
    
    public QuestBoardScreen(List<QuestDefinition> quests, Consumer<ResourceLocation> acceptHandler) {
        this(quests, acceptHandler, 0);
    }
    
    public QuestBoardScreen(List<QuestDefinition> quests, Consumer<ResourceLocation> acceptHandler, int playerReputationLevel) {
        super(Component.translatable("gui.roadweaver_rpg.quest_board.title"));
        this.acceptHandler = acceptHandler;
        this.playerReputationLevel = playerReputationLevel;
        
        this.questsByRank = new EnumMap<>(QuestRank.class);
        for (QuestRank rank : QuestRank.values()) {
            questsByRank.put(rank, new ArrayList<>());
            scrollOffsets.put(rank, 0);
        }
        for (QuestDefinition quest : quests) {
            questsByRank.get(quest.getRank()).add(quest);
        }
    }
    
    @Override
    protected void init() {
        super.init();
        guiWidth = width * GUI_SIZE_RATIO / 100;
        guiHeight = height * GUI_SIZE_RATIO / 100;
        guiLeft = (width - guiWidth) / 2;
        guiTop = (height - guiHeight) / 2;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTitle(graphics);
        renderMainPanel(graphics);
        
        hoveredQuest = null;
        for (QuestRank rank : QuestRank.values()) {
            renderRankColumn(graphics, rank, mouseX, mouseY);
        }
        
        if (hoveredQuest != null) {
            renderQuestTooltip(graphics, mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderMainPanel(GuiGraphics graphics) {
        graphics.fill(guiLeft + 3, guiTop + 3, guiLeft + guiWidth + 3, guiTop + guiHeight + 3, 0x60000000);
        GuiRenderer.drawWoodenPanel(graphics, guiLeft, guiTop, guiWidth, guiHeight);
    }
    
    private void renderTitle(GuiGraphics graphics) {
        int titleY = guiTop - TITLE_HEIGHT - 5;
        int titleWidth = font.width(title) + 40;
        int titleX = guiLeft + guiWidth / 2 - titleWidth / 2;
        GuiRenderer.drawPanel(graphics, titleX, titleY, titleWidth, TITLE_HEIGHT, 
                0xDD3D2817, 0xFF8B7355, 2);
        graphics.drawCenteredString(font, title, guiLeft + guiWidth / 2, titleY + 6, 0xFFE8DCC8);
    }
    
    private void renderRankColumn(GuiGraphics graphics, QuestRank rank, int mouseX, int mouseY) {
        int availableWidth = guiWidth - PADDING * 2;
        int actualColumnWidth = availableWidth / 5;
        
        int columnX = guiLeft + PADDING + rank.getIndex() * actualColumnWidth;
        int columnY = guiTop + PADDING;
        int columnHeight = guiHeight - PADDING * 2;
        
        int headerBgColor = (rank.getScrollColor() & 0xFFFFFF) | 0x40000000;
        graphics.fill(columnX + 2, columnY, columnX + actualColumnWidth - 2, columnY + HEADER_HEIGHT, headerBgColor);
        
        Component rankLabel = Component.literal(rank.getDisplayName() + "级").withStyle(rank.getColor());
        graphics.drawCenteredString(font, rankLabel, columnX + actualColumnWidth / 2, columnY + 8, 0xFFFFFF);
        graphics.fill(columnX + 4, columnY + HEADER_HEIGHT, columnX + actualColumnWidth - 4, 
                columnY + HEADER_HEIGHT + 1, 0x80000000);
        
        int cardAreaY = columnY + HEADER_HEIGHT + 4;
        int cardAreaHeight = columnHeight - HEADER_HEIGHT - 8;
        
        graphics.enableScissor(columnX, cardAreaY, columnX + actualColumnWidth, cardAreaY + cardAreaHeight);
        
        List<QuestDefinition> quests = questsByRank.get(rank);
        int scrollOffset = scrollOffsets.get(rank);
        
        for (int i = 0; i < quests.size(); i++) {
            int cardY = cardAreaY + i * (CARD_HEIGHT + CARD_SPACING) - scrollOffset;
            
            if (cardY + CARD_HEIGHT >= cardAreaY && cardY < cardAreaY + cardAreaHeight) {
                QuestDefinition quest = quests.get(i);
                boolean hovered = isMouseOverCard(mouseX, mouseY, columnX + 4, cardY, actualColumnWidth - 8, CARD_HEIGHT);
                boolean selected = quest == selectedQuest;
                
                if (hovered) hoveredQuest = quest;
                renderQuestCard(graphics, columnX + 4, cardY, actualColumnWidth - 8, CARD_HEIGHT, quest, hovered, selected);
            }
        }
        
        graphics.disableScissor();
        
        if (quests.size() * (CARD_HEIGHT + CARD_SPACING) > cardAreaHeight) {
            renderScrollIndicator(graphics, columnX + actualColumnWidth - 6, cardAreaY, cardAreaHeight, 
                    scrollOffset, quests.size() * (CARD_HEIGHT + CARD_SPACING) - cardAreaHeight);
        }
    }
    
    private void renderQuestCard(GuiGraphics graphics, int x, int y, int width, int height,
                                  QuestDefinition quest, boolean hovered, boolean selected) {
        boolean locked = quest.hasReputationRequirement() && playerReputationLevel < quest.getMinReputationLevel();
        boolean isDaily = ClientDailyQuestData.getInstance().isDailyQuest(quest.getId());
        
        int cardColor = locked ? 0x808080 : quest.getRank().getScrollColor();
        GuiRenderer.drawQuestCard(graphics, x, y, width, height, cardColor, hovered && !locked, selected);
        
        String titleStr = quest.getTitle().getString();
        int maxWidth = width - 12;
        
        if (isDaily) {
            titleStr = "★ " + titleStr;
        }
        
        if (font.width(titleStr) > maxWidth) {
            while (font.width(titleStr + "..") > maxWidth && titleStr.length() > 0) {
                titleStr = titleStr.substring(0, titleStr.length() - 1);
            }
            titleStr += "..";
        }
        
        int textColor;
        if (locked) {
            textColor = 0xFF888888;
        } else if (isDaily) {
            textColor = hovered ? 0xFFFFD700 : 0xFFDAA520;
        } else {
            textColor = hovered ? 0xFF3D2817 : 0xFF5C4033;
        }
        
        graphics.drawString(font, titleStr, x + 8, y + (height - 8) / 2, textColor, false);
        
        if (locked) {
            graphics.drawString(font, "🔒", x + width - 16, y + (height - 8) / 2, 0xFFAA0000, false);
        }
    }
    
    private void renderScrollIndicator(GuiGraphics graphics, int x, int y, int height, int offset, int maxOffset) {
        if (maxOffset <= 0) return;
        graphics.fill(x, y, x + 3, y + height, 0x40000000);
        int thumbHeight = Math.max(20, height * height / (height + maxOffset));
        int thumbY = y + (int)((height - thumbHeight) * ((float)offset / maxOffset));
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xAA8B7355);
    }
    
    private void renderQuestTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(hoveredQuest.getDisplayTitle());
        
        String desc = hoveredQuest.getDescription().getString();
        if (desc.length() > 40) desc = desc.substring(0, 40) + "...";
        tooltip.add(Component.literal(desc).withStyle(style -> style.withColor(0x888888)));
        
        if (ClientDailyQuestData.getInstance().isDailyQuest(hoveredQuest.getId())) {
            tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.daily_quest")
                    .withStyle(style -> style.withColor(0xFFD700)));
        }
        
        if (hoveredQuest.hasReputationRequirement()) {
            int required = hoveredQuest.getMinReputationLevel();
            boolean met = playerReputationLevel >= required;
            int color = met ? 0x55FF55 : 0xFF5555;
            tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.reputation_required", required)
                    .withStyle(style -> style.withColor(color)));
            if (!met) {
                tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.reputation_current", playerReputationLevel)
                        .withStyle(style -> style.withColor(0xAAAAAA).withItalic(true)));
            }
        }
        
        if (hoveredQuest.hasTimeLimit()) {
            int minutes = hoveredQuest.getTimeLimit() / 60;
            tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.time_limit", 
                    Component.translatable("gui.roadweaver_rpg.time.minutes", minutes))
                    .withStyle(style -> style.withColor(0xFFAA00)));
        }
        
        if (hoveredQuest.isRepeatable()) {
            tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.repeatable")
                    .withStyle(style -> style.withColor(0x55FFFF)));
        }
        
        tooltip.add(Component.literal(""));
        
        boolean locked = hoveredQuest.hasReputationRequirement() && playerReputationLevel < hoveredQuest.getMinReputationLevel();
        if (locked) {
            tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.locked")
                    .withStyle(style -> style.withColor(0xFF5555).withItalic(true)));
        } else {
            tooltip.add(Component.translatable("gui.roadweaver_rpg.quest_board.click_to_view")
                    .withStyle(style -> style.withColor(0x55FF55).withItalic(true)));
        }
        
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }
    
    private boolean isMouseOverCard(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredQuest != null) {
            boolean locked = hoveredQuest.hasReputationRequirement() && 
                    playerReputationLevel < hoveredQuest.getMinReputationLevel();
            if (locked) {
                return true;
            }
            
            selectedQuest = hoveredQuest;
            if (minecraft != null) {
                minecraft.setScreen(new QuestPreviewScreen(this, hoveredQuest, acceptHandler, playerReputationLevel));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int availableWidth = guiWidth - PADDING * 2;
        int actualColumnWidth = availableWidth / 5;
        
        for (QuestRank rank : QuestRank.values()) {
            int columnX = guiLeft + PADDING + rank.getIndex() * actualColumnWidth;
            if (mouseX >= columnX && mouseX < columnX + actualColumnWidth) {
                int cardAreaHeight = guiHeight - PADDING * 2 - HEADER_HEIGHT - 12;
                int maxOffset = Math.max(0, questsByRank.get(rank).size() * (CARD_HEIGHT + CARD_SPACING) - cardAreaHeight);
                int currentOffset = scrollOffsets.get(rank);
                int newOffset = (int) Math.max(0, Math.min(maxOffset, currentOffset - delta * 20));
                scrollOffsets.put(rank, newOffset);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
