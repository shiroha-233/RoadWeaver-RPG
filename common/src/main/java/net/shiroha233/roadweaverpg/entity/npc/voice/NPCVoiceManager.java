package net.shiroha233.roadweaverpg.entity.npc.voice;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC语音管理器
 * 职责：管理NPC的语音播放
 * 原理：
 * - 单例模式确保全局唯一
 * - 使用TouhouLittleMaid的Sound系统播放语音
 * - 支持语音冷却防止频繁播放
 * - 线程安全的状态管理
 */
public final class NPCVoiceManager {
    
    private static final NPCVoiceManager INSTANCE = new NPCVoiceManager();
    
    // 语音冷却时间（tick）
    private static final int DEFAULT_COOLDOWN = 40;
    
    // NPC语音冷却状态 (entityId -> lastPlayTime)
    private final Map<Integer, Long> voiceCooldowns = new ConcurrentHashMap<>();
    
    // NPC语音启用状态 (entityId -> enabled)
    private final Map<Integer, Boolean> voiceEnabled = new ConcurrentHashMap<>();
    
    // 随机数生成器
    private final RandomSource random = RandomSource.create();
    
    private NPCVoiceManager() {}
    
    public static NPCVoiceManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 播放NPC语音（根据概率）
     * @param entity NPC实体
     * @param voiceType 语音类型
     * @return 是否成功播放
     */
    public boolean playVoice(EntityMaid entity, NPCVoiceType voiceType) {
        if (entity == null) return false;
        
        // 检查是否启用语音
        if (!isVoiceEnabled(entity.getId())) {
            return false;
        }
        
        // 检查概率
        if (random.nextFloat() > voiceType.getProbability()) {
            return false;
        }
        
        return playVoiceInternal(entity, voiceType);
    }
    
    /**
     * 强制播放NPC语音（忽略概率）
     * @param entity NPC实体
     * @param voiceType 语音类型
     * @return 是否成功播放
     */
    public boolean playVoiceForced(EntityMaid entity, NPCVoiceType voiceType) {
        if (entity == null) return false;
        
        // 检查是否启用语音
        if (!isVoiceEnabled(entity.getId())) {
            return false;
        }
        
        return playVoiceInternal(entity, voiceType);
    }
    
    /**
     * 内部播放逻辑
     */
    private boolean playVoiceInternal(EntityMaid entity, NPCVoiceType voiceType) {
        int entityId = entity.getId();
        Level level = entity.level();
        
        // 检查冷却
        if (isOnCooldown(entityId, level.getGameTime())) {
            return false;
        }
        
        // 获取对应的SoundEvent
        SoundEvent soundEvent = getSoundEvent(voiceType);
        if (soundEvent == null) {
            RoadWeaverRPG.LOGGER.warn("找不到语音事件: {}", voiceType.getSoundEventId());
            return false;
        }
        
        // 播放声音
        float volume = 1.0f;
        float pitch = 0.9f + random.nextFloat() * 0.2f; // 随机音调变化
        
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
        
        // 设置冷却
        voiceCooldowns.put(entityId, level.getGameTime());
        
        RoadWeaverRPG.LOGGER.debug("NPC {} 播放语音: {}", entityId, voiceType.getId());
        return true;
    }
    
    /**
     * 检查是否在冷却中
     */
    private boolean isOnCooldown(int entityId, long currentTime) {
        Long lastPlayTime = voiceCooldowns.get(entityId);
        if (lastPlayTime == null) {
            return false;
        }
        return (currentTime - lastPlayTime) < DEFAULT_COOLDOWN;
    }
    
    /**
     * 获取SoundEvent
     */
    private SoundEvent getSoundEvent(NPCVoiceType voiceType) {
        // 映射到TouhouLittleMaid的声音事件
        return switch (voiceType.getSoundEventId()) {
            case "maid.mode.idle" -> InitSounds.MAID_IDLE.get();
            case "maid.mode.attack" -> InitSounds.MAID_ATTACK.get();
            case "maid.ai.hurt" -> InitSounds.MAID_HURT.get();
            case "maid.ai.death" -> InitSounds.MAID_DEATH.get();
            case "maid.ai.find_target" -> InitSounds.MAID_FIND_TARGET.get();
            case "maid.ai.item_get" -> InitSounds.MAID_ITEM_GET.get();
            case "maid.ai.game_win" -> InitSounds.GAME_WIN.get();
            case "maid.ai.game_lost" -> InitSounds.GAME_LOST.get();
            case "maid.environment.morning" -> InitSounds.MAID_MORNING.get();
            case "maid.environment.night" -> InitSounds.MAID_NIGHT.get();
            case "maid.environment.rain" -> InitSounds.MAID_RAIN.get();
            case "maid.environment.cold" -> InitSounds.MAID_COLD.get();
            case "maid.environment.hot" -> InitSounds.MAID_HOT.get();
            default -> InitSounds.MAID_IDLE.get();
        };
    }
    
    /**
     * 是否启用语音
     */
    public boolean isVoiceEnabled(int entityId) {
        return voiceEnabled.getOrDefault(entityId, true);
    }
    
    /**
     * 设置是否启用语音
     */
    public void setVoiceEnabled(int entityId, boolean enabled) {
        voiceEnabled.put(entityId, enabled);
    }
    
    /**
     * 清理实体的语音状态
     */
    public void cleanup(int entityId) {
        voiceCooldowns.remove(entityId);
        voiceEnabled.remove(entityId);
    }
    
    /**
     * 清理过期的冷却数据（防止内存泄漏）
     */
    public void cleanupExpiredCooldowns(long currentTime) {
        voiceCooldowns.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > DEFAULT_COOLDOWN * 10);
    }
}
