package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.ConditionRegistry;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.condition.impl.AreaCondition;
import net.shiroha233.roadweaverpg.condition.impl.BiomeCondition;
import net.shiroha233.roadweaverpg.quest.type.QuestType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 探索目标 - 全面使用condition系统
 * 
 * 重构说明：
 * - 移除ExploreTarget中的自定义checkReached方法
 * - 使用AreaCondition + BiomeCondition + DimensionCondition组合判定
 * - 所有判定逻辑统一由condition系统处理
 */
public class ExploreObjective extends AbstractObjective {
    
    private final List<ExploreTarget> targets;
    private final List<PlayerCondition<ConditionContext>> extraConditions;
    
    public ExploreObjective(String id, ResourceLocation locationId, List<ExploreTarget> targets,
                            List<PlayerCondition<ConditionContext>> extraConditions, Component description) {
        super(id, QuestType.EXPLORE, locationId, targets.size(), description);
        this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
        this.extraConditions = extraConditions != null 
                ? Collections.unmodifiableList(new ArrayList<>(extraConditions))
                : Collections.emptyList();
    }
    
    public List<ExploreTarget> getTargets() { return targets; }
    public List<PlayerCondition<ConditionContext>> getExtraConditions() { return extraConditions; }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        
        buf.writeVarInt(targets.size());
        for (ExploreTarget target : targets) {
            target.toNetwork(buf);
        }
        
        // 额外条件不通过网络传输（仅服务端使用）
        buf.writeVarInt(0);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        
        com.google.gson.JsonArray targetsArray = new com.google.gson.JsonArray();
        for (ExploreTarget target : targets) {
            targetsArray.add(target.toJson());
        }
        json.add("targets", targetsArray);
        
        return json;
    }
    
    public static ExploreObjective fromNetwork(FriendlyByteBuf buf) {
        ObjectiveData data = readBaseFromNetwork(buf);
        
        int targetCount = buf.readVarInt();
        List<ExploreTarget> targets = new ArrayList<>(targetCount);
        for (int i = 0; i < targetCount; i++) {
            targets.add(ExploreTarget.fromNetwork(buf));
        }
        
        buf.readVarInt(); // 跳过额外条件数量
        
        return new ExploreObjective(data.id(), data.target(), targets, null, data.description());
    }
    
    public static ExploreObjective fromJson(String id, JsonObject json) {
        ResourceLocation location = json.has("location") 
                ? new ResourceLocation(json.get("location").getAsString())
                : new ResourceLocation("minecraft", "overworld");
        
        Component desc = json.has("description") 
                ? parseDescription(json.get("description").getAsString())
                : Component.translatable("objective.roadweaver_rpg.explore");
        
        List<ExploreTarget> targets = new ArrayList<>();
        
        // 新格式：targets数组
        if (json.has("targets") && json.get("targets").isJsonArray()) {
            for (var elem : json.getAsJsonArray("targets")) {
                if (elem.isJsonObject()) {
                    targets.add(ExploreTarget.fromJson(elem.getAsJsonObject()));
                }
            }
        } else {
            // 兼容旧格式：单点
            int x = json.has("x") ? json.get("x").getAsInt() : 0;
            int y = json.has("y") ? json.get("y").getAsInt() : 64;
            int z = json.has("z") ? json.get("z").getAsInt() : 0;
            int radius = json.has("radius") ? json.get("radius").getAsInt() : 16;
            ResourceLocation dim = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            ResourceLocation biome = json.has("biome")
                    ? new ResourceLocation(json.get("biome").getAsString()) : null;
            targets.add(new ExploreTarget(new BlockPos(x, y, z), radius, null, dim, biome));
        }
        
        // 解析额外条件
        List<PlayerCondition<ConditionContext>> extraConditions = new ArrayList<>();
        if (json.has("extra_conditions") && json.get("extra_conditions").isJsonArray()) {
            for (var elem : json.getAsJsonArray("extra_conditions")) {
                if (elem.isJsonObject()) {
                    extraConditions.add(ConditionRegistry.fromJson(elem.getAsJsonObject()));
                }
            }
        }
        
        return new ExploreObjective(id, location, targets, extraConditions, desc);
    }
    
    private static Component parseDescription(String text) {
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
    
    /**
     * 探索目标点定义 - 使用condition系统
     * 
     * 重构说明：
     * - checkReached方法改为使用toCondition()生成的条件进行判定
     * - 维度、群系、区域判定全部委托给condition系统
     */
    public record ExploreTarget(
            BlockPos center,
            int radius,
            @Nullable String name,
            @Nullable ResourceLocation dimension,
            @Nullable ResourceLocation biome
    ) {
        /**
         * 检查玩家是否到达此目标点（使用condition系统）
         */
        public boolean checkReached(ServerPlayer player, BlockPos playerPos, ResourceLocation playerDim) {
            ConditionContext ctx = ConditionContext.builder()
                    .position(playerPos)
                    .dimension(playerDim)
                    .build();
            return toCondition().evaluate(player, ctx);
        }
        
        /**
         * 转换为统一条件（核心方法）
         * 
         * 原理：将目标点的所有判定条件组合成一个PlayerCondition
         */
        public PlayerCondition<ConditionContext> toCondition() {
            // 基础区域条件（圆柱形）
            PlayerCondition<ConditionContext> cond = AreaCondition.cylinder(center, radius, radius, dimension);
            
            // 添加群系条件
            if (biome != null) {
                cond = cond.and(BiomeCondition.of(biome.toString()));
            }
            
            return cond;
        }
        
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeBlockPos(center);
            buf.writeVarInt(radius);
            buf.writeUtf(name != null ? name : "");
            buf.writeBoolean(dimension != null);
            if (dimension != null) buf.writeResourceLocation(dimension);
            buf.writeBoolean(biome != null);
            if (biome != null) buf.writeResourceLocation(biome);
        }
        
        public static ExploreTarget fromNetwork(FriendlyByteBuf buf) {
            BlockPos center = buf.readBlockPos();
            int radius = buf.readVarInt();
            String name = buf.readUtf();
            ResourceLocation dim = buf.readBoolean() ? buf.readResourceLocation() : null;
            ResourceLocation biome = buf.readBoolean() ? buf.readResourceLocation() : null;
            return new ExploreTarget(center, radius, name.isEmpty() ? null : name, dim, biome);
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("x", center.getX());
            json.addProperty("y", center.getY());
            json.addProperty("z", center.getZ());
            json.addProperty("radius", radius);
            if (name != null) json.addProperty("name", name);
            if (dimension != null) json.addProperty("dimension", dimension.toString());
            if (biome != null) json.addProperty("biome", biome.toString());
            return json;
        }
        
        public static ExploreTarget fromJson(JsonObject json) {
            int x = json.get("x").getAsInt();
            int y = json.has("y") ? json.get("y").getAsInt() : 64;
            int z = json.get("z").getAsInt();
            int radius = json.has("radius") ? json.get("radius").getAsInt() : 16;
            String name = json.has("name") ? json.get("name").getAsString() : null;
            ResourceLocation dim = json.has("dimension")
                    ? new ResourceLocation(json.get("dimension").getAsString()) : null;
            ResourceLocation biome = json.has("biome")
                    ? new ResourceLocation(json.get("biome").getAsString()) : null;
            return new ExploreTarget(new BlockPos(x, y, z), radius, name, dim, biome);
        }
    }
}
