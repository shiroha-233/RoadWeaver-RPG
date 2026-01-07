package net.shiroha233.roadweaverpg.client.gui.galgame;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Galgame实体渲染器
 * 职责：在指定屏幕区域渲染实体的正面视图
 * 原理：使用EntityRenderDispatcher在GUI中渲染实体，确保光照一致性
 */
public final class GalgameEntityRenderer {
    
    private GalgameEntityRenderer() {}
    
    // 正在渲染的实体集合（用于禁用名称渲染）
    private static final Set<Integer> renderingEntities = new HashSet<>();
    
    /**
     * 检查实体是否正在被Galgame渲染器渲染（用于外部判断是否跳过名称）
     */
    public static boolean isRenderingEntity(int entityId) {
        return renderingEntities.contains(entityId);
    }
    
    /**
     * 在指定位置渲染实体正面（不渲染名称）
     * @param entity 要渲染的实体
     * @param x 屏幕X坐标（实体中心）
     * @param y 屏幕Y坐标（实体底部）
     * @param scale 缩放比例
     * @param yaw 实体朝向角度
     * @param partialTick 插值tick
     */
    public static void renderEntityFront(LivingEntity entity, int x, int y, float scale, 
                                          float yaw, float partialTick) {
        if (entity == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        
        // 标记实体正在渲染
        renderingEntities.add(entity.getId());
        
        // 保存实体原始状态
        float originalYaw = entity.getYRot();
        float originalYawO = entity.yRotO;
        float originalBodyYaw = entity.yBodyRot;
        float originalBodyYawO = entity.yBodyRotO;
        float originalHeadYaw = entity.yHeadRot;
        float originalHeadYawO = entity.yHeadRotO;
        
        // 保存并临时清除名称
        Component originalCustomName = entity.getCustomName();
        boolean originalCustomNameVisible = entity.isCustomNameVisible();
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
        
        // 设置实体朝向（面向摄像机）
        entity.setYRot(180f + yaw);
        entity.yRotO = 180f + yaw;
        entity.yBodyRot = 180f + yaw;
        entity.yBodyRotO = 180f + yaw;
        entity.yHeadRot = 180f + yaw;
        entity.yHeadRotO = 180f + yaw;
        
        // 设置渲染矩阵
        var poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.translate(x, y, 50.0f);
        poseStack.scale(scale, -scale, scale);
        
        RenderSystem.applyModelViewMatrix();
        
        // 设置固定光照
        Lighting.setupForFlatItems();
        
        // 禁用阴影
        dispatcher.setRenderShadow(false);
        
        // 渲染实体
        var bufferSource = mc.renderBuffers().bufferSource();
        var entityPoseStack = new com.mojang.blaze3d.vertex.PoseStack();
        
        dispatcher.render(entity, 0, 0, 0, 0, partialTick, 
                entityPoseStack, bufferSource, 0xF000F0);
        
        bufferSource.endBatch();
        
        // 恢复设置
        dispatcher.setRenderShadow(true);
        
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        
        // 恢复实体原始状态
        entity.setYRot(originalYaw);
        entity.yRotO = originalYawO;
        entity.yBodyRot = originalBodyYaw;
        entity.yBodyRotO = originalBodyYawO;
        entity.yHeadRot = originalHeadYaw;
        entity.yHeadRotO = originalHeadYawO;
        entity.setCustomName(originalCustomName);
        entity.setCustomNameVisible(originalCustomNameVisible);
        
        // 移除渲染标记
        renderingEntities.remove(entity.getId());
    }
    
    /**
     * 计算实体渲染的合适缩放比例
     * @param entity 实体
     * @param viewportHeight 视口高度
     * @return 缩放比例
     */
    public static float calculateEntityScale(LivingEntity entity, int viewportHeight) {
        if (entity == null) return 1.0f;
        
        float entityHeight = entity.getBbHeight();
        // 让实体占据视口高度的70%
        float targetHeight = viewportHeight * 0.7f;
        return targetHeight / entityHeight;
    }
}
