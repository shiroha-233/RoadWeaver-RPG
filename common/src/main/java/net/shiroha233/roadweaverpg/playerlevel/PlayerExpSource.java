package net.shiroha233.roadweaverpg.playerlevel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家经验来源配置
 * 定义击杀特定怪物可获得的经验值
 */
public class PlayerExpSource {
    
    private final ResourceLocation entityType;
    private final int baseExp;
    private final List<String> tags; // 可选的实体标签匹配
    
    public PlayerExpSource(ResourceLocation entityType, int baseExp, List<String> tags) {
        this.entityType = entityType;
        this.baseExp = baseExp;
        this.tags = Collections.unmodifiableList(tags);
    }
    
    public ResourceLocation getEntityType() { return entityType; }
    public int getBaseExp() { return baseExp; }
    public List<String> getTags() { return tags; }
    
    public static PlayerExpSource fromJson(JsonObject json) {
        ResourceLocation entityType = new ResourceLocation(
                json.has("entity") ? json.get("entity").getAsString() : "minecraft:zombie");
        int baseExp = json.has("exp") ? json.get("exp").getAsInt() : 1;
        
        List<String> tags = new ArrayList<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            JsonArray tagArray = json.getAsJsonArray("tags");
            for (JsonElement elem : tagArray) {
                tags.add(elem.getAsString());
            }
        }
        
        return new PlayerExpSource(entityType, baseExp, tags);
    }
}
