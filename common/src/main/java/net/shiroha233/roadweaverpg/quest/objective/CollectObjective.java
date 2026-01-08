package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.ConditionRegistry;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 收集目标 - 支持额外条件（天气、群系等）
 */
public class CollectObjective extends AbstractObjective {
    
    private final boolean consumeOnComplete;
    private final List<PlayerCondition<ConditionContext>> conditions;
    
    public CollectObjective(String id, ResourceLocation itemId, int amount, 
                            Component description, boolean consumeOnComplete,
                            List<PlayerCondition<ConditionContext>> conditions) {
        super(id, QuestType.COLLECT, itemId, amount, description);
        this.consumeOnComplete = consumeOnComplete;
        this.conditions = conditions != null ? Collections.unmodifiableList(new ArrayList<>(conditions)) : Collections.emptyList();
    }
    
    public CollectObjective(String id, ResourceLocation itemId, int amount, 
                            Component description, boolean consumeOnComplete) {
        this(id, itemId, amount, description, consumeOnComplete, null);
    }
    
    public boolean shouldConsumeOnComplete() { return consumeOnComplete; }
    public List<PlayerCondition<ConditionContext>> getConditions() { return conditions; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeBoolean(consumeOnComplete);
        // 条件不通过网络传输（仅服务端使用）
        buf.writeVarInt(0);
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
        buf.readVarInt(); // 跳过条件数量
        return new CollectObjective(data.id(), data.target(), data.amount(), 
                data.description(), consume, null);
    }
    
    public static CollectObjective fromJson(String id, JsonObject json) {
        ResourceLocation item = new ResourceLocation(json.get("target").getAsString());
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        Component desc = json.has("description") 
                ? parseDescription(json.get("description").getAsString())
                : Component.literal("Collect items");
        boolean consume = !json.has("consume_on_complete") || json.get("consume_on_complete").getAsBoolean();
        
        // 解析额外条件
        List<PlayerCondition<ConditionContext>> conditions = new ArrayList<>();
        if (json.has("conditions") && json.get("conditions").isJsonArray()) {
            for (var elem : json.getAsJsonArray("conditions")) {
                if (elem.isJsonObject()) {
                    conditions.add(ConditionRegistry.fromJson(elem.getAsJsonObject()));
                }
            }
        }
        
        return new CollectObjective(id, item, amount, desc, consume, conditions);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
}
