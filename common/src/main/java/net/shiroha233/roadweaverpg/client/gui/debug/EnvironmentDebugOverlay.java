package net.shiroha233.roadweaverpg.client.gui.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * 环境调试覆盖层 - 实时显示玩家所处环境信息
 * 用于调试条件判定系统
 * 按键绑定：Z+V 切换显示
 */
public class EnvironmentDebugOverlay {
    
    private static boolean enabled = false;
    private static ResourceLocation dimension = null;
    private static ResourceLocation biome = null;
    private static int posX, posY, posZ;
    private static boolean isRaining, isThundering, isDay, isNight, canSeeSky, isRainingAtPlayer;
    private static int moonPhase;
    private static long worldTime;
    private static int lightLevel, skyLight, blockLight;
    
    public static void toggle() {
        enabled = !enabled;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String status = enabled ? "§a已开启" : "§c已关闭";
            mc.player.displayClientMessage(
                    Component.literal("§e[调试] §f环境信息显示 " + status), true);
        }
    }
    
    public static boolean isEnabled() { return enabled; }
    
    /** 从玩家更新环境数据 */
    public static void updateFromPlayer(LocalPlayer player) {
        if (player == null) return;
        
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        
        posX = pos.getX();
        posY = pos.getY();
        posZ = pos.getZ();
        dimension = level.dimension().location();
        
        Holder<Biome> biomeHolder = level.getBiome(pos);
        biome = biomeHolder.unwrapKey().map(key -> key.location()).orElse(new ResourceLocation("unknown"));
        
        isRaining = level.isRaining();
        isThundering = level.isThundering();
        isRainingAtPlayer = level.isRainingAt(pos.above());
        
        worldTime = level.getDayTime() % 24000;
        isDay = worldTime >= 0 && worldTime < 12000;
        isNight = worldTime >= 13000 && worldTime < 23000;
        moonPhase = level.getMoonPhase();
        
        lightLevel = level.getMaxLocalRawBrightness(pos);
        skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        canSeeSky = level.canSeeSky(pos);
    }
    
    /** 渲染调试信息 */
    public static void render(GuiGraphics graphics, float partialTick) {
        if (!enabled) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        int x = 10, y = 10, lineHeight = 11;
        graphics.fill(x - 5, y - 5, x + 220, y + 185, 0xA0000000);
        
        // 标题
        graphics.drawString(mc.font, "§e§l【环境调试信息】", x, y, 0xFFFF00, false);
        y += lineHeight + 5;
        
        // 位置
        graphics.drawString(mc.font, "§6▶ 位置", x, y, 0xFFAA00, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7坐标: §f" + posX + ", " + posY + ", " + posZ, x, y, 0xFFFFFF, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7维度: §f" + (dimension != null ? dimension.toString() : "未知"), x, y, 0xFFFFFF, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7群系: §f" + (biome != null ? biome.toString() : "未知"), x, y, 0xFFFFFF, false);
        y += lineHeight + 3;
        
        // 天气
        graphics.drawString(mc.font, "§6▶ 天气", x, y, 0xFFAA00, false);
        y += lineHeight;
        String weather = isThundering ? "§c雷暴" : (isRaining ? "§9下雨" : "§a晴朗");
        graphics.drawString(mc.font, "  §7全局天气: " + weather, x, y, 0xFFFFFF, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7玩家位置下雨: " + (isRainingAtPlayer ? "§9是" : "§a否"), x, y, 0xFFFFFF, false);
        y += lineHeight + 3;
        
        // 时间
        graphics.drawString(mc.font, "§6▶ 时间", x, y, 0xFFAA00, false);
        y += lineHeight;
        String time = isDay ? "§e白天" : (isNight ? "§8夜晚" : "§6黄昏/黎明");
        graphics.drawString(mc.font, "  §7时段: " + time + " §7(" + worldTime + ")", x, y, 0xFFFFFF, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7月相: §f" + getMoonPhaseName(moonPhase) + " §7(" + moonPhase + ")", x, y, 0xFFFFFF, false);
        y += lineHeight + 3;
        
        // 光照
        graphics.drawString(mc.font, "§6▶ 光照", x, y, 0xFFAA00, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7综合亮度: §f" + lightLevel, x, y, 0xFFFFFF, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7天空光: §f" + skyLight + " §7| 方块光: §f" + blockLight, x, y, 0xFFFFFF, false);
        y += lineHeight;
        graphics.drawString(mc.font, "  §7天空可见: " + (canSeeSky ? "§a可见" : "§c不可见"), x, y, 0xFFFFFF, false);
        y += lineHeight + 8;
        
        graphics.drawString(mc.font, "§8按 Z+V 切换显示", x, y, 0x888888, false);
        RenderSystem.disableBlend();
    }
    
    private static String getMoonPhaseName(int phase) {
        return switch (phase) {
            case 0 -> "满月"; case 1 -> "亏凸月"; case 2 -> "下弦月"; case 3 -> "残月";
            case 4 -> "新月"; case 5 -> "娥眉月"; case 6 -> "上弦月"; case 7 -> "盈凸月";
            default -> "未知";
        };
    }
}
