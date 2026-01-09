package net.shiroha233.roadweaverpg.client.gui.reputation;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.shiroha233.roadweaverpg.client.ClientReputationCache;
import net.shiroha233.roadweaverpg.client.gui.render.GuiRenderer;
import net.shiroha233.roadweaverpg.reputation.ReputationLevel;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;
import net.shiroha233.roadweaverpg.quest.reward.ItemReward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 声望总览界面 - 现代化设计，与冒险等级GUI风格一致
 * 使用纯黑半透明背景和竖向时间轴
 */
public class ReputationOverviewScreen extends Screen {
    
    // 布局常量
    private static final int NODE_RADIUS = 18;
    private static final int NODE_SPACING = 140;
    private static final int TIMELINE_Y_OFFSET = 100;
    private static final int REWARD_SLOT_SIZE = 24;
    private static final int REWARD_SLOT_SPACING = 3;
    private static final int MAX_REWARDS_PER_ROW = 3;
    
    // 颜色常量
    private static final int COLOR_BG_OVERLAY = 0x88000000;
    private static final int COLOR_TIMELINE = 0xFF3D3D5C;
    private static final int COLOR_TIMELINE_COMPLETED = 0xFF6B8E23;
    private static final int COLOR_NODE_LOCKED = 0xFF4A4A6A;
    private static final int COLOR_NODE_CURRENT = 0xFFFFD700;
    private static final int COLOR_NODE_COMPLETED = 0xFF32CD32;
    private static final int COLOR_TEXT_EXP = 0xFFB0B0B0;
    private static final int COLOR_GLOW = 0x40FFD700;
    
    // 滚动状态
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float scrollVelocity = 0;
    private boolean isDragging = false;
    private double lastDragX = 0;
    
    // 动画状态
    private float animationProgress = 0f;
    private int hoveredLevel = -1;
    
    // 数据
    private final Map<Integer, ReputationLevel> levels;
    private final int playerLevel;
    private final int playerExp;
    private final String factionId = "roadweaver_rpg:guild";
    private int maxLevel;
    
    public ReputationOverviewScreen() {
        super(Component.translatable("gui.roadweaver_rpg.reputation_overview.title"));
        this.levels = ClientReputationCache.getLevelDefinitions();
        this.playerLevel = ClientReputationCache.getPlayerLevel(factionId);
        this.playerExp = ClientReputationCache.getPlayerExperience(factionId);
        this.maxLevel = levels.keySet().stream().mapToInt(i -> i).max().orElse(60);
    }
    
    @Override
    protected void init() {
        super.init();
        targetScrollOffset = calculateScrollForLevel(playerLevel);
        scrollOffset = targetScrollOffset;
    }
    
    private float calculateScrollForLevel(int level) {
        int centerX = width / 2;
        float levelX = getNodeX(level);
        return levelX - centerX;
    }
    
