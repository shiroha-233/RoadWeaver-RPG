package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

/**
 * 目标抽象基类
 */
public abstract class AbstractObjective implements QuestObjective {
    
    protected final String id;
    protected final QuestType type;
    protected final ResourceLocation targetResource;
    protected final int requiredAmount;
    protected final Component description;
    
    protected AbstractObjective(String id, QuestType type, ResourceLocation targetResource, 
                                 int requiredAmount, Component description) {
        this.id = id;
        this.type = type;
        this.targetResource = targetResource;
        this.requiredAmount = requiredAmount;
        this.description = description;
    }
    
    @Override public String getId() { return id; }
    @Override public QuestType getType() { return type; }
    @Override public ResourceLocation getTargetResource() { return targetResource; }
    @Override public int getRequiredAmount() { return requiredAmount; }
    @Override public Component getDescription() { return description; }
    
    @Override
    public Component getDisplayText(int currentProgress) {
        return description.copy()
                .append(Component.literal(" (" + currentProgress + "/" + requiredAmount + ")"));
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeEnum(type);
        buf.writeResourceLocation(targetResource);
        buf.writeVarInt(requiredAmount);
        buf.writeComponent(description);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.getSerializedName());
        json.addProperty("target", targetResource.toString());
        json.addProperty("amount", requiredAmount);
        json.addProperty("description", description.getString());
        return json;
    }
    
    protected static ObjectiveData readBaseFromNetwork(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        QuestType type = buf.readEnum(QuestType.class);
        ResourceLocation target = buf.readResourceLocation();
        int amount = buf.readVarInt();
        Component desc = buf.readComponent();
        return new ObjectiveData(id, type, target, amount, desc);
    }
    
    protected record ObjectiveData(String id, QuestType type, ResourceLocation target, 
                                    int amount, Component description) {}
}
