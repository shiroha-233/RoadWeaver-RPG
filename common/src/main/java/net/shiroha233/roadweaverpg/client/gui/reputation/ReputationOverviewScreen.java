package net.shiroha233.roadweaverpg.client.gui.reputation;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.client.ClientReputationCache;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;

import java.util.Map;

/**
 * 声望总览界面
 */
public class ReputationOverviewScreen extends Screen {
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 180;
    private static final int ITEM_HEIGHT = 30;

    private int guiLeft, guiTop;
    private double scrollAmount = 0;
    private final String factionId = "roadweaver_rpg:guild";

    public ReputationOverviewScreen() {
        super(Component.translatable("gui.roadweaver_rpg.reputation_overview.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        GuiRenderer.drawWoodenPanel(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        
        graphics.drawCenteredString(this.font, this.title, this.width / 2, guiTop + 10, 0xFFE8DCC8);

        int currentLevel = ClientReputationCache.getPlayerLevel(factionId);
        int currentXp = ClientReputationCache.getPlayerExperience(factionId);
        Component currentStatus = Component.translatable("gui.roadweaver_rpg.reputation.current_status", currentLevel, currentXp);
        graphics.drawString(this.font, currentStatus, guiLeft + 15, guiTop + 25, 0xFFAA00);

        int listY = guiTop + 40;
        int listHeight = GUI_HEIGHT - 55;
        graphics.enableScissor(guiLeft + 10, listY, guiLeft + GUI_WIDTH - 10, listY + listHeight);
        
        renderLevelList(graphics, mouseX, mouseY, listY);
        
        graphics.disableScissor();

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLevelList(GuiGraphics graphics, int mouseX, int mouseY, int startY) {
        Map<Integer, ReputationLevel> levels = ClientReputationCache.getLevelDefinitions();
        int yOffset = (int) -scrollAmount;

        for (int i = 1; i <= 60; i++) {
            ReputationLevel levelInfo = levels.get(i);
            int itemY = startY + yOffset + (i - 1) * ITEM_HEIGHT;
            
            if (itemY + ITEM_HEIGHT < startY || itemY > startY + GUI_HEIGHT) {
                continue;
            }

            int bgColor = i <= ClientReputationCache.getPlayerLevel(factionId) ? 0x4000FF00 : 0x40000000;
            graphics.fill(guiLeft + 15, itemY, guiLeft + GUI_WIDTH - 15, itemY + ITEM_HEIGHT - 2, bgColor);
            
            graphics.drawString(this.font, "Lv." + i, guiLeft + 20, itemY + 10, 0xFFFFFF);
            
            if (levelInfo != null) {
                graphics.drawString(this.font, "XP: " + levelInfo.getRequiredExperience(), guiLeft + 60, itemY + 10, 0xAAAAAA);
                
                int rewardX = guiLeft + 120;
                for (QuestReward reward : levelInfo.getRewards()) {
                    Component desc = reward.getDescription();
                    int width = this.font.width(desc);
                    if (rewardX + width < guiLeft + GUI_WIDTH - 20) {
                        graphics.drawString(this.font, desc, rewardX, itemY + 10, 0x55FF55);
                        rewardX += width + 10;
                    }
                }
            } else {
                graphics.drawString(this.font, "???", guiLeft + 60, itemY + 10, 0x555555);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollAmount = Math.max(0, Math.min(scrollAmount - delta * 20, Math.max(0, 60 * ITEM_HEIGHT - (GUI_HEIGHT - 55))));
        return true;
    }
}
