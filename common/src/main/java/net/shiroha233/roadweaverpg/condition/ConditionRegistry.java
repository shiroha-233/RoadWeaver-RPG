package net.shiroha233.roadweaverpg.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.condition.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 条件注册表
 * 
 * 负责从JSON解析条件对象
 */
public final class ConditionRegistry {
    
    private ConditionRegistry() {}
    
    private static final ConcurrentHashMap<String, Function<JsonObject, PlayerCondition<ConditionContext>>> 
            FACTORIES = new ConcurrentHashMap<>();
    
    private static volatile boolean initialized = false;
    
    public static synchronized void init() {
        if (initialized) return;
        
        registerDimensionConditions();
        registerBiomeConditions();
        registerTimeConditions();
        registerWeatherConditions();
        registerHeightConditions();
        registerAreaConditions();
        registerEnvironmentConditions();
        registerPlayerStateConditions();
        registerItemConditions();
        registerQuestConditions();
        registerCompositeConditions();
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("条件系统初始化完成，已注册 {} 个条件类型", FACTORIES.size());
    }
    
    public static void register(String type, Function<JsonObject, PlayerCondition<ConditionContext>> factory) {
        FACTORIES.put(type, factory);
    }
    
    public static PlayerCondition<ConditionContext> fromJson(JsonObject json) {
        if (json == null || !json.has("type")) {
            return PlayerCondition.always();
        }
        
        String type = json.get("type").getAsString();
        Function<JsonObject, PlayerCondition<ConditionContext>> factory = FACTORIES.get(type);
        
        if (factory == null) {
            RoadWeaverRPG.LOGGER.warn("未知条件类型: {}", type);
            return PlayerCondition.always();
        }
        
        try {
            return factory.apply(json);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("解析条件失败: {} - {}", type, e.getMessage());
            return PlayerCondition.always();
        }
    }
    
    public static PlayerCondition<ConditionContext> fromJsonArray(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return PlayerCondition.always();
        }
        
