package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 目标类型注册表
 * 
 * 设计原理：
 * - 工厂模式：统一管理目标的创建
 * - 开闭原则：支持扩展新目标类型
 * - 线程安全：使用ConcurrentHashMap
 * 
 * V2重构改进：
 * - 支持自定义目标类型注册
 * - 添加目标类型验证
 * - 统一的错误处理
 */
public final class ObjectiveRegistry {
    
    private static final Map<QuestType, BiFunction<String, JsonObject, QuestObjective>> JSON_DESERIALIZERS 
            = new ConcurrentHashMap<>(new EnumMap<>(QuestType.class));
    private static final Map<QuestType, Function<FriendlyByteBuf, QuestObjective>> NETWORK_DESERIALIZERS 
            = new ConcurrentHashMap<>(new EnumMap<>(QuestType.class));
    
    // 目标类型到事件类型的映射
    private static final Map<QuestType, String> EVENT_TYPE_MAPPING = new ConcurrentHashMap<>();
    
    private static volatile boolean initialized = false;
    
    private ObjectiveRegistry() {}
    
    /**
     * 初始化注册表（线程安全）
     */
    public static synchronized void init() {
        if (initialized) return;
        
        // 注册内置目标类型
        register(QuestType.COLLECT, CollectObjective::fromJson, CollectObjective::fromNetwork, "inventory_check");
        register(QuestType.KILL, KillObjective::fromJson, KillObjective::fromNetwork, "entity_kill");
        register(QuestType.LOCATION_KILL, LocationKillObjective::fromJson, LocationKillObjective::fromNetwork, "entity_kill");
        register(QuestType.EXPLORE, ExploreObjective::fromJson, ExploreObjective::fromNetwork, "player_move");
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("目标注册表初始化完成，已注册 {} 个目标类型", JSON_DESERIALIZERS.size());
    }
    
    /**
     * 注册目标类型（完整版）
     */
    public static void register(QuestType type, 
                                 BiFunction<String, JsonObject, QuestObjective> jsonDeserializer,
                                 Function<FriendlyByteBuf, QuestObjective> networkDeserializer,
                                 String eventType) {
        JSON_DESERIALIZERS.put(type, jsonDeserializer);
        NETWORK_DESERIALIZERS.put(type, networkDeserializer);
        EVENT_TYPE_MAPPING.put(type, eventType);
    }
    
    /**
     * 注册目标类型（简化版，兼容旧代码）
     */
    public static void register(QuestType type, 
                                 BiFunction<String, JsonObject, QuestObjective> jsonDeserializer,
                                 Function<FriendlyByteBuf, QuestObjective> networkDeserializer) {
        JSON_DESERIALIZERS.put(type, jsonDeserializer);
        NETWORK_DESERIALIZERS.put(type, networkDeserializer);
    }
    
    /**
     * 从JSON解析目标
     */
    public static QuestObjective fromJson(String id, JsonObject json) {
        String typeStr = json.has("type") ? json.get("type").getAsString() : "collect";
        QuestType type = QuestType.fromString(typeStr);
        
        BiFunction<String, JsonObject, QuestObjective> deserializer = JSON_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.warn("未知目标类型: {}，使用默认收集类型", typeStr);
            deserializer = JSON_DESERIALIZERS.get(QuestType.COLLECT);
        }
        
        try {
            return deserializer.apply(id, json);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("解析目标失败: {} - {}", id, e.getMessage());
            return null;
        }
    }
    
    /**
     * 从网络数据解析目标
     */
    public static QuestObjective fromNetwork(FriendlyByteBuf buf) {
        QuestType type = buf.readEnum(QuestType.class);
        
        Function<FriendlyByteBuf, QuestObjective> deserializer = NETWORK_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.error("网络数据中存在未知目标类型: {}", type);
            return null;
        }
        
        try {
            return deserializer.apply(buf);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("从网络解析目标失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 序列化目标到网络
     */
    public static void toNetwork(QuestObjective objective, FriendlyByteBuf buf) {
        buf.writeEnum(objective.getType());
        objective.toNetwork(buf);
    }
    
    /**
     * 获取目标类型对应的事件类型
     */
    public static String getEventType(QuestType type) {
        return EVENT_TYPE_MAPPING.getOrDefault(type, "unknown");
    }
    
    /**
     * 检查目标类型是否已注册
     */
    public static boolean isRegistered(QuestType type) {
        return JSON_DESERIALIZERS.containsKey(type);
    }
}
