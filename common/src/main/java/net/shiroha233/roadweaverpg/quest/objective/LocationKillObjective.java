package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.ConditionRegistry;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.condition.impl.AreaCondition;
import net.shiroha233.roadweaverpg.quest.type.QuestType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 定点击杀目标 - 全面使用condition系统
 * 
 * 重构说明：
 * - 移除TargetLocation中的自定义checkPosition/contains方法
 * - 使用AreaCondition进行区域判定
 * - 所有判定逻辑统一由condition系统处理
 */
public class LocationKillObjective extends AbstractObjective {
    
    private final List<TargetLocation> locations;
    private final SpawnConfig spawnConfig;
    private final boolean autoCollectDrops;
    private final boolean showOnMap;
    private final String mapIcon;
    private final int mapColor;
    private final List<PlayerCondition<ConditionContext>> conditions;
    
    public LocationKillObjective(String id, ResourceLocation entityId, int amount,
                                  Component description, List<TargetLocation> locations,
                                  SpawnConfig spawnConfig, boolean autoCollectDrops,
                                  boolean showOnMap, String mapIcon, int mapColor,
                                  List<PlayerCondition<ConditionContext>> conditions) {
        super(id, QuestType.LOCATION_KILL, entityId, amount, description);
        this.locations = Collections.unmodifiableList(new ArrayList<>(locations));
        this.spawnConfig = spawnConfig;
        this.autoCollectDrops = autoCollectDrops;
        this.showOnMap = showOnMap;
        this.mapIcon = mapIcon != null ? mapIcon : "quest_target";
        this.mapColor = mapColor;
        this.conditions = conditions != null 
                ? Collections.unmodifiableList(new ArrayList<>(conditions)) 
                : Collections.emptyList();
    }
    
    // region Getters
    public List<TargetLocation> getLocations() { return locations; }
    public SpawnConfig getSpawnConfig() { return spawnConfig; }
    public boolean isAutoCollectDrops() { return autoCollectDrops; }
    public boolean isShowOnMap() { return showOnMap; }
    public String getMapIcon() { return mapIcon; }
    public int getMapColor() { return mapColor; }
    public List<PlayerCondition<ConditionContext>> getConditions() { return conditions; }
    // endregion
    
    public boolean isInTargetArea(BlockPos pos) {
        for (TargetLocation loc : locations) {
            if (loc.toCondition().evaluate(null, ConditionContext.ofPosition(pos))) {
                return true;
            }
        }
        return false;
    }
    
    public TargetLocation getNearestLocation(BlockPos pos) {
        TargetLocation nearest = null;
        double minDist = Double.MAX_VALUE;
        for (TargetLocation loc : locations) {
            double dist = loc.distanceTo(pos);
            if (dist < minDist) {
                minDist = dist;
                nearest = loc;
            }
        }
        return nearest;
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeVarInt(locations.size());
        for (TargetLocation loc : locations) {
            loc.toNetwork(buf);
        }
        buf.writeBoolean(spawnConfig != null);
        if (spawnConfig != null) {
            spawnConfig.toNetwork(buf);
        }
        buf.writeBoolean(autoCollectDrops);
        buf.writeBoolean(showOnMap);
        buf.writeUtf(mapIcon);
        buf.writeVarInt(mapColor);
        buf.writeVarInt(0); // 条件不通过网络传输
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonArray locArray = new JsonArray();
        for (TargetLocation loc : locations) {
            locArray.add(loc.toJson());
        }
        json.add("locations", locArray);
        if (spawnConfig != null) {
            json.add("spawn_config", spawnConfig.toJson());
        }
        json.addProperty("auto_collect_drops", autoCollectDrops);
        json.addProperty("show_on_map", showOnMap);
        json.addProperty("map_icon", mapIcon);
        json.addProperty("map_color", mapColor);
        return json;
    }
    
    public static LocationKillObjective fromNetwork(FriendlyByteBuf buf) {
        ObjectiveData data = readBaseFromNetwork(buf);
        int locCount = buf.readVarInt();
        List<TargetLocation> locations = new ArrayList<>(locCount);
        for (int i = 0; i < locCount; i++) {
            locations.add(TargetLocation.fromNetwork(buf));
        }
        SpawnConfig spawnConfig = buf.readBoolean() ? SpawnConfig.fromNetwork(buf) : null;
        boolean autoCollect = buf.readBoolean();
        boolean showOnMap = buf.readBoolean();
        String mapIcon = buf.readUtf();
        int mapColor = buf.readVarInt();
        buf.readVarInt(); // 跳过条件数量
        return new LocationKillObjective(data.id(), data.target(), data.amount(), data.description(),
                locations, spawnConfig, autoCollect, showOnMap, mapIcon, mapColor, null);
    }
    
