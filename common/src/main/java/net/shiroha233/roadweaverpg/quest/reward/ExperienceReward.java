package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 经验奖励
 */
public class ExperienceReward implements QuestReward {
    
    private final int amount;
    private final boolean isLevels;
    
    public ExperienceReward(int amount, boolean isLevels) {
        this.amount = amount;
        this.isLevels = isLevels;
    }
    
    @Override
    public RewardType getType() { return RewardType.EXPERIENCE; }
    
    @Override
    public Component getDescription() {
        String key = isLevels ? "reward.roadweaver_rpg.levels" : "reward.roadweaver_rpg.experience";
        return Component.translatable(key, amount);
    }
    
    @Override
    public void grant(ServerPlayer player) {
        if (isLevels) {
            player.giveExperienceLevels(amount);
        } else {
            player.giveExperiencePoints(amount);
        }
    }
    
    @Override
    public boolean canGrant(ServerPlayer player) { return true; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(amount);
        buf.writeBoolean(isLevels);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", RewardType.EXPERIENCE.getSerializedName());
        json.addProperty("amount", amount);
        json.addProperty("levels", isLevels);
        return json;
    }
    
    public static ExperienceReward fromNetwork(FriendlyByteBuf buf) {
        int amount = buf.readVarInt();
        boolean isLevels = buf.readBoolean();
        return new ExperienceReward(amount, isLevels);
    }
    
    public static ExperienceReward fromJson(JsonObject json) {
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 100;
        boolean isLevels = json.has("levels") && json.get("levels").getAsBoolean();
        return new ExperienceReward(amount, isLevels);
    }
}
