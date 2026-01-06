package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 目标类型注册表
 */
public final class ObjectiveRegistry {
    
    private static final Map<QuestType, BiFunction<String, JsonObject, QuestObjective>> JSON_DESERIALIZERS 
            = new EnumMap<>(QuestType.class);
    private static final Map<QuestType, Function<FriendlyByteBuf, QuestObjective>> NETWORK_DESERIALIZERS 
            = new EnumMap<>(QuestType.class);
    
    private ObjectiveRegistry() {}
    
    static {
        register(QuestType.COLLECT, CollectObjective::fromJson, CollectObjective::fromNetwork);
        register(QuestType.KILL, KillObjective::fromJson, KillObjective::fromNetwork);
        register(QuestType.EXPLORE, ExploreObjective::fromJson, ExploreObjective::fromNetwork);
    }
    
    public static void register(QuestType type, 
                                 BiFunction<String, JsonObject, QuestObjective> jsonDeserializer,
                                 Function<FriendlyByteBuf, QuestObjective> networkDeserializer) {
        JSON_DESERIALIZERS.put(type, jsonDeserializer);
        NETWORK_DESERIALIZERS.put(type, networkDeserializer);
    }
    
    public static QuestObjective fromJson(String id, JsonObject json) {
        String typeStr = json.has("type") ? json.get("type").getAsString() : "collect";
        QuestType type = QuestType.fromString(typeStr);
        
        BiFunction<String, JsonObject, QuestObjective> deserializer = JSON_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.warn("Unknown objective type: {}, defaulting to collect", typeStr);
            deserializer = JSON_DESERIALIZERS.get(QuestType.COLLECT);
        }
        
        return deserializer.apply(id, json);
    }
    
    public static QuestObjective fromNetwork(FriendlyByteBuf buf) {
        QuestType type = buf.readEnum(QuestType.class);
        
        Function<FriendlyByteBuf, QuestObjective> deserializer = NETWORK_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.error("Unknown objective type in network: {}", type);
            return null;
        }
        
        return deserializer.apply(buf);
    }
    
    public static void toNetwork(QuestObjective objective, FriendlyByteBuf buf) {
        buf.writeEnum(objective.getType());
        objective.toNetwork(buf);
    }
}
