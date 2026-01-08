package net.shiroha233.roadweaverpg.entity.npc.action;

import net.minecraft.resources.ResourceLocation;

/**
 * 可动画NPC接口
 * 职责：定义NPC动画相关的行为
 * 原理：接口隔离原则 - 只有需要动画功能的NPC才实现此接口
 */
public interface INPCAnimatable {
    
    /**
     * 播放动作
     * @param actionId 动作ID（从数据包加载）
     */
    void playAction(ResourceLocation actionId);
    
    /**
     * 播放动作（带回调）
     * @param actionId 动作ID
     * @param onComplete 动作完成回调
     */
    void playAction(ResourceLocation actionId, Runnable onComplete);
    
    /**
     * 停止当前动作，恢复空闲状态
     */
    void stopAction();
    
    /**
     * 获取当前动作ID
     */
    ResourceLocation getCurrentAction();
    
    /**
     * 是否正在播放动作
     */
    boolean isPlayingAction();
    
    /**
     * 获取当前动作剩余时间（tick）
     */
    int getActionRemainingTicks();
}
