package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.item.ModItems;

/**
 * 金币奖励
 */
public class CoinReward implements QuestReward {
    
    private final int amount;
    
    public CoinReward(int amount) {
        this.amount = amount;
    }
    
    @Override
    public RewardType getType() {
        return RewardType.COIN;
    }
    
    @Override
    public Component getDescription() {
        return Component.translatable("reward.roadweaver_rpg.coin", amount);
    }
    
    @Override
    public void grant(ServerPlayer player) {
        if (ModItems.COIN == null) return;
        
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, 64);
            ItemStack coinStack = new ItemStack(ModItems.COIN.get(), stackSize);
            
            if (!player.getInventory().add(coinStack)) {
                player.drop(coinStack, false);
            }
            remaining -= stackSize;
        }
    }
    
    @Override
    public boolean canGrant(ServerPlayer player) {
        return ModItems.COIN != null;
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(amount);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", RewardType.COIN.getSerializedName());
        json.addProperty("amount", amount);
        return json;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public static CoinReward fromNetwork(FriendlyByteBuf buf) {
        return new CoinReward(buf.readVarInt());
    }
    
    public static CoinReward fromJson(JsonObject json) {
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        return new CoinReward(amount);
    }
}
