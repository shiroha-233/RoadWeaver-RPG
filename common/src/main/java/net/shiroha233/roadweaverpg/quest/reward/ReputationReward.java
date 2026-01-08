package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;

/**
 * 声望奖励
 */
public class ReputationReward implements QuestReward {
    
    private final ResourceLocation factionId;
    private final int amount;
    
    public ReputationReward(ResourceLocation factionId, int amount) {
        this.factionId = factionId;
        this.amount = amount;
    }
    
    public ResourceLocation getFactionId() { return factionId; }
    public int getAmount() { return amount; }
    
    @Override
    public RewardType getType() { return RewardType.REPUTATION; }
    
    @Override
    public Component getDescription() {
        String sign = amount >= 0 ? "+" : "";
        return Component.translatable("reward.roadweaver_rpg.reputation", 
                sign + amount, factionId.getPath());
    }
    
    @Override
    public void grant(ServerPlayer player) {
        try {
            PlayerQuestService.getInstance().addReputationXp(player, factionId, amount);
            RoadWeaverRPG.LOGGER.info("Granted {} reputation with {} to {}", 
                    amount, factionId, player.getName().getString());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to grant reputation reward", e);
            player.sendSystemMessage(Component.translatable("message.roadweaver_rpg.reputation_error")
                    .withStyle(style -> style.withColor(0xFF5555)));
        }
    }
    
    @Override
    public boolean canGrant(ServerPlayer player) { return true; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(factionId);
        buf.writeVarInt(amount);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", RewardType.REPUTATION.getSerializedName());
        json.addProperty("faction", factionId.toString());
        json.addProperty("amount", amount);
        return json;
    }
    
    public static ReputationReward fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation faction = buf.readResourceLocation();
        int amount = buf.readVarInt();
        return new ReputationReward(faction, amount);
    }
    
    public static ReputationReward fromJson(JsonObject json) {
        ResourceLocation faction = new ResourceLocation(
                json.has("faction") ? json.get("faction").getAsString() : "roadweaver_rpg:guild");
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 10;
        return new ReputationReward(faction, amount);
    }
    
    /**
     * 从 NBT 反序列化
     */
    public static ReputationReward fromNbt(net.minecraft.nbt.CompoundTag tag) {
        ResourceLocation faction = new ResourceLocation(tag.getString("faction"));
        int amount = tag.getInt("amount");
        return new ReputationReward(faction, amount);
    }
}
