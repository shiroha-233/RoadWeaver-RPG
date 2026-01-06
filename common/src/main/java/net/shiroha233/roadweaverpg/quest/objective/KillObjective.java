package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

import java.util.Optional;

/**
 * 消灭目标
 */
public class KillObjective extends AbstractObjective {
    
    private final boolean requiresLocation;
    private final ResourceLocation locationId;
    
    public KillObjective(String id, ResourceLocation entityId, int amount, 
                         Component description, boolean requiresLocation, 
                         ResourceLocation locationId) {
        super(id, QuestType.KILL, entityId, amount, description);
        this.requiresLocation = requiresLocation;
        this.locationId = locationId;
    }
    
    public KillObjective(String id, ResourceLocation entityId, int amount, Component description) {
        this(id, entityId, amount, description, false, null);
    }
    
    @Override
    public int checkProgress(ServerPlayer player, String eventType, Object eventData) {
        if (!"entity_kill".equals(eventType) || !(eventData instanceof Entity entity)) {
            return 0;
        }
        
        Optional<ResourceLocation> entityType = RegistryHelper.getEntityTypeId(entity.getType());
        if (entityType.isEmpty() || !entityType.get().equals(targetResource)) {
            return 0;
        }
        
        if (requiresLocation && locationId != null) {
            if (!isInRequiredLocation(player, entity)) {
                return 0;
            }
        }
        
        return 1;
    }
    
    private boolean isInRequiredLocation(ServerPlayer player, Entity entity) {
        if (locationId == null) return true;
        
        try {
            Class<?> roadServiceClass = Class.forName("net.shiroha233.roadweaver.road.RoadService");
            Object service = roadServiceClass.getMethod("getInstance").invoke(null);
            if (service != null) {
                Boolean result = (Boolean) roadServiceClass.getMethod("isNearRoad", 
                        net.minecraft.core.BlockPos.class, int.class)
                        .invoke(service, entity.blockPosition(), 32);
                return result != null && result;
            }
        } catch (ClassNotFoundException e) {
            return true;
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.debug("Failed to check road location: {}", e.getMessage());
            return true;
        }
        return true;
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeBoolean(requiresLocation);
        buf.writeBoolean(locationId != null);
        if (locationId != null) {
            buf.writeResourceLocation(locationId);
        }
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
        return new KillObjective(data.id(), data.target(), data.amount(), 
                data.description(), reqLoc, loc);
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
        return new KillObjective(id, entity, amount, desc, reqLoc, loc);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
}
