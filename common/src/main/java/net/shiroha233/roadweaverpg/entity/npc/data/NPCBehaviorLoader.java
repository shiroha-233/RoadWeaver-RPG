package net.shiroha233.roadweaverpg.entity.npc.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC行为数据包加载器
 * 职责：从数据包加载NPC行为预设配置
 * 原理：
 * - 使用SimpleJsonResourceReloadListener实现资源重载
 * - 线程安全的数据存储
 * - 支持数据包覆盖和扩展
 * 
 * 数据包路径：data/<namespace>/npc_behaviors/<id>.json
 * 
 * 配置格式：
 * {
 *   "task_id": "touhoulittlemaid:idle",      // TouhouLittleMaid的Task ID
 *   "duration_ticks": 60,                     // 持续时间（0=循环）
 *   "sound_event_id": "maid.mode.idle",       // TouhouLittleMaid的Sound事件ID（可选）
 *   "sound_probability": 1.0,                 // 语音播放概率（0-1）
 *   "force_sound": true                       // 是否强制播放语音
 * }
 */
public class NPCBehaviorLoader extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final NPCBehaviorLoader INSTANCE = new NPCBehaviorLoader();
    
    // 行为配置缓存
    private final Map<ResourceLocation, NPCBehaviorConfig> behaviors = new ConcurrentHashMap<>();
    
    private NPCBehaviorLoader() {
        super(GSON, "npc_behaviors");
    }
    
    public static NPCBehaviorLoader getInstance() {
        return INSTANCE;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, 
                         ProfilerFiller profiler) {
        behaviors.clear();
        
        int count = 0;
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation id = entry.getKey();
            
            try {
                if (!entry.getValue().isJsonObject()) {
                    RoadWeaverRPG.LOGGER.warn("NPC行为数据必须是JSON对象: {}", id);
                    continue;
                }
                
                JsonObject json = entry.getValue().getAsJsonObject();
                NPCBehaviorConfig config = NPCBehaviorConfig.fromJson(id, json);
                behaviors.put(id, config);
                count++;
                
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("加载NPC行为数据失败: {}", id, e);
            }
        }
        
        RoadWeaverRPG.LOGGER.info("已加载 {} 个NPC行为配置", count);
    }
    
    /**
     * 获取行为配置
     */
    public NPCBehaviorConfig getBehavior(ResourceLocation id) {
        return behaviors.get(id);
    }
    
    /**
     * 获取所有行为配置
     */
    public Map<ResourceLocation, NPCBehaviorConfig> getAllBehaviors() {
        return Map.copyOf(behaviors);
    }
}
