package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.adventure.AdventureDataService;

/**
 * 冒险经验奖励
 * 完成任务时给予冒险经验
 */
public class AdventureExpReward implements QuestReward {
    
    private final int amount;
    
    public AdventureExpReward(int amount) {
        this.amount = amount;
    }
    
    public int getAmount() { return amount; }
    
    @Override
    public RewardType getType() { return RewardType.ADVENTURE_EXP; }
    
    @Override
    public Component getDescription() {
        return Component.translatable("reward.roadweaver_rpg.adventure_exp", "+" + amount);
    }
    
    @Override
    public void grant(ServerPlayer player) {
        try {
            AdventureDataService.getInstance().addAdventureExp(player, amount);
            RoadWeaverRPG.LOGGER.debug("Granted {} adventure exp to {}", 
                    amount, player.getName().getString());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to grant adventure exp reward", e);
        }
    }
    
    @Override
    public boolean canGrant(ServerPlayer player) { return true; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(amount);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", RewardType.ADVENTURE_EXP.getSerializedName());
        json.addProperty("amount", amount);
        return json;
    }
    
    public static AdventureExpReward fromNetwork(FriendlyByteBuf buf) {
        int amount = buf.readVarInt();
        return new AdventureExpReward(amount);
    }
    
    public static AdventureExpReward fromJson(JsonObject json) {
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 10;
        return new AdventureExpReward(amount);
    }
    
    public static AdventureExpReward fromNbt(CompoundTag tag) {
        int amount = tag.getInt("amount");
        return new AdventureExpReward(amount);
    }
}
