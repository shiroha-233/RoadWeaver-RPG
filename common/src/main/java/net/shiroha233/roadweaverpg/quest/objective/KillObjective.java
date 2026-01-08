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
 * 消灭目标 - 支持额外条件（群系、天气、维度等）
 */
public class KillObjective extends AbstractObjective {
    
    private final boolean requiresLocation;
    private final ResourceLocation locationId;
    private final List<PlayerCondition<ConditionContext>> conditions;
    
    public KillObjective(String id, ResourceLocation entityId, int amount, 
                         Component description, boolean requiresLocation, 
                         ResourceLocation locationId,
                         List<PlayerCondition<ConditionContext>> conditions) {
        super(id, QuestType.KILL, entityId, amount, description);
        this.requiresLocation = requiresLocation;
        this.locationId = locationId;
        this.conditions = conditions != null ? Collections.unmodifiableList(new ArrayList<>(conditions)) : Collections.emptyList();
    }
    
    public KillObjective(String id, ResourceLocation entityId, int amount, Component description) {
        this(id, entityId, amount, description, false, null, null);
    }
    
    public List<PlayerCondition<ConditionContext>> getConditions() { return conditions; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeBoolean(requiresLocation);
        buf.writeBoolean(locationId != null);
        if (locationId != null) {
            buf.writeResourceLocation(locationId);
        }
        // 条件不通过网络传输
        buf.writeVarInt(0);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("requires_location", requiresLocation);
        if (locationId != null) {
            json.addProperty("location", locationId.toString());
        }
        return json;
    }
    
    public static KillObjective fromNetwork(FriendlyByteBuf buf) {
        ObjectiveData data = readBaseFromNetwork(buf);
        boolean reqLoc = buf.readBoolean();
        ResourceLocation loc = buf.readBoolean() ? buf.readResourceLocation() : null;
        buf.readVarInt(); // 跳过条件数量
        return new KillObjective(data.id(), data.target(), data.amount(), 
                data.description(), reqLoc, loc, null);
    }
    
    public static KillObjective fromJson(String id, JsonObject json) {
        ResourceLocation entity = new ResourceLocation(json.get("target").getAsString());
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        Component desc = json.has("description") 
                ? parseDescription(json.get("description").getAsString())
                : Component.literal("Kill targets");
        boolean reqLoc = json.has("requires_location") && json.get("requires_location").getAsBoolean();
        ResourceLocation loc = json.has("location") 
                ? new ResourceLocation(json.get("location").getAsString()) : null;
        
        // 解析额外条件
        List<PlayerCondition<ConditionContext>> conditions = new ArrayList<>();
        if (json.has("conditions") && json.get("conditions").isJsonArray()) {
            for (var elem : json.getAsJsonArray("conditions")) {
                if (elem.isJsonObject()) {
                    conditions.add(ConditionRegistry.fromJson(elem.getAsJsonObject()));
                }
            }
        }
        
        return new KillObjective(id, entity, amount, desc, reqLoc, loc, conditions);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
}
