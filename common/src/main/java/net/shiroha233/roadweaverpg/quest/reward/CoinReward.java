package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.wallet.WalletService;

/**
 * 金币奖励 - 直接存入玩家钱包
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
        // 直接存入钱包，不再给物品
        WalletService.addCoins(player, amount);
    }
    
    @Override
    public boolean canGrant(ServerPlayer player) {
        return true; // 钱包系统始终可用
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
    
    public static CoinReward fromNbt(net.minecraft.nbt.CompoundTag tag) {
        int amount = tag.getInt("amount");
        return new CoinReward(amount);
    }
}
