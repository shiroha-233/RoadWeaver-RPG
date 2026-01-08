package net.shiroha233.roadweaverpg.entity.npc.voice;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorConfig;
import net.shiroha233.roadweaverpg.entity.npc.data.NPCBehaviorLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * NPC语音管理器
 * 职责：管理NPC的语音播放
 * 原理：
 * - 单例模式确保全局唯一
 * - 使用平台注入的声音事件提供者
 * - 支持语音冷却防止频繁播放
 * - 线程安全的状态管理
 * - 数据包驱动：从NPCBehaviorLoader加载语音配置
 */
public final class NPCVoiceManager {
    
    private static final NPCVoiceManager INSTANCE = new NPCVoiceManager();
    
    // NPC语音冷却状态 (entityId -> lastPlayTime)
    private final Map<Integer, Long> voiceCooldowns = new ConcurrentHashMap<>();
    
    // NPC语音启用状态 (entityId -> enabled)
    private final Map<Integer, Boolean> voiceEnabled = new ConcurrentHashMap<>();
    
    // 声音事件提供者（由平台注入）
    private static Supplier<Map<String, SoundEvent>> soundEventProvider;
    
    // 随机数生成器
    private final RandomSource random = RandomSource.create();
    
    private NPCVoiceManager() {}
    
    public static NPCVoiceManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 设置声音事件提供者（由平台特定代码调用）
     */
    public static void setSoundEventProvider(Supplier<Map<String, SoundEvent>> provider) {
        soundEventProvider = provider;
        RoadWeaverRPG.LOGGER.info("NPC语音管理器声音事件提供者已设置");
    }
    
    /**
     * 播放NPC语音（根据概率）
     * @param entity NPC实体
     * @param behaviorId 行为ID（从数据包加载）
     */
    public boolean playVoice(EntityMaid entity, ResourceLocation behaviorId) {
        if (entity == null) return false;
        
        if (!isVoiceEnabled(entity.getId())) {
            return false;
        }
        
        // 从数据包加载行为配置
        NPCBehaviorConfig config = NPCBehaviorLoader.getInstance().getBehavior(behaviorId);
        if (config == null || !config.hasSound()) {
            return false;
        }
        
        if (random.nextFloat() > config.soundProbability()) {
            return false;
        }
        
        return playVoiceInternal(entity, behaviorId, config);
    }
    
    /**
     * 强制播放NPC语音（忽略概率）
     */
    public boolean playVoiceForced(EntityMaid entity, ResourceLocation behaviorId) {
        if (entity == null) return false;
        
        if (!isVoiceEnabled(entity.getId())) {
            return false;
        }
        
        // 从数据包加载行为配置
        NPCBehaviorConfig config = NPCBehaviorLoader.getInstance().getBehavior(behaviorId);
        if (config == null || !config.hasSound()) {
            return false;
        }
        
        return playVoiceInternal(entity, behaviorId, config);
    }
    
    /**
     * 内部播放逻辑
     */
    private boolean playVoiceInternal(EntityMaid entity, ResourceLocation behaviorId, NPCBehaviorConfig config) {
        int entityId = entity.getId();
        Level level = entity.level();
        
        // 使用固定冷却时间40 ticks
        if (isOnCooldown(entityId, level.getGameTime(), 40)) {
            return false;
        }
        
        SoundEvent soundEvent = getSoundEvent(config.soundEventId());
        if (soundEvent == null) {
            RoadWeaverRPG.LOGGER.debug("找不到语音事件: {}", config.soundEventId());
            return false;
        }
        
        float volume = 1.0f;
        float pitch = 0.9f + random.nextFloat() * 0.2f;
        
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                soundEvent,
                SoundSource.NEUTRAL,
                volume,
                pitch
        );
        
        voiceCooldowns.put(entityId, level.getGameTime());
        
        RoadWeaverRPG.LOGGER.debug("NPC {} 播放语音: {}", entityId, behaviorId);
        return true;
    }
    
    private boolean isOnCooldown(int entityId, long currentTime, int cooldownTicks) {
        Long lastPlayTime = voiceCooldowns.get(entityId);
        if (lastPlayTime == null) {
            return false;
        }
        return (currentTime - lastPlayTime) < cooldownTicks;
    }
    
    /**
     * 获取SoundEvent（通过平台注入的提供者）
     */
    private SoundEvent getSoundEvent(String soundEventId) {
        if (soundEventProvider == null) {
            RoadWeaverRPG.LOGGER.warn("声音事件提供者未设置");
            return null;
        }
        
        Map<String, SoundEvent> soundEvents = soundEventProvider.get();
        if (soundEvents == null) {
            return null;
        }
        
        return soundEvents.get(soundEventId);
    }
    
    public boolean isVoiceEnabled(int entityId) {
        return voiceEnabled.getOrDefault(entityId, true);
    }
    
    public void setVoiceEnabled(int entityId, boolean enabled) {
        voiceEnabled.put(entityId, enabled);
    }
    
    public void cleanup(int entityId) {
        voiceCooldowns.remove(entityId);
        voiceEnabled.remove(entityId);
    }
    
    public void cleanupExpiredCooldowns(long currentTime, int maxCooldown) {
        voiceCooldowns.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > maxCooldown * 10);
    }
}
