package net.shiroha233.roadweaverpg.playerlevel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectRegistry;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;
import net.shiroha233.roadweaverpg.quest.reward.RewardRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家等级定义
 * 定义每个等级所需经验、效果增益和一次性奖励
 */
public class PlayerLevel {
    
    private final int level;
    private final int requiredExperience;
    private final List<LevelEffect> effects;      // 持续性效果（属性加成等）
    private final List<QuestReward> rewards;      // 一次性奖励（物品等）
    
    public PlayerLevel(int level, int requiredExperience, 
                       List<LevelEffect> effects, List<QuestReward> rewards) {
        this.level = level;
        this.requiredExperience = requiredExperience;
        this.effects = Collections.unmodifiableList(effects);
        this.rewards = Collections.unmodifiableList(rewards);
    }
    
    public int getLevel() { return level; }
    public int getRequiredExperience() { return requiredExperience; }
    public List<LevelEffect> getEffects() { return effects; }
    public List<QuestReward> getRewards() { return rewards; }
    
    /**
     * 序列化到网络
     */
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarInt(requiredExperience);
        
        // 效果列表
        buf.writeVarInt(effects.size());
        for (LevelEffect effect : effects) {
            LevelEffectRegistry.toNetwork(effect, buf);
        }
        
        // 奖励列表
        buf.writeVarInt(rewards.size());
        for (QuestReward reward : rewards) {
            RewardRegistry.toNetwork(reward, buf);
        }
    }
    
    /**
     * 从网络反序列化
     */
    public static PlayerLevel fromNetwork(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        int xp = buf.readVarInt();
        
        // 效果列表
        int effectCount = buf.readVarInt();
        List<LevelEffect> effects = new ArrayList<>();
        for (int i = 0; i < effectCount; i++) {
            LevelEffect effect = LevelEffectRegistry.fromNetwork(buf);
            if (effect != null) effects.add(effect);
        }
        
        // 奖励列表
        int rewardCount = buf.readVarInt();
        List<QuestReward> rewards = new ArrayList<>();
        for (int i = 0; i < rewardCount; i++) {
            QuestReward reward = RewardRegistry.fromNetwork(buf);
            if (reward != null) rewards.add(reward);
        }
        
        return new PlayerLevel(level, xp, effects, rewards);
    }
    
    /**
     * 从JSON反序列化
     */
    public static PlayerLevel fromJson(int level, JsonObject json) {
        int xp = json.has("xp") ? json.get("xp").getAsInt() : 0;
        
        // 解析效果列表
        List<LevelEffect> effects = new ArrayList<>();
        if (json.has("effects") && json.get("effects").isJsonArray()) {
            JsonArray effectArray = json.getAsJsonArray("effects");
            for (JsonElement elem : effectArray) {
                if (elem.isJsonObject()) {
                    LevelEffect effect = LevelEffectRegistry.fromJson(elem.getAsJsonObject());
                    if (effect != null) effects.add(effect);
                }
            }
        }
        
        // 解析奖励列表
        List<QuestReward> rewards = new ArrayList<>();
        if (json.has("rewards") && json.get("rewards").isJsonArray()) {
            JsonArray rewardArray = json.getAsJsonArray("rewards");
            for (JsonElement elem : rewardArray) {
                if (elem.isJsonObject()) {
                    QuestReward reward = RewardRegistry.fromJson(elem.getAsJsonObject());
                    if (reward != null) rewards.add(reward);
                }
            }
        }
        
        return new PlayerLevel(level, xp, effects, rewards);
    }
}
