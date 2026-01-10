package net.shiroha233.roadweaverpg.profession;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.*;

/**
 * 职业定义 - 数据驱动的职业模板
 * 
 * 设计原理：
 * - 完全由数据包驱动，支持热重载
 * - 每个职业有独立的初始属性、成长曲线、技能点加成倍率
 * - 支持限制可加点的属性类型
 */
public class ProfessionDefinition {
    
    private final ResourceLocation id;
    private final Component name;
    private final Component description;
    private final ResourceLocation icon;
    
    // 基础属性（创建角色时的初始值）
    private final Map<StatType, Double> baseStats;
    
    // 每级成长属性
    private final Map<StatType, Double> statGrowth;
    
    // 技能点加成倍率（1.0为标准，>1.0为加成，<1.0为削弱）
    private final Map<StatType, Double> statBonusMultiplier;
    
    // 该职业可加点的属性列表
    private final Set<StatType> unlockedStats;
    
    // 初始物品
    private final List<StartingItem> startingItems;
    
    // 解锁条件
    private final int minAdventureLevel;
    private final Set<ResourceLocation> requiredProfessions; // 前置职业
    
    // 标签（用于分类和筛选）
    private final Set<String> tags;
    
    // 是否为默认职业（新玩家可直接选择）
    private final boolean defaultProfession;
    
    // 排序权重
    private final int sortOrder;
    
