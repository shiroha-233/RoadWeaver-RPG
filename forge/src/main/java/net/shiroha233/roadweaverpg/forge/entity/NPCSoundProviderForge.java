package net.shiroha233.roadweaverpg.forge.entity;

import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.minecraft.sounds.SoundEvent;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Forge平台NPC声音事件提供者
 * 职责：为common模块提供TouhouLittleMaid的声音事件
 * 原理：依赖倒置 - common模块依赖抽象，平台模块提供具体实现
 */
public final class NPCSoundProviderForge {
    
    private static Map<String, SoundEvent> soundEventCache;
    
    private NPCSoundProviderForge() {}
    
    /**
     * 初始化声音事件提供者
     */
    public static void init() {
        NPCVoiceManager.setSoundEventProvider(NPCSoundProviderForge::getSoundEvents);
    }
    
    /**
     * 获取声音事件映射
     */
    private static Map<String, SoundEvent> getSoundEvents() {
        if (soundEventCache == null) {
            soundEventCache = buildSoundEventMap();
        }
        return soundEventCache;
    }
    
    /**
     * 构建声音事件映射
     */
    private static Map<String, SoundEvent> buildSoundEventMap() {
        Map<String, SoundEvent> map = new HashMap<>();
        
        // 模式相关
        map.put("maid.mode.idle", InitSounds.MAID_IDLE.get());
        map.put("maid.mode.attack", InitSounds.MAID_ATTACK.get());
        map.put("maid.mode.range_attack", InitSounds.MAID_RANGE_ATTACK.get());
        map.put("maid.mode.farm", InitSounds.MAID_FARM.get());
        map.put("maid.mode.feed", InitSounds.MAID_FEED.get());
        map.put("maid.mode.shears", InitSounds.MAID_SHEARS.get());
        map.put("maid.mode.milk", InitSounds.MAID_MILK.get());
        map.put("maid.mode.torch", InitSounds.MAID_TORCH.get());
        
        // AI相关
        map.put("maid.ai.find_target", InitSounds.MAID_FIND_TARGET.get());
        map.put("maid.ai.hurt", InitSounds.MAID_HURT.get());
        map.put("maid.ai.hurt_fire", InitSounds.MAID_HURT_FIRE.get());
        map.put("maid.ai.hurt_player", InitSounds.MAID_PLAYER.get());
        map.put("maid.ai.tamed", InitSounds.MAID_TAMED.get());
        map.put("maid.ai.item_get", InitSounds.MAID_ITEM_GET.get());
        map.put("maid.ai.death", InitSounds.MAID_DEATH.get());
        map.put("maid.ai.game_win", InitSounds.GAME_WIN.get());
        map.put("maid.ai.game_lost", InitSounds.GAME_LOST.get());
        
        // 环境相关
        map.put("maid.environment.hot", InitSounds.MAID_HOT.get());
        map.put("maid.environment.cold", InitSounds.MAID_COLD.get());
        map.put("maid.environment.rain", InitSounds.MAID_RAIN.get());
        map.put("maid.environment.snow", InitSounds.MAID_SNOW.get());
        map.put("maid.environment.morning", InitSounds.MAID_MORNING.get());
        map.put("maid.environment.night", InitSounds.MAID_NIGHT.get());
        
        return map;
    }
}
