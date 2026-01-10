package net.shiroha233.roadweaverpg.worlddifficulty;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 环境适应加成定义
 * 
 * 设计原理：
 * - 定义特定环境条件下的属性加成
 * - 支持天气、时间、生物群系等条件
 * - 数据驱动，从JSON加载
 */
public record EnvironmentBonus(
        String id,
        EnvironmentCondition condition,
        float armorBonus,
        float healthBonus,
        float damageBonus,
        float resistanceBonus
) {
    
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        condition.toNetwork(buf);
        buf.writeFloat(armorBonus);
        buf.writeFloat(healthBonus);
        buf.writeFloat(damageBonus);
        buf.writeFloat(resistanceBonus);
    }
    
    public static EnvironmentBonus fromNetwork(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        EnvironmentCondition condition = EnvironmentCondition.fromNetwork(buf);
        return new EnvironmentBonus(
                id, condition,
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()
        );
    }
    
    public static EnvironmentBonus fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "unknown";
        EnvironmentCondition condition = EnvironmentCondition.fromJson(
                json.has("condition") ? json.getAsJsonObject("condition") : new JsonObject()
        );
        float armor = json.has("armor_bonus") ? json.get("armor_bonus").getAsFloat() : 0;
        float health = json.has("health_bonus") ? json.get("health_bonus").getAsFloat() : 0;
        float damage = json.has("damage_bonus") ? json.get("damage_bonus").getAsFloat() : 0;
        float resistance = json.has("resistance_bonus") ? json.get("resistance_bonus").getAsFloat() : 0;
        
        return new EnvironmentBonus(id, condition, armor, health, damage, resistance);
    }
    
    /**
     * 环境条件定义
     */
    public record EnvironmentCondition(
            ConditionType type,
            String value,
            List<String> values
    ) {
        public enum ConditionType {
            WEATHER,      // 天气：clear, rain, thunder
            TIME,         // 时间：day, night, dawn, dusk
            BIOME,        // 生物群系
            BIOME_TAG,    // 生物群系标签
            DIMENSION,    // 维度
            UNDERGROUND,  // 地下
            ALWAYS        // 始终生效
        }
        
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeEnum(type);
            buf.writeUtf(value != null ? value : "");
            buf.writeVarInt(values.size());
            for (String v : values) {
                buf.writeUtf(v);
            }
        }
        
        public static EnvironmentCondition fromNetwork(FriendlyByteBuf buf) {
            ConditionType type = buf.readEnum(ConditionType.class);
            String value = buf.readUtf();
            int count = buf.readVarInt();
            List<String> values = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                values.add(buf.readUtf());
            }
            return new EnvironmentCondition(type, value.isEmpty() ? null : value, values);
        }
        
        public static EnvironmentCondition fromJson(JsonObject json) {
            String typeStr = json.has("type") ? json.get("type").getAsString() : "always";
            ConditionType type = switch (typeStr.toLowerCase()) {
                case "weather" -> ConditionType.WEATHER;
                case "time" -> ConditionType.TIME;
                case "biome" -> ConditionType.BIOME;
                case "biome_tag" -> ConditionType.BIOME_TAG;
                case "dimension" -> ConditionType.DIMENSION;
                case "underground" -> ConditionType.UNDERGROUND;
                default -> ConditionType.ALWAYS;
            };
            
            String value = json.has("value") ? json.get("value").getAsString() : null;
            List<String> values = new ArrayList<>();
            if (json.has("values") && json.get("values").isJsonArray()) {
                for (var elem : json.getAsJsonArray("values")) {
                    values.add(elem.getAsString());
                }
            }
            
            return new EnvironmentCondition(type, value, values);
        }
        
        public static EnvironmentCondition always() {
            return new EnvironmentCondition(ConditionType.ALWAYS, null, List.of());
        }
    }
}
