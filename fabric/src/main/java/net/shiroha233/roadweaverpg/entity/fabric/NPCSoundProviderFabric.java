package net.shiroha233.roadweaverpg.entity.fabric;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fabric平台NPC声音事件提供者
 * 职责：为common模块提供TouhouLittleMaid的声音事件
 * 原理：通过ResourceLocation从注册表获取声音事件
 */
public final class NPCSoundProviderFabric {
    
    private static final String TLM_MOD_ID = "touhoulittlemaid";
    private static Map<String, SoundEvent> soundEventCache;
    
    private NPCSoundProviderFabric() {}
    
    /**
     * 初始化声音事件提供者
     */
    public static void init() {
        NPCVoiceManager.setSoundEventProvider(NPCSoundProviderFabric::getSoundEvents);
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
     * 构建声音事件映射（通过注册表查找）
     */
    private static Map<String, SoundEvent> buildSoundEventMap() {
        Map<String, SoundEvent> map = new HashMap<>();
        
        // 模式相关
        putSoundEvent(map, "maid.mode.idle");
        putSoundEvent(map, "maid.mode.attack");
        putSoundEvent(map, "maid.mode.range_attack");
        putSoundEvent(map, "maid.mode.farm");
        putSoundEvent(map, "maid.mode.feed");
        putSoundEvent(map, "maid.mode.shears");
        putSoundEvent(map, "maid.mode.milk");
        putSoundEvent(map, "maid.mode.torch");
        
        // AI相关
        putSoundEvent(map, "maid.ai.find_target");
        putSoundEvent(map, "maid.ai.hurt");
        putSoundEvent(map, "maid.ai.hurt_fire");
        putSoundEvent(map, "maid.ai.hurt_player");
        putSoundEvent(map, "maid.ai.tamed");
        putSoundEvent(map, "maid.ai.item_get");
        putSoundEvent(map, "maid.ai.death");
        putSoundEvent(map, "maid.ai.game_win");
        putSoundEvent(map, "maid.ai.game_lost");
        
        // 环境相关
        putSoundEvent(map, "maid.environment.hot");
        putSoundEvent(map, "maid.environment.cold");
        putSoundEvent(map, "maid.environment.rain");
        putSoundEvent(map, "maid.environment.snow");
        putSoundEvent(map, "maid.environment.morning");
        putSoundEvent(map, "maid.environment.night");
        
        return map;
    }
    
    /**
     * 从注册表获取声音事件并放入映射
     */
    private static void putSoundEvent(Map<String, SoundEvent> map, String soundId) {
        ResourceLocation location = new ResourceLocation(TLM_MOD_ID, soundId);
        Optional<SoundEvent> soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(location);
        
        if (soundEvent.isPresent()) {
            map.put(soundId, soundEvent.get());
        } else {
            // 如果注册表中没有，创建一个固定范围的声音事件
            SoundEvent created = SoundEvent.createFixedRangeEvent(location, 16.0f);
            map.put(soundId, created);
            RoadWeaverRPG.LOGGER.debug("创建声音事件: {}", location);
        }
    }
}
