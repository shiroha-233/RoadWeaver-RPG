package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 奖励类型注册表
 * 
 * 改进：添加 NBT 序列化支持，用于持久化待发放奖励
 */
public final class RewardRegistry {
    
    private static final Map<RewardType, Function<JsonObject, QuestReward>> JSON_DESERIALIZERS 
            = new EnumMap<>(RewardType.class);
    private static final Map<RewardType, Function<FriendlyByteBuf, QuestReward>> NETWORK_DESERIALIZERS 
            = new EnumMap<>(RewardType.class);
    private static final Map<RewardType, Function<CompoundTag, QuestReward>> NBT_DESERIALIZERS 
            = new EnumMap<>(RewardType.class);
    
    private RewardRegistry() {}
    
    static {
        register(RewardType.ITEM, ItemReward::fromJson, ItemReward::fromNetwork, ItemReward::fromNbt);
        register(RewardType.EXPERIENCE, ExperienceReward::fromJson, ExperienceReward::fromNetwork, ExperienceReward::fromNbt);
        register(RewardType.REPUTATION, ReputationReward::fromJson, ReputationReward::fromNetwork, ReputationReward::fromNbt);
        register(RewardType.COIN, CoinReward::fromJson, CoinReward::fromNetwork, CoinReward::fromNbt);
        register(RewardType.ADVENTURE_EXP, AdventureExpReward::fromJson, AdventureExpReward::fromNetwork, AdventureExpReward::fromNbt);
    }
    
    public static void register(RewardType type,
                                 Function<JsonObject, QuestReward> jsonDeserializer,
                                 Function<FriendlyByteBuf, QuestReward> networkDeserializer,
                                 Function<CompoundTag, QuestReward> nbtDeserializer) {
        JSON_DESERIALIZERS.put(type, jsonDeserializer);
        NETWORK_DESERIALIZERS.put(type, networkDeserializer);
        NBT_DESERIALIZERS.put(type, nbtDeserializer);
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
    
    /**
     * 从 NBT 反序列化奖励
     */
    public static QuestReward fromNbt(CompoundTag tag) {
        String typeStr = tag.getString("type");
        RewardType type = RewardType.fromString(typeStr);
        
        Function<CompoundTag, QuestReward> deserializer = NBT_DESERIALIZERS.get(type);
        if (deserializer == null) {
            RoadWeaverRPG.LOGGER.error("Unknown reward type in NBT: {}", typeStr);
            return null;
        }
        
        return deserializer.apply(tag);
    }
    
    /**
     * 序列化奖励到 NBT
     */
    public static CompoundTag toNbt(QuestReward reward) {
        CompoundTag tag = reward.toJson().toString().isEmpty() ? new CompoundTag() : new CompoundTag();
        tag.putString("type", reward.getType().getSerializedName());
        
        // 使用 toJson 作为中间格式（简化实现）
        JsonObject json = reward.toJson();
        json.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonPrimitive()) {
                if (entry.getValue().getAsJsonPrimitive().isString()) {
                    tag.putString(entry.getKey(), entry.getValue().getAsString());
                } else if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                    tag.putInt(entry.getKey(), entry.getValue().getAsInt());
                }
            }
        });
        
        return tag;
    }
}
