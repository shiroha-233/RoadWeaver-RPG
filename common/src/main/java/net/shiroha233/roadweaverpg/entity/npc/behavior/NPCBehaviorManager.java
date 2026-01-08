package net.shiroha233.roadweaverpg.entity.npc.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.action.NPCActionManager;
import net.shiroha233.roadweaverpg.entity.npc.action.NPCActionType;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceManager;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC行为管理器
 * 职责：协调动作和语音的组合播放
 * 原理：
 * - 门面模式 - 提供统一的接口来协调动作和语音
 * - 预设行为组合，简化调用
 */
public final class NPCBehaviorManager {
    
    private static final NPCBehaviorManager INSTANCE = new NPCBehaviorManager();
    
    private final NPCActionManager actionManager = NPCActionManager.getInstance();
    private final NPCVoiceManager voiceManager = NPCVoiceManager.getInstance();
    
    // 预设行为组合
    private final Map<String, BehaviorPreset> presets = new ConcurrentHashMap<>();
    
    private NPCBehaviorManager() {
        initDefaultPresets();
    }
    
    public static NPCBehaviorManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 初始化默认预设
     */
    private void initDefaultPresets() {
        // 打招呼
        registerPreset("greeting", new BehaviorPreset(
                NPCActionType.GREETING, NPCVoiceType.GREETING, true));
        
        // 告别
        registerPreset("farewell", new BehaviorPreset(
                NPCActionType.BOW, NPCVoiceType.FAREWELL, true));
        
        // 开心
        registerPreset("happy", new BehaviorPreset(
                NPCActionType.HAPPY, NPCVoiceType.HAPPY, true));
        
        // 悲伤
        registerPreset("sad", new BehaviorPreset(
                NPCActionType.SAD, NPCVoiceType.SAD, true));
        
        // 生气
        registerPreset("angry", new BehaviorPreset(
                NPCActionType.ANGRY, NPCVoiceType.ANGRY, true));
        
        // 思考
        registerPreset("thinking", new BehaviorPreset(
                NPCActionType.THINKING, NPCVoiceType.THINKING, false));
        
        // 点头确认
        registerPreset("confirm", new BehaviorPreset(
                NPCActionType.NOD, NPCVoiceType.CONFIRM, true));
        
        // 摇头拒绝
        registerPreset("deny", new BehaviorPreset(
                NPCActionType.SHAKE_HEAD, NPCVoiceType.DENY, true));
        
        // 接受任务
        registerPreset("quest_accept", new BehaviorPreset(
                NPCActionType.NOD, NPCVoiceType.QUEST_ACCEPT, true));
        
        // 完成任务
        registerPreset("quest_complete", new BehaviorPreset(
                NPCActionType.HAPPY, NPCVoiceType.QUEST_COMPLETE, true));
        
        // 商店欢迎
        registerPreset("shop_welcome", new BehaviorPreset(
                NPCActionType.BOW, NPCVoiceType.SHOP_WELCOME, true));
        
        // 购买成功
        registerPreset("shop_buy", new BehaviorPreset(
                NPCActionType.NOD, NPCVoiceType.SHOP_BUY, true));
        
        // 攻击
        registerPreset("attack", new BehaviorPreset(
                NPCActionType.ATTACK, NPCVoiceType.ATTACK, true));
        
        // 拔武器
        registerPreset("draw_weapon", new BehaviorPreset(
                NPCActionType.DRAW_WEAPON, NPCVoiceType.FIND_TARGET, true));
        
        RoadWeaverRPG.LOGGER.info("已注册 {} 个NPC行为预设", presets.size());
    }
    
    /**
     * 注册行为预设
     */
    public void registerPreset(String id, BehaviorPreset preset) {
        presets.put(id, preset);
    }
    
    /**
     * 播放预设行为
     * @param entity NPC实体
     * @param presetId 预设ID
     */
    public void playPreset(EntityMaid entity, String presetId) {
        playPreset(entity, presetId, null);
    }
    
    /**
     * 播放预设行为（带回调）
     * @param entity NPC实体
     * @param presetId 预设ID
     * @param onComplete 完成回调
     */
    public void playPreset(EntityMaid entity, String presetId, Runnable onComplete) {
        BehaviorPreset preset = presets.get(presetId);
        if (preset == null) {
            RoadWeaverRPG.LOGGER.warn("找不到行为预设: {}", presetId);
            return;
        }
        
        playBehavior(entity, preset.actionType, preset.voiceType, preset.forceVoice, onComplete);
    }
    
    /**
     * 播放自定义行为组合
     * @param entity NPC实体
     * @param actionType 动作类型
     * @param voiceType 语音类型（可为null）
     */
    public void playBehavior(EntityMaid entity, NPCActionType actionType, NPCVoiceType voiceType) {
        playBehavior(entity, actionType, voiceType, false, null);
    }
    
    /**
     * 播放自定义行为组合（完整参数）
     */
    public void playBehavior(EntityMaid entity, NPCActionType actionType, 
                             NPCVoiceType voiceType, boolean forceVoice, Runnable onComplete) {
        if (entity == null) return;
        
        // 播放动作
        actionManager.playAction(entity, actionType, onComplete);
        
        // 播放语音
        if (voiceType != null) {
            if (forceVoice) {
                voiceManager.playVoiceForced(entity, voiceType);
            } else {
                voiceManager.playVoice(entity, voiceType);
            }
        }
    }
    
    /**
     * 仅播放动作
     */
    public void playAction(EntityMaid entity, NPCActionType actionType) {
        actionManager.playAction(entity, actionType);
    }
    
    /**
     * 仅播放动作（带回调）
     */
    public void playAction(EntityMaid entity, NPCActionType actionType, Runnable onComplete) {
        actionManager.playAction(entity, actionType, onComplete);
    }
    
    /**
     * 仅播放语音
     */
    public void playVoice(EntityMaid entity, NPCVoiceType voiceType) {
        voiceManager.playVoice(entity, voiceType);
    }
    
    /**
     * 强制播放语音
     */
    public void playVoiceForced(EntityMaid entity, NPCVoiceType voiceType) {
        voiceManager.playVoiceForced(entity, voiceType);
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
    
    /**
     * 行为预设数据
     */
    public record BehaviorPreset(
            NPCActionType actionType,
            NPCVoiceType voiceType,
            boolean forceVoice
    ) {}
}