    private ProfessionDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.icon = builder.icon;
        this.baseStats = Collections.unmodifiableMap(new EnumMap<>(builder.baseStats));
        this.statGrowth = Collections.unmodifiableMap(new EnumMap<>(builder.statGrowth));
        this.statBonusMultiplier = Collections.unmodifiableMap(new EnumMap<>(builder.statBonusMultiplier));
        this.unlockedStats = Collections.unmodifiableSet(EnumSet.copyOf(builder.unlockedStats));
        this.startingItems = Collections.unmodifiableList(new ArrayList<>(builder.startingItems));
        this.minAdventureLevel = builder.minAdventureLevel;
        this.requiredProfessions = Collections.unmodifiableSet(new HashSet<>(builder.requiredProfessions));
        this.tags = Collections.unmodifiableSet(new HashSet<>(builder.tags));
        this.defaultProfession = builder.defaultProfession;
        this.sortOrder = builder.sortOrder;
    }

    // region Getters
    public ResourceLocation getId() { return id; }
    public Component getName() { return name; }
    public Component getDescription() { return description; }
    public ResourceLocation getIcon() { return icon; }
    public Map<StatType, Double> getBaseStats() { return baseStats; }
    public Map<StatType, Double> getStatGrowth() { return statGrowth; }
    public Map<StatType, Double> getStatBonusMultiplier() { return statBonusMultiplier; }
    public Set<StatType> getUnlockedStats() { return unlockedStats; }
    public List<StartingItem> getStartingItems() { return startingItems; }
    public int getMinAdventureLevel() { return minAdventureLevel; }
    public Set<ResourceLocation> getRequiredProfessions() { return requiredProfessions; }
    public Set<String> getTags() { return tags; }
    public boolean isDefaultProfession() { return defaultProfession; }
    public int getSortOrder() { return sortOrder; }
    // endregion
    
    /**
     * 获取指定属性的基础值
     */
    public double getBaseStat(StatType type) {
        return baseStats.getOrDefault(type, getDefaultBaseStat(type));
    }
    
    /**
     * 获取指定属性的每级成长值
     */
    public double getGrowth(StatType type) {
        return statGrowth.getOrDefault(type, 0.0);
    }
    
    /**
     * 获取指定属性的技能点加成倍率
     */
    public double getBonusMultiplier(StatType type) {
        return statBonusMultiplier.getOrDefault(type, 1.0);
    }
    
    /**
     * 检查属性是否可加点
     */
    public boolean isStatUnlocked(StatType type) {
        return unlockedStats.contains(type);
    }
    
    /**
     * 获取默认基础属性值
     */
    private static double getDefaultBaseStat(StatType type) {
        return switch (type) {
            case MAX_HEALTH -> 20.0;
            case MAX_MANA -> 100.0;
            case ATTACK -> 1.0;
            case DEFENSE -> 0.0;
            case MAGIC_ATTACK -> 1.0;
            case MAGIC_DEFENSE -> 0.0;
            case CRIT_RATE -> 5.0;
            case CRIT_DAMAGE -> 150.0;
            case HIT_RATE -> 100.0;
            case DODGE_RATE -> 0.0;
            case ATTACK_COOLDOWN -> 0.0;
            case MOVE_SPEED -> 100.0;
            case HEALTH_REGEN -> 0.0;
            case MANA_REGEN -> 1.0;
            default -> 0.0;
        };
    }
    
    // region 序列化
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeComponent(name);
        buf.writeComponent(description);
        buf.writeResourceLocation(icon);
        
        // 基础属性
        buf.writeVarInt(baseStats.size());
        baseStats.forEach((type, value) -> {
            buf.writeUtf(type.getId());
            buf.writeDouble(value);
        });
        
        // 成长属性
        buf.writeVarInt(statGrowth.size());
        statGrowth.forEach((type, value) -> {
            buf.writeUtf(type.getId());
            buf.writeDouble(value);
        });
        
        // 加成倍率
        buf.writeVarInt(statBonusMultiplier.size());
        statBonusMultiplier.forEach((type, value) -> {
            buf.writeUtf(type.getId());
            buf.writeDouble(value);
        });
        
        // 可加点属性
        buf.writeVarInt(unlockedStats.size());
        unlockedStats.forEach(type -> buf.writeUtf(type.getId()));
        
        // 初始物品
        buf.writeVarInt(startingItems.size());
        startingItems.forEach(item -> item.toNetwork(buf));
        
        // 解锁条件
        buf.writeVarInt(minAdventureLevel);
        buf.writeVarInt(requiredProfessions.size());
        requiredProfessions.forEach(buf::writeResourceLocation);
        
        // 标签
        buf.writeVarInt(tags.size());
        tags.forEach(buf::writeUtf);
        
        buf.writeBoolean(defaultProfession);
        buf.writeVarInt(sortOrder);
    }

    public static ProfessionDefinition fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        Component name = buf.readComponent();
        Component description = buf.readComponent();
        ResourceLocation icon = buf.readResourceLocation();
        
        Builder builder = new Builder(id)
                .name(name)
                .description(description)
                .icon(icon);
        
        // 基础属性
        int baseCount = buf.readVarInt();
        for (int i = 0; i < baseCount; i++) {
            StatType type = StatType.fromId(buf.readUtf());
            double value = buf.readDouble();
            if (type != null) builder.baseStat(type, value);
        }
        
        // 成长属性
        int growthCount = buf.readVarInt();
        for (int i = 0; i < growthCount; i++) {
            StatType type = StatType.fromId(buf.readUtf());
            double value = buf.readDouble();
            if (type != null) builder.statGrowth(type, value);
        }
        
        // 加成倍率
        int multiplierCount = buf.readVarInt();
        for (int i = 0; i < multiplierCount; i++) {
            StatType type = StatType.fromId(buf.readUtf());
            double value = buf.readDouble();
            if (type != null) builder.statBonusMultiplier(type, value);
        }
        
        // 可加点属性
        int unlockedCount = buf.readVarInt();
        for (int i = 0; i < unlockedCount; i++) {
            StatType type = StatType.fromId(buf.readUtf());
            if (type != null) builder.unlockStat(type);
        }
        
        // 初始物品
        int itemCount = buf.readVarInt();
        for (int i = 0; i < itemCount; i++) {
            builder.startingItem(StartingItem.fromNetwork(buf));
        }
        
        // 解锁条件
        builder.minAdventureLevel(buf.readVarInt());
        int reqCount = buf.readVarInt();
        for (int i = 0; i < reqCount; i++) {
            builder.requiredProfession(buf.readResourceLocation());
        }
        
        // 标签
        int tagCount = buf.readVarInt();
        for (int i = 0; i < tagCount; i++) {
            builder.tag(buf.readUtf());
        }
        
        builder.defaultProfession(buf.readBoolean());
        builder.sortOrder(buf.readVarInt());
        
        return builder.build();
    }
    
    public static ProfessionDefinition fromJson(ResourceLocation id, JsonObject json) {
        Builder builder = new Builder(id);
        
        // 名称和描述
        if (json.has("name")) {
            builder.name(Component.translatable(json.get("name").getAsString()));
        }
        if (json.has("description")) {
            builder.description(Component.translatable(json.get("description").getAsString()));
        }
        if (json.has("icon")) {
            builder.icon(new ResourceLocation(json.get("icon").getAsString()));
        }
        
        // 基础属性
        if (json.has("base_stats")) {
            JsonObject stats = json.getAsJsonObject("base_stats");
            for (String key : stats.keySet()) {
                StatType type = StatType.fromId(key);
                if (type != null) {
                    builder.baseStat(type, stats.get(key).getAsDouble());
                }
            }
        }
        
        // 成长属性
        if (json.has("stat_growth")) {
            JsonObject growth = json.getAsJsonObject("stat_growth");
            for (String key : growth.keySet()) {
                StatType type = StatType.fromId(key);
                if (type != null) {
                    builder.statGrowth(type, growth.get(key).getAsDouble());
                }
            }
        }
        
        // 加成倍率
        if (json.has("stat_bonus_multiplier")) {
            JsonObject multiplier = json.getAsJsonObject("stat_bonus_multiplier");
            for (String key : multiplier.keySet()) {
                StatType type = StatType.fromId(key);
                if (type != null) {
                    builder.statBonusMultiplier(type, multiplier.get(key).getAsDouble());
                }
            }
        }
        
        // 可加点属性
        if (json.has("unlocked_stats")) {
            JsonArray arr = json.getAsJsonArray("unlocked_stats");
            for (JsonElement elem : arr) {
                StatType type = StatType.fromId(elem.getAsString());
                if (type != null) builder.unlockStat(type);
            }
        }
        
        // 初始物品
        if (json.has("starting_items")) {
            JsonArray items = json.getAsJsonArray("starting_items");
            for (JsonElement elem : items) {
                builder.startingItem(StartingItem.fromJson(elem.getAsJsonObject()));
            }
        }
        
        // 解锁条件
        if (json.has("requirements")) {
            JsonObject req = json.getAsJsonObject("requirements");
            if (req.has("min_adventure_level")) {
                builder.minAdventureLevel(req.get("min_adventure_level").getAsInt());
            }
            if (req.has("required_professions")) {
                JsonArray profs = req.getAsJsonArray("required_professions");
                for (JsonElement elem : profs) {
                    builder.requiredProfession(new ResourceLocation(elem.getAsString()));
                }
            }
        }
        
        // 标签
        if (json.has("tags")) {
            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement elem : tags) {
                builder.tag(elem.getAsString());
            }
        }
        
        if (json.has("default")) {
            builder.defaultProfession(json.get("default").getAsBoolean());
        }
        if (json.has("sort_order")) {
            builder.sortOrder(json.get("sort_order").getAsInt());
        }
        
        return builder.build();
    }
    // endregion

    // region Builder
    public static class Builder {
        private final ResourceLocation id;
        private Component name;
        private Component description;
        private ResourceLocation icon;
        private final Map<StatType, Double> baseStats = new EnumMap<>(StatType.class);
        private final Map<StatType, Double> statGrowth = new EnumMap<>(StatType.class);
        private final Map<StatType, Double> statBonusMultiplier = new EnumMap<>(StatType.class);
        private final Set<StatType> unlockedStats = EnumSet.noneOf(StatType.class);
        private final List<StartingItem> startingItems = new ArrayList<>();
        private int minAdventureLevel = 1;
        private final Set<ResourceLocation> requiredProfessions = new HashSet<>();
        private final Set<String> tags = new HashSet<>();
        private boolean defaultProfession = false;
        private int sortOrder = 0;
        
        public Builder(ResourceLocation id) {
            this.id = id;
            this.name = Component.literal(id.getPath());
            this.description = Component.empty();
            this.icon = new ResourceLocation(id.getNamespace(), 
                    "textures/gui/profession/" + id.getPath() + ".png");
            
            // 默认解锁所有可分配属性
            for (StatType type : StatType.values()) {
                if (type.isAllocatable()) {
                    unlockedStats.add(type);
                }
            }
        }
        
        public Builder name(Component name) { this.name = name; return this; }
        public Builder description(Component desc) { this.description = desc; return this; }
        public Builder icon(ResourceLocation icon) { this.icon = icon; return this; }
        
        public Builder baseStat(StatType type, double value) {
            baseStats.put(type, value);
            return this;
        }
        
        public Builder statGrowth(StatType type, double value) {
            statGrowth.put(type, value);
            return this;
        }
        
        public Builder statBonusMultiplier(StatType type, double value) {
            statBonusMultiplier.put(type, value);
            return this;
        }
        
        public Builder unlockStat(StatType type) {
            unlockedStats.add(type);
            return this;
        }
        
        public Builder lockStat(StatType type) {
            unlockedStats.remove(type);
            return this;
        }
        
        public Builder clearUnlockedStats() {
            unlockedStats.clear();
            return this;
        }
        
        public Builder startingItem(StartingItem item) {
            startingItems.add(item);
            return this;
        }
        
        public Builder minAdventureLevel(int level) {
            this.minAdventureLevel = level;
            return this;
        }
        
        public Builder requiredProfession(ResourceLocation profId) {
            requiredProfessions.add(profId);
            return this;
        }
        
        public Builder tag(String tag) {
            tags.add(tag);
            return this;
        }
        
        public Builder defaultProfession(boolean isDefault) {
            this.defaultProfession = isDefault;
            return this;
        }
        
        public Builder sortOrder(int order) {
            this.sortOrder = order;
            return this;
        }
        
        public ProfessionDefinition build() {
            return new ProfessionDefinition(this);
        }
    }
    // endregion
    
    /**
     * 初始物品定义
     */
    public record StartingItem(ResourceLocation itemId, int count, String nbt) {
        
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeResourceLocation(itemId);
            buf.writeVarInt(count);
            buf.writeUtf(nbt != null ? nbt : "");
        }
        
        public static StartingItem fromNetwork(FriendlyByteBuf buf) {
            ResourceLocation itemId = buf.readResourceLocation();
            int count = buf.readVarInt();
            String nbt = buf.readUtf();
            return new StartingItem(itemId, count, nbt.isEmpty() ? null : nbt);
        }
        
        public static StartingItem fromJson(JsonObject json) {
            ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            String nbt = json.has("nbt") ? json.get("nbt").getAsString() : null;
            return new StartingItem(itemId, count, nbt);
        }
    }
}
