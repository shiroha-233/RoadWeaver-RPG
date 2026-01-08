package net.shiroha233.roadweaverpg.entity.npc.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.action.NPCActionManager;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorConfig;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorLoader;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceManager;

/**
 * NPC行为管理器
 * 职责：协调动作和语音的组合播放
 * 原理：
 * - 门面模式 - 提供统一的接口来协调动作和语音
 * - 数据包驱动：从NPCBehaviorLoader加载配置
 */
public final class NPCBehaviorManager {
    
    private static final NPCBehaviorManager INSTANCE = new NPCBehaviorManager();
    
    private final NPCActionManager actionManager = NPCActionManager.getInstance();
    private final NPCVoiceManager voiceManager = NPCVoiceManager.getInstance();
    
    private NPCBehaviorManager() {}
    
    public static NPCBehaviorManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 播放行为（从数据包加载）
     * @param entity NPC实体
     * @param behaviorId 行为ID
     */
    public void playPreset(EntityMaid entity, ResourceLocation behaviorId) {
        playPreset(entity, behaviorId, null);
    }
    
    /**
     * 播放行为（带回调）
     * @param entity NPC实体
     * @param behaviorId 行为ID
     * @param onComplete 完成回调
     */
    public void playPreset(EntityMaid entity, ResourceLocation behaviorId, Runnable onComplete) {
        if (entity == null) return;
        
        NPCBehaviorConfig config = NPCBehaviorLoader.getInstance().getBehavior(behaviorId);
        if (config == null) {
            RoadWeaverRPG.LOGGER.warn("找不到行为配置: {}", behaviorId);
            return;
        }
        
        // 播放动作
        actionManager.playAction(entity, behaviorId, onComplete);
        
        // 播放语音
        if (config.hasSound()) {
            if (config.forceSound()) {
                voiceManager.playVoiceForced(entity, behaviorId);
            } else {
                voiceManager.playVoice(entity, behaviorId);
            }
        }
    }
    
    /**
     * 仅播放动作
     */
    public void playAction(EntityMaid entity, ResourceLocation behaviorId) {
        actionManager.playAction(entity, behaviorId);
    }
    
    /**
     * 仅播放动作（带回调）
     */
    public void playAction(EntityMaid entity, ResourceLocation behaviorId, Runnable onComplete) {
        actionManager.playAction(entity, behaviorId, onComplete);
    }
    
    /**
     * 仅播放语音
     */
    public void playVoice(EntityMaid entity, ResourceLocation behaviorId) {
        voiceManager.playVoice(entity, behaviorId);
    }
    
    /**
     * 强制播放语音
     */
    public void playVoiceForced(EntityMaid entity, ResourceLocation behaviorId) {
        voiceManager.playVoiceForced(entity, behaviorId);
    }
    
    /**
     * 停止所有行为
     */
    public void stopAll(EntityMaid entity) {
        actionManager.stopAction(entity);
    }
    
    /**
     * 更新（每tick调用）
     */
    public void tick(EntityMaid entity) {
        actionManager.tick(entity);
    }
    
    /**
     * 清理实体状态
     */
    public void cleanup(int entityId) {
        actionManager.cleanup(entityId);
        voiceManager.cleanup(entityId);
    }
    
    /**
     * 获取动作管理器
     */
    public NPCActionManager getActionManager() {
        return actionManager;
    }
    
    /**
     * 获取语音管理器
     */
    public NPCVoiceManager getVoiceManager() {
        return voiceManager;
    }
}
