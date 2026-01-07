package net.shiroha233.roadweaverpg.client.gui.dialog;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 相机控制器 - 管理对话时的视角变换
 * 将玩家视角移动到NPC面对面偏右方位置
 */
public class CameraController {
    
    private float originalYaw;
    private float originalPitch;
    private boolean hasSavedCamera = false;
    
    private static final float RIGHT_OFFSET_ANGLE = 25f;
    private static final float PITCH_OFFSET = -5f;
    
    /**
     * 设置对话视角
     */
    public void setupCamera(Player player, LivingEntity target) {
        if (player == null || target == null) return;
        
        originalYaw = player.getYRot();
        originalPitch = player.getXRot();
        hasSavedCamera = true;
        
        Vec3 playerPos = player.position();
        Vec3 targetPos = target.position();
        
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        
        float targetYaw = (float)(Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        float newYaw = targetYaw + RIGHT_OFFSET_ANGLE;
        
        double dy = (targetPos.y + target.getEyeHeight()) - (playerPos.y + player.getEyeHeight());
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float newPitch = (float)(-Mth.atan2(dy, horizontalDist) * (180D / Math.PI)) + PITCH_OFFSET;
        
        player.setYRot(newYaw);
        player.setXRot(Mth.clamp(newPitch, -90f, 90f));
        
        player.yRotO = newYaw;
        player.xRotO = player.getXRot();
    }
    
    /**
     * 恢复原始视角
     */
    public void restoreCamera() {
        if (!hasSavedCamera) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setYRot(originalYaw);
            mc.player.setXRot(originalPitch);
            mc.player.yRotO = originalYaw;
            mc.player.xRotO = originalPitch;
        }
        
        hasSavedCamera = false;
    }
    
    /**
     * 平滑过渡视角
     */
    public void smoothTransition(Player player, float targetYaw, float targetPitch, float progress) {
        if (player == null) return;
        
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        
        float newYaw = Mth.lerp(progress, currentYaw, targetYaw);
        float newPitch = Mth.lerp(progress, currentPitch, targetPitch);
        
        player.setYRot(newYaw);
        player.setXRot(newPitch);
    }
}
