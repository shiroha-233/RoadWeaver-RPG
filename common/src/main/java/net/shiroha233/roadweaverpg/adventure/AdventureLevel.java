package net.shiroha233.roadweaverpg.adventure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;
import net.shiroha233.roadweaverpg.quest.reward.RewardRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 冒险等级定义
 * 定义每个等级所需经验和升级奖励
 */
public class AdventureLevel {
    private final int level;
    private final int requiredExperience;
    private final List<QuestReward> rewards;

    public AdventureLevel(int level, int requiredExperience, List<QuestReward> rewards) {
        this.level = level;
        this.requiredExperience = requiredExperience;
        this.rewards = Collections.unmodifiableList(rewards);
    }

    public int getLevel() { return level; }
    public int getRequiredExperience() { return requiredExperience; }
    public List<QuestReward> getRewards() { return rewards; }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarInt(requiredExperience);
        buf.writeVarInt(rewards.size());
        for (QuestReward reward : rewards) {
            RewardRegistry.toNetwork(reward, buf);
        }
    }

    public static AdventureLevel fromNetwork(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        int xp = buf.readVarInt();
        int rewardCount = buf.readVarInt();
        List<QuestReward> rewards = new ArrayList<>();
        for (int i = 0; i < rewardCount; i++) {
            QuestReward reward = RewardRegistry.fromNetwork(buf);
            if (reward != null) rewards.add(reward);
        }
        return new AdventureLevel(level, xp, rewards);
    }

    public static AdventureLevel fromJson(int level, JsonObject json) {
        int xp = json.has("xp") ? json.get("xp").getAsInt() : 0;
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
        return new AdventureLevel(level, xp, rewards);
    }
}