        List<PlayerCondition<ConditionContext>> conditions = new ArrayList<>();
        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                conditions.add(fromJson(elem.getAsJsonObject()));
            }
        }
        return combineAnd(conditions);
    }
    
    // ==================== 维度条件 ====================
    private static void registerDimensionConditions() {
        register("in_dimension", json -> {
            String dim = json.get("dimension").getAsString();
            return DimensionCondition.of(dim);
        });
        register("in_overworld", json -> DimensionCondition.overworld());
        register("in_nether", json -> DimensionCondition.nether());
        register("in_end", json -> DimensionCondition.end());
    }
    
    // ==================== 群系条件 ====================
    private static void registerBiomeConditions() {
        register("in_biome", json -> {
            String biome = json.get("biome").getAsString();
            return BiomeCondition.of(biome);
        });
        register("in_biome_tag", json -> {
            String tag = json.get("tag").getAsString();
            return BiomeCondition.ofTag(tag);
        });
    }
    
    // ==================== 时间条件 ====================
    private static void registerTimeConditions() {
        register("is_day", json -> TimeCondition.day());
        register("is_night", json -> TimeCondition.night());
        register("is_dawn", json -> TimeCondition.dawn());
        register("is_dusk", json -> TimeCondition.dusk());
        register("time_range", json -> {
            long start = json.get("start").getAsLong();
            long end = json.get("end").getAsLong();
            return TimeCondition.range(start, end);
        });
        register("full_moon", json -> MoonPhaseCondition.fullMoon());
        register("new_moon", json -> MoonPhaseCondition.newMoon());
        register("moon_phase", json -> {
            int phase = json.get("phase").getAsInt();
            return MoonPhaseCondition.phase(phase);
        });
    }
    
    // ==================== 天气条件 ====================
    private static void registerWeatherConditions() {
        register("is_clear", json -> WeatherCondition.clear());
        register("is_raining", json -> WeatherCondition.rain());
        register("is_thundering", json -> WeatherCondition.thunder());
        register("rain_at_player", json -> WeatherCondition.rainAt());
    }
    
    // ==================== 高度条件 ====================
    private static void registerHeightConditions() {
        register("min_y", json -> {
            int minY = json.get("value").getAsInt();
            return HeightCondition.minY(minY);
        });
        register("max_y", json -> {
            int maxY = json.get("value").getAsInt();
            return HeightCondition.maxY(maxY);
        });
        register("in_height_range", json -> {
            int minY = json.get("min_y").getAsInt();
            int maxY = json.get("max_y").getAsInt();
            return HeightCondition.range(minY, maxY);
        });
        register("underground", json -> HeightCondition.underground());
    }
    
    // ==================== 区域条件 ====================
    private static void registerAreaConditions() {
        register("in_sphere", json -> {
            BlockPos center = parseBlockPos(json);
            double radius = json.get("radius").getAsDouble();
            ResourceLocation dim = json.has("dimension") 
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            return AreaCondition.sphere(center, radius, dim);
        });
        
        register("in_cylinder", json -> {
            BlockPos center = parseBlockPos(json);
            double hRadius = json.get("horizontal_radius").getAsDouble();
            double vRadius = json.has("vertical_radius") 
                    ? json.get("vertical_radius").getAsDouble() : hRadius;
            ResourceLocation dim = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            return AreaCondition.cylinder(center, hRadius, vRadius, dim);
        });
        
        register("in_area", json -> {
            BlockPos center = parseBlockPos(json);
            double radius = json.get("radius").getAsDouble();
            ResourceLocation dim = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            return AreaCondition.cylinder(center, radius, radius, dim);
        });
        
        register("in_box", json -> {
            BlockPos min = parseBlockPos(json, "min");
            BlockPos max = parseBlockPos(json, "max");
            ResourceLocation dim = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            return AreaCondition.box(min, max, dim);
        });
        
        register("in_structure", json -> {
            String structure = json.get("structure").getAsString();
            return StructureCondition.of(structure);
        });
    }
    
    // ==================== 环境条件 ====================
    private static void registerEnvironmentConditions() {
        register("in_water", json -> EnvironmentCondition.inWater());
        register("in_lava", json -> EnvironmentCondition.inLava());
        register("on_fire", json -> EnvironmentCondition.onFire());
        register("under_sky", json -> EnvironmentCondition.underSky());
    }
    
    // ==================== 玩家状态条件 ====================
    private static void registerPlayerStateConditions() {
        register("is_sneaking", json -> PlayerStateCondition.sneaking());
        register("is_sprinting", json -> PlayerStateCondition.sprinting());
        register("is_swimming", json -> PlayerStateCondition.swimming());
        register("is_flying", json -> PlayerStateCondition.flying());
        register("is_on_ground", json -> PlayerStateCondition.onGround());
        
        register("game_mode", json -> {
            String mode = json.get("mode").getAsString();
            GameType gameType = switch (mode.toLowerCase()) {
                case "creative" -> GameType.CREATIVE;
                case "adventure" -> GameType.ADVENTURE;
                case "spectator" -> GameType.SPECTATOR;
                default -> GameType.SURVIVAL;
            };
            return PlayerStateCondition.gameMode(gameType);
        });
        
        register("health", json -> {
            String op = json.has("operator") ? json.get("operator").getAsString() : ">=";
            float value = json.get("value").getAsFloat();
            return "<=".equals(op) ? HealthCondition.atMost(value) : HealthCondition.atLeast(value);
        });
        
        register("health_percent", json -> {
            float percent = json.get("value").getAsFloat();
            return HealthCondition.percentAtLeast(percent);
        });
        
        register("has_effect", json -> {
            String effect = json.get("effect").getAsString();
            if (json.has("level")) {
                int level = json.get("level").getAsInt();
                return EffectCondition.hasEffectLevel(effect, level);
            }
            return EffectCondition.hasEffect(effect);
        });
    }
    
    // ==================== 物品条件 ====================
    private static void registerItemConditions() {
        register("has_item", json -> {
            String item = json.get("item").getAsString();
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return ItemCondition.hasItem(item, count);
        });
        
        register("has_item_tag", json -> {
            String tag = json.get("tag").getAsString();
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            return ItemCondition.hasItemTag(tag, count);
        });
        
        register("main_hand_item", json -> {
            String item = json.get("item").getAsString();
            return ItemCondition.mainHand(item);
        });
        
        register("holding_item", json -> {
            String item = json.get("item").getAsString();
            return ItemCondition.holding(item);
        });
        
        register("wearing_armor", json -> {
            String item = json.get("item").getAsString();
            String slot = json.get("slot").getAsString();
            EquipmentSlot equipSlot = switch (slot.toLowerCase()) {
                case "head", "helmet" -> EquipmentSlot.HEAD;
                case "chest", "chestplate" -> EquipmentSlot.CHEST;
                case "legs", "leggings" -> EquipmentSlot.LEGS;
                case "feet", "boots" -> EquipmentSlot.FEET;
                default -> EquipmentSlot.HEAD;
            };
            return ItemCondition.wearing(item, equipSlot);
        });
    }
    
    // ==================== 委托条件 ====================
    private static void registerQuestConditions() {
        register("quest_completed", json -> {
            String quest = json.get("quest").getAsString();
            return QuestCondition.completed(quest);
        });
        
        register("quest_active", json -> {
            String quest = json.get("quest").getAsString();
            return QuestCondition.active(quest);
        });
        
        register("quest_count", json -> {
            String quest = json.get("quest").getAsString();
            int count = json.get("count").getAsInt();
            return QuestCondition.completionCount(quest, count);
        });
        
        register("reputation_level", json -> {
            String faction = json.get("faction").getAsString();
            int level = json.get("level").getAsInt();
            return QuestCondition.reputationLevel(faction, level);
        });
    }
    
    // ==================== 组合条件 ====================
    private static void registerCompositeConditions() {
        register("and", json -> {
            JsonArray conditions = json.getAsJsonArray("conditions");
            List<PlayerCondition<ConditionContext>> list = new ArrayList<>();
            for (JsonElement elem : conditions) {
                list.add(fromJson(elem.getAsJsonObject()));
            }
            return combineAnd(list);
        });
        
        register("or", json -> {
            JsonArray conditions = json.getAsJsonArray("conditions");
            List<PlayerCondition<ConditionContext>> list = new ArrayList<>();
            for (JsonElement elem : conditions) {
                list.add(fromJson(elem.getAsJsonObject()));
            }
            return combineOr(list);
        });
        
        register("not", json -> {
            JsonObject condition = json.getAsJsonObject("condition");
            return fromJson(condition).negate();
        });
    }
    
    // ==================== 辅助方法 ====================
    private static BlockPos parseBlockPos(JsonObject json) {
        int x = json.has("x") ? json.get("x").getAsInt() : 0;
        int y = json.has("y") ? json.get("y").getAsInt() : 64;
        int z = json.has("z") ? json.get("z").getAsInt() : 0;
        return new BlockPos(x, y, z);
    }
    
    private static BlockPos parseBlockPos(JsonObject json, String key) {
        if (json.has(key)) {
            JsonObject pos = json.getAsJsonObject(key);
            return new BlockPos(
                    pos.get("x").getAsInt(),
                    pos.has("y") ? pos.get("y").getAsInt() : 64,
                    pos.get("z").getAsInt()
            );
        }
        return parseBlockPos(json);
    }
    
    private static PlayerCondition<ConditionContext> combineAnd(List<PlayerCondition<ConditionContext>> conditions) {
        if (conditions.isEmpty()) return PlayerCondition.always();
        if (conditions.size() == 1) return conditions.get(0);
        
        return (player, ctx) -> {
            for (PlayerCondition<ConditionContext> cond : conditions) {
                if (!cond.evaluate(player, ctx)) return false;
            }
            return true;
        };
    }
    
    private static PlayerCondition<ConditionContext> combineOr(List<PlayerCondition<ConditionContext>> conditions) {
        if (conditions.isEmpty()) return PlayerCondition.never();
        if (conditions.size() == 1) return conditions.get(0);
        
        return (player, ctx) -> {
            for (PlayerCondition<ConditionContext> cond : conditions) {
                if (cond.evaluate(player, ctx)) return true;
            }
            return false;
        };
    }
}
