package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

import java.util.Optional;

/**
 * 收集目标
 */
public class CollectObjective extends AbstractObjective {
    
    private final boolean consumeOnComplete;
    
    public CollectObjective(String id, ResourceLocation itemId, int amount, 
                            Component description, boolean consumeOnComplete) {
        super(id, QuestType.COLLECT, itemId, amount, description);
        this.consumeOnComplete = consumeOnComplete;
    }
    
    public boolean shouldConsumeOnComplete() { return consumeOnComplete; }
    
    @Override
    public int checkProgress(ServerPlayer player, String eventType, Object eventData) {
        if (!"inventory_check".equals(eventType)) return 0;
        
        Optional<Item> targetItem = RegistryHelper.getItem(targetResource);
        if (targetItem.isEmpty()) return 0;
        
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == targetItem.get()) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeBoolean(consumeOnComplete);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("consume_on_complete", consumeOnComplete);
        return json;
    }
    
    public static CollectObjective fromNetwork(FriendlyByteBuf buf) {
        ObjectiveData data = readBaseFromNetwork(buf);
        boolean consume = buf.readBoolean();
        return new CollectObjective(data.id(), data.target(), data.amount(), 
                data.description(), consume);
    }
    
    public static CollectObjective fromJson(String id, JsonObject json) {
        ResourceLocation item = new ResourceLocation(json.get("target").getAsString());
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        Component desc = json.has("description") 
                ? parseDescription(json.get("description").getAsString())
                : Component.literal("Collect items");
        boolean consume = !json.has("consume_on_complete") || json.get("consume_on_complete").getAsBoolean();
        return new CollectObjective(id, item, amount, desc, consume);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
}
