package net.shiroha233.roadweaverpg.client.gui.galgame;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Galgame双视角相机控制器
 * 职责：管理对话时的双角色视角，支持左右分屏展示NPC和玩家
 * 原理：通过计算NPC和玩家之间的中点位置，设置相机朝向以同时展示双方
 */
public class GalgameCameraController {
    
    private float originalYaw;
    private float originalPitch;
    private boolean hasSavedCamera = false;
    
    // 当前说话者（用于高亮效果）
    private Speaker currentSpeaker = Speaker.NPC;
    
    // 目标视角参数
    private float targetYaw;
    private float targetPitch;
    
    // 平滑过渡进度
    private float transitionProgress = 0f;
    
    public enum Speaker {
        NPC,    // NPC说话时，相机稍微偏向NPC
        PLAYER  // 玩家说话时，相机稍微偏向玩家
    }
    
    /**
     * 初始化对话视角
     * 设置相机位置使其能同时看到NPC和玩家的正面
     */
    public void setupDialogCamera(Player player, LivingEntity npc) {
        if (player == null || npc == null) return;
        
        // 保存原始视角
        originalYaw = player.getYRot();
        originalPitch = player.getXRot();
        hasSavedCamera = true;
        
        // 计算面向NPC的基础角度
        Vec3 playerPos = player.position();
        Vec3 npcPos = npc.position();
        
        double dx = npcPos.x - playerPos.x;
        double dz = npcPos.z - playerPos.z;
        
        // 计算朝向NPC的角度
        float toNpcYaw = (float)(Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        
        // 设置初始视角（稍微偏向NPC侧）
        targetYaw = toNpcYaw + GalgameDialogConfig.NPC_VIEW_YAW_OFFSET;
        
        // 计算俯仰角（看向NPC眼睛高度）
        double dy = (npcPos.y + npc.getEyeHeight()) - (playerPos.y + player.getEyeHeight());
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        targetPitch = (float)(-Mth.atan2(dy, horizontalDist) * (180D / Math.PI)) 
                + GalgameDialogConfig.VIEW_PITCH_OFFSET;
        
        // 立即应用视角
        applyCamera(player, targetYaw, targetPitch);
        transitionProgress = 1f;
    }
    
    /**
     * 切换当前说话者
     * 相机会平滑过渡到新的视角
     */
    public void switchSpeaker(Speaker speaker, Player player, LivingEntity npc) {
        if (player == null || npc == null) return;
        
        this.currentSpeaker = speaker;
        this.transitionProgress = 0f;
        
        // 计算面向NPC的基础角度
        Vec3 playerPos = player.position();
        Vec3 npcPos = npc.position();
        
        double dx = npcPos.x - playerPos.x;
        double dz = npcPos.z - playerPos.z;
        float toNpcYaw = (float)(Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        
        // 根据说话者调整视角偏移
        float yawOffset = (speaker == Speaker.NPC) 
                ? GalgameDialogConfig.NPC_VIEW_YAW_OFFSET 
                : GalgameDialogConfig.PLAYER_VIEW_YAW_OFFSET;
        
        targetYaw = toNpcYaw + yawOffset;
    }
    
    /**
     * 更新相机过渡动画
     * 每帧调用以实现平滑过渡
     */
    public void tick(Player player) {
        if (player == null || !hasSavedCamera) return;
        
        if (transitionProgress < 1f) {
            transitionProgress = Math.min(1f, transitionProgress + 0.1f);
            
            float currentYaw = player.getYRot();
            float currentPitch = player.getXRot();
            
            // 使用缓动函数实现平滑过渡
            float easedProgress = easeOutCubic(transitionProgress);
            float newYaw = Mth.lerp(easedProgress, currentYaw, targetYaw);
            float newPitch = Mth.lerp(easedProgress, currentPitch, targetPitch);
            
            applyCamera(player, newYaw, newPitch);
        }
    }
    
    /**
     * 恢复原始视角
     */
    public void restoreCamera() {
        if (!hasSavedCamera) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            applyCamera(mc.player, originalYaw, originalPitch);
        }
        
        hasSavedCamera = false;
        transitionProgress = 0f;
    }
    
    /**
     * 应用相机视角
     */
    private void applyCamera(Player player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -90f, 90f));
        player.yRotO = yaw;
        player.xRotO = player.getXRot();
    }
    
    /**
     * 缓出三次方缓动函数
     */
    private float easeOutCubic(float t) {
        return 1f - (float)Math.pow(1 - t, 3);
    }
    
    public Speaker getCurrentSpeaker() {
        return currentSpeaker;
    }
    
    public boolean isTransitioning() {
        return transitionProgress < 1f;
    }
}
