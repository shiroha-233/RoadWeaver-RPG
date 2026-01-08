package net.shiroha233.roadweaverpg.entity.npc.voice;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * NPC语音类型枚举
 * 职责：定义所有可用的NPC语音类型
 * 原理：基于TouhouLittleMaid的Sound系统，映射到对应的声音事件
 */
public enum NPCVoiceType {
    
    // 基础语音
    IDLE("idle", "maid.mode.idle", 0.3f),
    
    // 交互语音
    GREETING("greeting", "maid.mode.idle", 1.0f),           // 打招呼
    FAREWELL("farewell", "maid.mode.idle", 1.0f),           // 告别
    THANKS("thanks", "maid.mode.idle", 1.0f),               // 感谢
    CONFIRM("confirm", "maid.mode.idle", 1.0f),             // 确认
    DENY("deny", "maid.mode.idle", 1.0f),                   // 拒绝
    
    // 对话语音
    DIALOG_START("dialog_start", "maid.mode.idle", 1.0f),   // 对话开始
    DIALOG_CONTINUE("dialog_continue", "maid.mode.idle", 0.8f), // 对话继续
    DIALOG_END("dialog_end", "maid.mode.idle", 1.0f),       // 对话结束
    QUESTION("question", "maid.mode.idle", 1.0f),           // 提问
    
    // 情感语音
    HAPPY("happy", "maid.mode.idle", 1.0f),                 // 开心
    SAD("sad", "maid.ai.hurt", 1.0f),                       // 悲伤
    ANGRY("angry", "maid.ai.find_target", 1.0f),            // 生气
    SURPRISED("surprised", "maid.mode.idle", 1.0f),         // 惊讶
    THINKING("thinking", "maid.mode.idle", 0.5f),           // 思考
    
    // 任务相关
    QUEST_ACCEPT("quest_accept", "maid.mode.idle", 1.0f),   // 接受任务
    QUEST_COMPLETE("quest_complete", "maid.ai.game_win", 1.0f), // 完成任务
    QUEST_FAIL("quest_fail", "maid.ai.game_lost", 1.0f),    // 任务失败
    
    // 商店相关
    SHOP_WELCOME("shop_welcome", "maid.mode.idle", 1.0f),   // 商店欢迎
    SHOP_BUY("shop_buy", "maid.ai.item_get", 1.0f),         // 购买
    SHOP_SELL("shop_sell", "maid.mode.idle", 1.0f),         // 出售
    SHOP_INSUFFICIENT("shop_insufficient", "maid.mode.idle", 1.0f), // 金币不足
    
    // 战斗相关
    ATTACK("attack", "maid.mode.attack", 1.0f),             // 攻击
    HURT("hurt", "maid.ai.hurt", 1.0f),                     // 受伤
    DEATH("death", "maid.ai.death", 1.0f),                  // 死亡
    FIND_TARGET("find_target", "maid.ai.find_target", 1.0f), // 发现目标
    
    // 环境相关
    MORNING("morning", "maid.environment.morning", 0.5f),   // 早上
    NIGHT("night", "maid.environment.night", 0.5f),         // 晚上
    RAIN("rain", "maid.environment.rain", 0.3f),            // 下雨
    COLD("cold", "maid.environment.cold", 0.3f),            // 寒冷
    HOT("hot", "maid.environment.hot", 0.3f);               // 炎热
    
    private final String id;
    private final String soundEventId;  // 对应TouhouLittleMaid的Sound事件
    private final float probability;     // 播放概率（0-1）
    
    NPCVoiceType(String id, String soundEventId, float probability) {
        this.id = id;
        this.soundEventId = soundEventId;
        this.probability = probability;
    }
    
    public String getId() {
        return id;
    }
    
    public ResourceLocation getResourceLocation() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "voice/" + id);
    }
    
    public String getSoundEventId() {
        return soundEventId;
    }
    
    public ResourceLocation getSoundEventResourceLocation() {
        return new ResourceLocation("touhoulittlemaid", soundEventId);
    }
    
    public float getProbability() {
        return probability;
    }
    
    /**
     * 根据ID查找语音类型
     */
    public static NPCVoiceType fromId(String id) {
        for (NPCVoiceType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return IDLE;
    }
}