    private float getNodeX(int level) {
        return 100 + (level - 1) * NODE_SPACING;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (animationProgress < 1f) {
            animationProgress = Math.min(1f, animationProgress + 0.05f);
        }
        
        if (!isDragging) {
            float diff = targetScrollOffset - scrollOffset;
            scrollVelocity = diff * 0.15f;
            scrollOffset += scrollVelocity;
            
            if (Math.abs(diff) < 0.5f) {
                scrollOffset = targetScrollOffset;
                scrollVelocity = 0;
            }
        }
        
        float minScroll = -100;
        float maxScroll = getNodeX(maxLevel) - width + 100;
        targetScrollOffset = Mth.clamp(targetScrollOffset, minScroll, maxScroll);
        scrollOffset = Mth.clamp(scrollOffset, minScroll, maxScroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, COLOR_BG_OVERLAY);
        
        // 顶部信息栏
        renderHeader(graphics);
        
        // 时间轴区域（横向）
        int timelineLeft = 60;
        int timelineRight = width - 20;
        graphics.enableScissor(timelineLeft, 60, timelineRight, height - 20);
        
        renderTimeline(graphics, mouseX, mouseY, partialTick);
        
        graphics.disableScissor();
        
        // 悬停提示
        if (hoveredLevel > 0) {
            renderLevelTooltip(graphics, mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void renderHeader(GuiGraphics graphics) {
        int alpha = (int)(255 * animationProgress);
        
        // 标题
        graphics.drawCenteredString(font, title, width / 2, 15, (alpha << 24) | 0xFFFFFF);
        
        // 当前等级和经验
        String levelText = String.format("Lv.%d", playerLevel);
        String expText = String.format("声望: %d", playerExp);
        
        int nextLevelExp = ClientReputationCache.getExpForNextLevel(factionId);
        String progressText = nextLevelExp > 0 
                ? String.format(" / %d", nextLevelExp) 
                : " (MAX)";
        
        int infoY = 35;
        int centerX = width / 2;
        
        graphics.drawCenteredString(font, levelText, centerX - 60, infoY, 
                (alpha << 24) | (COLOR_NODE_CURRENT & 0xFFFFFF));
        
        graphics.drawString(font, expText + progressText, centerX - 20, infoY, 
                (alpha << 24) | (COLOR_TEXT_EXP & 0xFFFFFF), false);
        
        // 进度条
        if (nextLevelExp > 0) {
            int currentLevelExp = ClientReputationCache.getExpForCurrentLevel(factionId);
            float progress = (float)(playerExp - currentLevelExp) / (nextLevelExp - currentLevelExp);
            progress = Mth.clamp(progress, 0f, 1f);
            
            int barWidth = 200;
            int barHeight = 6;
            int barX = centerX - barWidth / 2;
            int barY = infoY + 14;
            
            GuiRenderer.drawRoundedRect(graphics, barX, barY, barWidth, barHeight, 3, 0x60000000);
            int fillWidth = (int)(barWidth * progress);
            if (fillWidth > 0) {
                GuiRenderer.drawRoundedRect(graphics, barX, barY, fillWidth, barHeight, 3, 
                        (alpha << 24) | (COLOR_NODE_CURRENT & 0xFFFFFF));
            }
        }
    }
    
    private void renderTimeline(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int timelineY = TIMELINE_Y_OFFSET + 60;
        hoveredLevel = -1;
        
        // 绘制横向时间轴线
        for (int level = 1; level <= maxLevel; level++) {
            float nodeX = getNodeX(level) - scrollOffset;
            float nextNodeX = getNodeX(level + 1) - scrollOffset;
            
            if (nodeX > width + 100) break;
            
            if (level < maxLevel && nextNodeX > -100) {
                int lineColor = level < playerLevel ? COLOR_TIMELINE_COMPLETED : COLOR_TIMELINE;
                int lineY = timelineY;
                int lineStartX = (int)nodeX + NODE_RADIUS;
                int lineEndX = (int)nextNodeX - NODE_RADIUS;
                
                graphics.fill(lineStartX, lineY - 2, lineEndX, lineY + 2, lineColor);
                
                if (level < playerLevel) {
                    graphics.fill(lineStartX, lineY - 1, lineEndX, lineY + 1, 0x40FFFFFF);
                }
            }
        }
        
        // 绘制节点
        for (int level = 1; level <= maxLevel; level++) {
            float nodeX = getNodeX(level) - scrollOffset;
            
            if (nodeX > width + 100) break;
            if (nodeX < -200) continue;
            
            renderNode(graphics, (int)nodeX, timelineY, level, mouseX, mouseY);
        }
    }

    private void renderNode(GuiGraphics graphics, int x, int y, int level, int mouseX, int mouseY) {
        ReputationLevel levelInfo = levels.get(level);
        boolean isCompleted = level <= playerLevel;
        boolean isCurrent = level == playerLevel;
        boolean isNext = level == playerLevel + 1;
        
        boolean hovered = isPointInCircle(mouseX, mouseY, x, y, NODE_RADIUS + 5);
        if (hovered) {
            hoveredLevel = level;
        }
        
        int nodeColor;
        if (isCurrent) {
            nodeColor = COLOR_NODE_CURRENT;
        } else if (isCompleted) {
            nodeColor = COLOR_NODE_COMPLETED;
        } else if (isNext) {
            nodeColor = 0xFF5A5AFF;
        } else {
            nodeColor = COLOR_NODE_LOCKED;
        }
        
        if (isCurrent || isNext || hovered) {
            float glowScale = 1.0f + 0.1f * (float)Math.sin(System.currentTimeMillis() / 200.0);
            int glowRadius = (int)(NODE_RADIUS * glowScale) + 8;
            drawGlowCircle(graphics, x, y, glowRadius, isCurrent ? COLOR_GLOW : 0x30FFFFFF);
        }
        
        drawFilledCircle(graphics, x, y, NODE_RADIUS, nodeColor);
        
        int borderColor = hovered ? 0xFFFFFFFF : (isCompleted ? 0xFF90EE90 : 0xFF5A5A7A);
        drawCircleOutline(graphics, x, y, NODE_RADIUS, borderColor);
        
        String levelStr = String.valueOf(level);
        int textColor = level > playerLevel + 1 ? 0xFFAAAAAA : 0xFFFFFFFF;
        graphics.drawCenteredString(font, levelStr, x, y - 4, textColor);
        
        if (levelInfo != null) {
            String expStr = String.valueOf(levelInfo.getRequiredExperience());
            graphics.drawCenteredString(font, expStr + " XP", x, y + NODE_RADIUS + 8, COLOR_TEXT_EXP);
        }
        
        renderRewardSlots(graphics, x, y, level, levelInfo, isCompleted, hovered, mouseX, mouseY);
    }
    
    private void renderRewardSlots(GuiGraphics graphics, int nodeX, int nodeY, int level,
                                    ReputationLevel levelInfo, boolean completed, boolean nodeHovered,
                                    int mouseX, int mouseY) {
        if (levelInfo == null) return;
        
        List<ItemReward> itemRewards = levelInfo.getRewards().stream()
                .filter(r -> r instanceof ItemReward)
                .map(r -> (ItemReward) r)
                .toList();
        
        if (itemRewards.isEmpty()) return;
        
        int rewardCount = itemRewards.size();
        int cols = Math.min(rewardCount, MAX_REWARDS_PER_ROW);
        int rows = (rewardCount + MAX_REWARDS_PER_ROW - 1) / MAX_REWARDS_PER_ROW;
        int totalWidth = cols * REWARD_SLOT_SIZE + (cols - 1) * REWARD_SLOT_SPACING;
        int startX = nodeX - totalWidth / 2;
        int startY = nodeY + NODE_RADIUS + 20;
        
        int containerPadding = 4;
        int containerWidth = totalWidth + containerPadding * 2;
        int containerHeight = rows * REWARD_SLOT_SIZE + (rows - 1) * REWARD_SLOT_SPACING + containerPadding * 2;
        
        int bgAlpha = nodeHovered ? 0x90 : 0x60;
        GuiRenderer.drawRoundedRect(graphics, 
                startX - containerPadding, startY - containerPadding,
                containerWidth, containerHeight, 8, 
                (bgAlpha << 24) | 0x1A1A2E);
        
        for (int i = 0; i < rewardCount; i++) {
            ItemReward reward = itemRewards.get(i);
            int col = i % MAX_REWARDS_PER_ROW;
            int row = i / MAX_REWARDS_PER_ROW;
            int slotX = startX + col * (REWARD_SLOT_SIZE + REWARD_SLOT_SPACING);
            int slotY = startY + row * (REWARD_SLOT_SIZE + REWARD_SLOT_SPACING);
            
            renderItemRewardSlot(graphics, slotX, slotY, reward, completed, mouseX, mouseY);
        }
    }
    
    private void renderItemRewardSlot(GuiGraphics graphics, int x, int y, ItemReward itemReward, 
                                       boolean completed, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + REWARD_SLOT_SIZE && 
                         mouseY >= y && mouseY < y + REWARD_SLOT_SIZE;
        
        int bgColor = completed ? 0x40006400 : (hovered ? 0x60404040 : 0x40000000);
        GuiRenderer.drawRoundedRect(graphics, x, y, REWARD_SLOT_SIZE, REWARD_SLOT_SIZE, 4, bgColor);
        
        int borderColor = completed ? 0x6032CD32 : (hovered ? 0x80FFFFFF : 0x40FFFFFF);
        graphics.renderOutline(x, y, REWARD_SLOT_SIZE, REWARD_SLOT_SIZE, borderColor);
        
        var stack = itemReward.createDisplayStack();
        if (!stack.isEmpty()) {
            int itemX = x + (REWARD_SLOT_SIZE - 16) / 2;
            int itemY = y + (REWARD_SLOT_SIZE - 16) / 2;
            graphics.renderItem(stack, itemX, itemY);
            
            if (stack.getCount() > 1) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                String countStr = String.valueOf(stack.getCount());
                graphics.drawString(font, countStr, 
                        x + REWARD_SLOT_SIZE - font.width(countStr) - 1,
                        y + REWARD_SLOT_SIZE - 8, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }
        }
        
        if (completed) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.drawCenteredString(font, "✓", x + REWARD_SLOT_SIZE / 2, y + REWARD_SLOT_SIZE / 2 - 3, 0xFF32CD32);
            graphics.pose().popPose();
        }
    }

    private void renderLevelTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ReputationLevel levelInfo = levels.get(hoveredLevel);
        if (levelInfo == null) return;
        
        List<Component> tooltip = new ArrayList<>();
        
        boolean completed = hoveredLevel <= playerLevel;
        boolean current = hoveredLevel == playerLevel;
        
        String statusStr = completed ? (current ? " §e(当前)" : " §a(已达成)") : " §7(未解锁)";
        tooltip.add(Component.literal("§6声望等级 " + hoveredLevel + statusStr));
        
        tooltip.add(Component.literal("§7所需声望: §f" + levelInfo.getRequiredExperience()));
        
        if (!levelInfo.getRewards().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("§e奖励:"));
            for (QuestReward reward : levelInfo.getRewards()) {
                tooltip.add(Component.literal("  §7• " + reward.getDescription().getString()));
            }
        }
        
        graphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
    }
    
