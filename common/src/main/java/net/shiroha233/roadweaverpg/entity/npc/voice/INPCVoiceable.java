package net.shiroha233.roadweaverpg.entity.npc.voice;

/**
 * 可发声NPC接口
 * 职责：定义NPC语音相关的行为
 * 原理：接口隔离原则 - 只有需要语音功能的NPC才实现此接口
 */
public interface INPCVoiceable {
    
    /**
     * 播放语音
     * @param voiceType 语音类型
     */
    void playVoice(NPCVoiceType voiceType);
    
    /**
     * 播放语音（强制播放，忽略概率）
     * @param voiceType 语音类型
     */
    void playVoiceForced(NPCVoiceType voiceType);
    
    /**
     * 停止当前语音
     */
    void stopVoice();
    
    /**
     * 是否正在播放语音
     */
    boolean isPlayingVoice();
    
    /**
     * 获取语音包ID
     */
    String getSoundPackId();
    
    /**
     * 设置语音包ID
     */
    void setSoundPackId(String soundPackId);
    
    /**
     * 是否启用语音
     */
    boolean isVoiceEnabled();
    
    /**
     * 设置是否启用语音
     */
    void setVoiceEnabled(boolean enabled);
}
