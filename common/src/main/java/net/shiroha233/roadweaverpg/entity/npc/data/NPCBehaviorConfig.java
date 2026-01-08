package net.shiroha233.roadweaverpg.entity.npc.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * NPC行为配置
 * 职责：表示从数据包加载的NPC行为预设
 * 原理：直接引用TouhouLittleMaid的Task和Sound ID，无需额外配置层
 */
public record NPCBehaviorConfig(
        ResourceLocation id,
        String taskId,           // TouhouLittleMaid的Task ID（如"touhoulittlemaid:idle"）
        int durationTicks,       // 动作持续时间（0=循环）
        @Nullable String soundEventId,  // TouhouLittleMaid的Sound事件ID（如"maid.mode.idle"）
        float soundProbability,  // 语音播放概率（0-1）
        boolean forceSound       // 是否强制播放语音
) {
    
    /**
     * 从JSON解析
     */
    public static NPCBehaviorConfig fromJson(ResourceLocation id, JsonObject json) {
        String taskId = json.has("task_id") 
                ? json.get("task_id").getAsString() 
                : "touhoulittlemaid:idle";
        
        int durationTicks = json.has("duration_ticks") 
                ? json.get("duration_ticks").getAsInt() 
                : 60;
        
        String soundEventId = json.has("sound_event_id") 
                ? json.get("sound_event_id").getAsString() 
                : null;
        
        float soundProbability = json.has("sound_probability") 
                ? json.get("sound_probability").getAsFloat() 
                : 1.0f;
        
        boolean forceSound = json.has("force_sound") 
                && json.get("force_sound").getAsBoolean();
        
        return new NPCBehaviorConfig(id, taskId, durationTicks, 
                soundEventId, soundProbability, forceSound);
    }
    
    /**
     * 是否循环动作
     */
    public boolean isLooping() {
        return durationTicks == 0;
    }
    
    /**
     * 获取Task的ResourceLocation
     */
    public ResourceLocation getTaskResourceLocation() {
        return new ResourceLocation(taskId);
    }
    
    /**
     * 是否有语音
     */
    public boolean hasSound() {
        return soundEventId != null && !soundEventId.isEmpty();
    }
}
