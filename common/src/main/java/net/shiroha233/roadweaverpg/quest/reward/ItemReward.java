package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;

import java.util.Optional;

/**
 * 物品奖励
 */
public class ItemReward implements QuestReward {
    
    private final ResourceLocation itemId;
    private final int count;
    private final CompoundTag nbt;
    
    public ItemReward(ResourceLocation itemId, int count, CompoundTag nbt) {
        this.itemId = itemId;
        this.count = count;
        this.nbt = nbt;
    }
    
    public ItemReward(ResourceLocation itemId, int count) {
        this(itemId, count, null);
    }
    
    @Override
    public RewardType getType() { return RewardType.ITEM; }
    
    @Override
    public Component getDescription() {
        Optional<Item> item = RegistryHelper.getItem(itemId);
        String itemName = item.map(i -> i.getDescription().getString()).orElse(itemId.toString());
        return Component.translatable("reward.roadweaver_rpg.item", count, itemName);
    }
    
    @Override
    public void grant(ServerPlayer player) {
        Optional<Item> item = RegistryHelper.getItem(itemId);
        if (item.isEmpty()) return;
        
        ItemStack stack = new ItemStack(item.get(), count);
        if (nbt != null) {
            stack.setTag(nbt.copy());
        }
        
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
    
    @Override
    public boolean canGrant(ServerPlayer player) { return true; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        buf.writeVarInt(count);
        buf.writeBoolean(nbt != null);
        if (nbt != null) {
            buf.writeNbt(nbt);
        }
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", RewardType.ITEM.getSerializedName());
        json.addProperty("item", itemId.toString());
        json.addProperty("count", count);
        return json;
    }
    
    public static ItemReward fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int count = buf.readVarInt();
        CompoundTag nbt = buf.readBoolean() ? buf.readNbt() : null;
        return new ItemReward(id, count, nbt);
    }
    
    public static ItemReward fromJson(JsonObject json) {
        ResourceLocation id = new ResourceLocation(json.get("item").getAsString());
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        return new ItemReward(id, count);
    }
    
    /**
     * 从 NBT 反序列化
     */
    public static ItemReward fromNbt(CompoundTag tag) {
        ResourceLocation id = new ResourceLocation(tag.getString("item"));
        int count = tag.getInt("count");
        CompoundTag nbt = tag.contains("nbt") ? tag.getCompound("nbt") : null;
        return new ItemReward(id, count, nbt);
    }
    
    /**
     * 创建用于显示的ItemStack（客户端使用）
     */
    public ItemStack createDisplayStack() {
        Optional<Item> item = RegistryHelper.getItem(itemId);
        if (item.isEmpty()) return ItemStack.EMPTY;
        
        ItemStack stack = new ItemStack(item.get(), count);
        if (nbt != null) {
            stack.setTag(nbt.copy());
        }
        return stack;
    }
    
    public ResourceLocation getItemId() { return itemId; }
    public int getCount() { return count; }
}