    public static LocationKillObjective fromJson(String id, JsonObject json) {
        ResourceLocation entity = new ResourceLocation(json.get("target").getAsString());
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        Component desc = json.has("description")
                ? parseDescription(json.get("description").getAsString())
                : Component.translatable("objective.roadweaver_rpg.location_kill");
        
        List<TargetLocation> locations = new ArrayList<>();
        if (json.has("locations") && json.get("locations").isJsonArray()) {
            for (var elem : json.getAsJsonArray("locations")) {
                if (elem.isJsonObject()) {
                    locations.add(TargetLocation.fromJson(elem.getAsJsonObject()));
                }
            }
        }
        
        SpawnConfig spawnConfig = null;
        if (json.has("spawn_config") && json.get("spawn_config").isJsonObject()) {
            spawnConfig = SpawnConfig.fromJson(json.getAsJsonObject("spawn_config"));
        }
        
        boolean autoCollect = json.has("auto_collect_drops") && json.get("auto_collect_drops").getAsBoolean();
        boolean showOnMap = !json.has("show_on_map") || json.get("show_on_map").getAsBoolean();
        String mapIcon = json.has("map_icon") ? json.get("map_icon").getAsString() : "quest_target";
        int mapColor = json.has("map_color") ? json.get("map_color").getAsInt() : 0xFF5555;
        
        // 解析额外条件
        List<PlayerCondition<ConditionContext>> conditions = new ArrayList<>();
        if (json.has("conditions") && json.get("conditions").isJsonArray()) {
            for (var elem : json.getAsJsonArray("conditions")) {
                if (elem.isJsonObject()) {
                    conditions.add(ConditionRegistry.fromJson(elem.getAsJsonObject()));
                }
            }
        }
        
        return new LocationKillObjective(id, entity, amount, desc, locations,
                spawnConfig, autoCollect, showOnMap, mapIcon, mapColor, conditions);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
    
    /** 
     * 目标区域定义 - 使用condition系统
     * 
     * 重构说明：
     * - 移除checkPosition/contains方法，改用toCondition()
     * - 区域判定委托给AreaCondition
     */
    public record TargetLocation(BlockPos center, int radius, @Nullable String name, @Nullable ResourceLocation dimension) {
        
        /**
         * 检查位置是否在目标区域内（使用condition系统）
         */
        public boolean checkPosition(BlockPos pos) {
            ConditionContext ctx = ConditionContext.ofPosition(pos);
            return toCondition().evaluate(null, ctx);
        }
        
        public double distanceTo(BlockPos pos) { return Math.sqrt(center.distSqr(pos)); }
        
        /**
         * 转换为统一条件（核心方法）
         */
        public PlayerCondition<ConditionContext> toCondition() {
            return AreaCondition.cylinder(center, radius, radius * 2, dimension);
        }
        
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeBlockPos(center);
            buf.writeVarInt(radius);
            buf.writeUtf(name != null ? name : "");
            buf.writeBoolean(dimension != null);
            if (dimension != null) buf.writeResourceLocation(dimension);
        }
        
        public static TargetLocation fromNetwork(FriendlyByteBuf buf) {
            BlockPos center = buf.readBlockPos();
            int radius = buf.readVarInt();
            String name = buf.readUtf();
            ResourceLocation dim = buf.readBoolean() ? buf.readResourceLocation() : null;
            return new TargetLocation(center, radius, name.isEmpty() ? null : name, dim);
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("x", center.getX());
            json.addProperty("y", center.getY());
            json.addProperty("z", center.getZ());
            json.addProperty("radius", radius);
            if (name != null) json.addProperty("name", name);
            if (dimension != null) json.addProperty("dimension", dimension.toString());
            return json;
        }
        
        public static TargetLocation fromJson(JsonObject json) {
            int x = json.get("x").getAsInt();
            int y = json.has("y") ? json.get("y").getAsInt() : 64;
            int z = json.get("z").getAsInt();
            int radius = json.has("radius") ? json.get("radius").getAsInt() : 32;
            String name = json.has("name") ? json.get("name").getAsString() : null;
            ResourceLocation dim = json.has("dimension") 
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            return new TargetLocation(new BlockPos(x, y, z), radius, name, dim);
        }
    }

    /** 怪物刷新配置 */
    public record SpawnConfig(boolean enabled, int maxCount, int respawnDelayTicks, int spawnRadius, boolean persistentMobs) {
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
            buf.writeVarInt(maxCount);
            buf.writeVarInt(respawnDelayTicks);
            buf.writeVarInt(spawnRadius);
            buf.writeBoolean(persistentMobs);
        }
        
        public static SpawnConfig fromNetwork(FriendlyByteBuf buf) {
            return new SpawnConfig(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("max_count", maxCount);
            json.addProperty("respawn_delay", respawnDelayTicks);
            json.addProperty("spawn_radius", spawnRadius);
            json.addProperty("persistent_mobs", persistentMobs);
            return json;
        }
        
        public static SpawnConfig fromJson(JsonObject json) {
            boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
            int maxCount = json.has("max_count") ? json.get("max_count").getAsInt() : 5;
            int respawnDelay = json.has("respawn_delay") ? json.get("respawn_delay").getAsInt() : 200;
            int spawnRadius = json.has("spawn_radius") ? json.get("spawn_radius").getAsInt() : 16;
            boolean persistent = json.has("persistent_mobs") && json.get("persistent_mobs").getAsBoolean();
            return new SpawnConfig(enabled, maxCount, respawnDelay, spawnRadius, persistent);
        }
        
        public static SpawnConfig defaultConfig() { return new SpawnConfig(true, 5, 200, 16, false); }
    }
}
