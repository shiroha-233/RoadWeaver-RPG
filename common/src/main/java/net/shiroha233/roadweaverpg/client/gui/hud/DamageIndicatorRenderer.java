package net.shiroha233.roadweaverpg.client.gui.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 伤害飘字渲染器
 */
public final class DamageIndicatorRenderer {
    
    private DamageIndicatorRenderer() {}
    
    private static final CopyOnWriteArrayList<DamageIndicator> indicators = new CopyOnWriteArrayList<>();
    
    private static final int LIFETIME = 30;
    private static final double RISE_SPEED = 0.05;
    private static final double RANDOM_OFFSET = 0.3;
    private static final int MAX_INDICATORS = 30;
    
    public static void addIndicator(double x, double y, double z, float damage, boolean isCritical) {
        double offsetX = (Math.random() - 0.5) * RANDOM_OFFSET * 2;
        double offsetZ = (Math.random() - 0.5) * RANDOM_OFFSET * 2;
        
        indicators.add(new DamageIndicator(x + offsetX, y, z + offsetZ, damage, isCritical, LIFETIME));
        
        // 限制数量防止内存泄漏
        while (indicators.size() > MAX_INDICATORS) {
            indicators.remove(0);
        }
    }
    
    public static void tick() {
        indicators.removeIf(indicator -> {
            indicator.tick();
            return indicator.isExpired();
        });
    }
    
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, 
                               Camera camera, float partialTick) {
        if (indicators.isEmpty()) return;
        
        Font font = Minecraft.getInstance().font;
        Vec3 cameraPos = camera.getPosition();
        
        for (DamageIndicator indicator : indicators) {
            renderIndicator(poseStack, bufferSource, font, camera, cameraPos, indicator, partialTick);
        }
    }
    
    private static void renderIndicator(PoseStack poseStack, MultiBufferSource bufferSource,
                                         Font font, Camera camera, Vec3 cameraPos,
                                         DamageIndicator indicator, float partialTick) {
        double x = indicator.x;
        double y = indicator.y + indicator.getYOffset(partialTick);
        double z = indicator.z;
        
        double relX = x - cameraPos.x;
        double relY = y - cameraPos.y;
        double relZ = z - cameraPos.z;
        
        double distSq = relX * relX + relY * relY + relZ * relZ;
        if (distSq > 48 * 48) return;
        
        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);
        poseStack.mulPose(camera.rotation());
        
        // 缩放
        float scale = indicator.isCritical ? 0.04f : 0.028f;
        float distScale = (float) Math.sqrt(distSq) * 0.08f + 1.0f;
        scale *= Math.min(distScale, 1.8f);
        poseStack.scale(-scale, -scale, scale);
        
        String text = formatDamage(indicator.damage);
        int textWidth = font.width(text);
        float alpha = indicator.getAlpha();
        int alphaInt = (int)(alpha * 255);
        
        // 暴击橙红色，普通白色
        int color = indicator.isCritical ? (0xFF5500 | (alphaInt << 24)) : (0xFFFFFF | (alphaInt << 24));
        
        Matrix4f matrix = poseStack.last().pose();
        
        // 渲染文本，无背景
        font.drawInBatch(
                text,
                -textWidth / 2f,
                0,
                color,
                true,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0, // 无背景
                15728880
        );
        
        // 暴击显示"暴击!"
        if (indicator.isCritical && indicator.lifetime > LIFETIME - 10) {
            String critText = net.minecraft.client.resources.language.I18n.get("combat.roadweaver_rpg.critical_hit");
            int critWidth = font.width(critText);
            int critColor = 0xFFD700 | (alphaInt << 24);
            
            font.drawInBatch(
                    critText,
                    -critWidth / 2f,
                    -10,
                    critColor,
                    true,
                    matrix,
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    15728880
            );
        }
        
        poseStack.popPose();
    }
    
    private static String formatDamage(float damage) {
        if (damage >= 1000) {
            return String.format("%.1fK", damage / 1000);
        } else if (damage == (int) damage) {
            return String.valueOf((int) damage);
        } else {
            return String.format("%.1f", damage);
        }
    }
    
    private static class DamageIndicator {
        final double x, z;
        double y;
        final float damage;
        final boolean isCritical;
        int lifetime;
        final int maxLifetime;
        
        DamageIndicator(double x, double y, double z, float damage, boolean isCritical, int lifetime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.damage = damage;
            this.isCritical = isCritical;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
        }
        
        void tick() {
            lifetime--;
            y += RISE_SPEED;
        }
        
        boolean isExpired() {
            return lifetime <= 0;
        }
        
        float getYOffset(float partialTick) {
            return (float)(RISE_SPEED * (1 - partialTick));
        }
        
        float getAlpha() {
            if (lifetime > maxLifetime * 0.7f) return 1.0f;
            return lifetime / (maxLifetime * 0.7f);
        }
    }
}