    // 绘制工具方法
    private void drawFilledCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int)Math.sqrt(radius * radius - y * y);
            graphics.fill(centerX - halfWidth, centerY + y, centerX + halfWidth, centerY + y + 1, color);
        }
    }
    
    private void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;
            
            int x1 = centerX + (int)(radius * Math.cos(angle1));
            int y1 = centerY + (int)(radius * Math.sin(angle1));
            int x2 = centerX + (int)(radius * Math.cos(angle2));
            int y2 = centerY + (int)(radius * Math.sin(angle2));
            
            graphics.fill(Math.min(x1, x2), Math.min(y1, y2), 
                    Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
        }
    }
    
    private void drawGlowCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int r = radius; r > radius - 6; r--) {
            int alpha = (color >> 24) & 0xFF;
            alpha = alpha * (radius - r + 1) / 6;
            int glowColor = (alpha << 24) | (color & 0x00FFFFFF);
            drawFilledCircle(graphics, centerX, centerY, r, glowColor);
        }
    }
    
    private boolean isPointInCircle(double px, double py, double cx, double cy, double radius) {
        double dx = px - cx;
        double dy = py - cy;
        return dx * dx + dy * dy <= radius * radius;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        targetScrollOffset -= (float)(delta * 60);
        return true;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = true;
            lastDragX = mouseX;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            double deltaX = mouseX - lastDragX;
            targetScrollOffset -= (float)deltaX;
            scrollOffset -= (float)deltaX;
            lastDragX = mouseX;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            onClose();
            return true;
        }
        if (keyCode == 263) { // LEFT
            targetScrollOffset -= 100;
            return true;
        }
        if (keyCode == 262) { // RIGHT
            targetScrollOffset += 100;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
