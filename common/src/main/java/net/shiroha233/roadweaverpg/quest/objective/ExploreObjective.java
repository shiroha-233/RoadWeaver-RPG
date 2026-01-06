package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

/**
 * 探索目标
 */
public class ExploreObjective extends AbstractObjective {
    
    private final BlockPos targetPos;
    private final int radius;
    
    public ExploreObjective(String id, ResourceLocation locationId, BlockPos targetPos, 
                            int radius, Component description) {
        super(id, QuestType.EXPLORE, locationId, 1, description);
        this.targetPos = targetPos;
        this.radius = radius;
    }
    
    public BlockPos getTargetPos() { return targetPos; }
    public int getRadius() { return radius; }
    
    @Override
    public int checkProgress(ServerPlayer player, String eventType, Object eventData) {
        if (!"player_move".equals(eventType)) return 0;
        
        BlockPos playerPos = player.blockPosition();
        double distance = Math.sqrt(playerPos.distSqr(targetPos));
        
        return distance <= radius ? 1 : 0;
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeBlockPos(targetPos);
        buf.writeVarInt(radius);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("x", targetPos.getX());
        json.addProperty("y", targetPos.getY());
        json.addProperty("z", targetPos.getZ());
        json.addProperty("radius", radius);
        return json;
    }
    
    public static ExploreObjective fromNetwork(FriendlyByteBuf buf) {
        ObjectiveData data = readBaseFromNetwork(buf);
        BlockPos pos = buf.readBlockPos();
        int radius = buf.readVarInt();
        return new ExploreObjective(data.id(), data.target(), pos, radius, data.description());
    }
    
    public static ExploreObjective fromJson(String id, JsonObject json) {
        ResourceLocation location = json.has("location") 
                ? new ResourceLocation(json.get("location").getAsString())
                : new ResourceLocation("minecraft", "overworld");
        
        int x = json.has("x") ? json.get("x").getAsInt() : 0;
        int y = json.has("y") ? json.get("y").getAsInt() : 64;
        int z = json.has("z") ? json.get("z").getAsInt() : 0;
        BlockPos pos = new BlockPos(x, y, z);
        
        int radius = json.has("radius") ? json.get("radius").getAsInt() : 16;
        Component desc = json.has("description") 
                ? parseDescription(json.get("description").getAsString())
                : Component.literal("Explore area");
        
        return new ExploreObjective(id, location, pos, radius, desc);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
}
