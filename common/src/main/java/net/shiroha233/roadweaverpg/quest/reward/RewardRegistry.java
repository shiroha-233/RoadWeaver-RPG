package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 奖励类型注册表
 */
public final class RewardRegistry {
    
    private static final Map<RewardType, Function<JsonObject, QuestReward>> JSON_DESERIALIZERS 
            = new EnumMap<>(RewardType.class);
    private static final Map<RewardType, Function<FriendlyByteBuf, QuestReward>> NETWORK_DESERIALIZERS 
            = new EnumMap<>(RewardType.class);
    
    private RewardRegistry() {}
    
    static {
        register(RewardType.ITEM, ItemReward::fromJson, ItemReward::fromNetwork);
        register(RewardType.EXPERIENCE, ExperienceReward::fromJson, ExperienceReward::fromNetwork);
        register(RewardType.REPUTATION, ReputationReward::fromJson, ReputationReward::fromNetwork);
        register(RewardType.COIN, CoinReward::fromJson, CoinReward::fromNetwork);
    }
    
    public static void register(RewardType type,
                                 Function<JsonObject, QuestReward> jsonDeserializer,
                                 Function<FriendlyByteBuf, QuestReward> networkDeserializer) {
        JSON_DESERIALIZERS.put(type, jsonDeserializer);
        NETWORK_DESERIALIZERS.put(type, networkDeserializer);
    }
    
    public static QuestReward fromJson(JsonObject json) {
        String typeStr = json.has("type") ? json.get("type").getAsString() : "item";
        RewardType type = RewardType.fromString(typeStr);
        
        Function<JsonObject, QuestReward> deserializer = JSON_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.warn("Unknown reward type: {}", typeStr);
            return null;
        }
        
        return deserializer.apply(json);
    }
    
    public static QuestReward fromNetwork(FriendlyByteBuf buf) {
        RewardType type = buf.readEnum(RewardType.class);
        
        Function<FriendlyByteBuf, QuestReward> deserializer = NETWORK_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.error("Unknown reward type in network: {}", type);
            return null;
        }
        
        return deserializer.apply(buf);
    }
    
    public static void toNetwork(QuestReward reward, FriendlyByteBuf buf) {
        buf.writeEnum(reward.getType());
        reward.toNetwork(buf);
    }
}
