package net.shiroha233.roadweaverpg.quest.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.type.QuestRank;
import net.shiroha233.roadweaverpg.quest.type.QuestType;
import net.shiroha233.roadweaverpg.quest.objective.ObjectiveRegistry;
import net.shiroha233.roadweaverpg.quest.objective.QuestObjective;
import net.shiroha233.roadweaverpg.quest.reward.QuestReward;
import net.shiroha233.roadweaverpg.quest.reward.RewardRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 委托定义 - 数据驱动的委托模板
 */
public class QuestDefinition {
    
    private final ResourceLocation id;
    private final Component title;
    private final Component description;
    private final QuestRank rank;
    private final QuestType primaryType;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final int timeLimit;
    private final List<ResourceLocation> prerequisites;
    private final List<ResourceLocation> unlocks;
    private final boolean repeatable;
    private final int cooldown;
    private final float baseDifficulty;
    
    // 声望等级限制
    private final int minReputationLevel;
    private final ResourceLocation reputationFaction;
    
    // 每日委托相关
    private final boolean dailyQuest;
    private final int dailyWeight;
    
    // 领取限制相关
    private final boolean oneTime;              // 单次任务（完成后永不刷新）
    private final int maxAcceptPerPeriod;       // 周期内最大领取次数（0=无限制）
    private final int acceptPeriodSeconds;      // 领取周期（秒，0=无周期限制）
    
    private QuestDefinition(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.rank = builder.rank;
        this.primaryType = builder.primaryType;
        this.objectives = Collections.unmodifiableList(builder.objectives);
        this.rewards = Collections.unmodifiableList(builder.rewards);
        this.timeLimit = builder.timeLimit;
        this.prerequisites = Collections.unmodifiableList(builder.prerequisites);
        this.unlocks = Collections.unmodifiableList(builder.unlocks);
        this.repeatable = builder.repeatable;
        this.cooldown = builder.cooldown;
        this.baseDifficulty = builder.baseDifficulty;
        this.minReputationLevel = builder.minReputationLevel;
        this.reputationFaction = builder.reputationFaction;
        this.dailyQuest = builder.dailyQuest;
        this.dailyWeight = builder.dailyWeight;
        this.oneTime = builder.oneTime;
        this.maxAcceptPerPeriod = builder.maxAcceptPerPeriod;
        this.acceptPeriodSeconds = builder.acceptPeriodSeconds;
    }
    
    // region Getters
    public ResourceLocation getId() { return id; }
    public Component getTitle() { return title; }
    public Component getDescription() { return description; }
    public QuestRank getRank() { return rank; }
    public QuestType getPrimaryType() { return primaryType; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public List<QuestReward> getRewards() { return rewards; }
    public int getTimeLimit() { return timeLimit; }
    public List<ResourceLocation> getPrerequisites() { return prerequisites; }
    public List<ResourceLocation> getUnlocks() { return unlocks; }
    public boolean isRepeatable() { return repeatable; }
    public int getCooldown() { return cooldown; }
    public float getBaseDifficulty() { return baseDifficulty; }
    
    // 声望等级限制
    public int getMinReputationLevel() { return minReputationLevel; }
    public ResourceLocation getReputationFaction() { return reputationFaction; }
    public boolean hasReputationRequirement() { return minReputationLevel > 0; }
    
    // 每日委托
    public boolean isDailyQuest() { return dailyQuest; }
    public int getDailyWeight() { return dailyWeight; }
    
    // 领取限制
    public boolean isOneTime() { return oneTime; }
    public int getMaxAcceptPerPeriod() { return maxAcceptPerPeriod; }
    public int getAcceptPeriodSeconds() { return acceptPeriodSeconds; }
    public boolean hasAcceptLimit() { return maxAcceptPerPeriod > 0 && acceptPeriodSeconds > 0; }
    
    public Component getDisplayTitle() {
        return Component.literal("[" + rank.getDisplayName() + "] ")
                .withStyle(rank.getColor())
                .append(getTitle());
    }
    
    public boolean hasTimeLimit() { return timeLimit > 0; }
    public boolean hasPrerequisites() { return !prerequisites.isEmpty(); }
    // endregion
    
    // region 序列化
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeComponent(title);
        buf.writeComponent(description);
        buf.writeEnum(rank);
        buf.writeEnum(primaryType);
        
        buf.writeVarInt(objectives.size());
        for (QuestObjective obj : objectives) {
            ObjectiveRegistry.toNetwork(obj, buf);
        }
        
        buf.writeVarInt(rewards.size());
        for (QuestReward reward : rewards) {
            RewardRegistry.toNetwork(reward, buf);
        }
        
        buf.writeVarInt(timeLimit);
        buf.writeCollection(prerequisites, FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(unlocks, FriendlyByteBuf::writeResourceLocation);
        buf.writeBoolean(repeatable);
        buf.writeVarInt(cooldown);
        buf.writeFloat(baseDifficulty);
        
        // 声望等级限制
        buf.writeVarInt(minReputationLevel);
        buf.writeBoolean(reputationFaction != null);
        if (reputationFaction != null) {
            buf.writeResourceLocation(reputationFaction);
        }
        
        // 每日委托
        buf.writeBoolean(dailyQuest);
        buf.writeVarInt(dailyWeight);
        
        // 领取限制
        buf.writeBoolean(oneTime);
        buf.writeVarInt(maxAcceptPerPeriod);
        buf.writeVarInt(acceptPeriodSeconds);
    }
    
    public static QuestDefinition fromNetwork(FriendlyByteBuf buf) {
        Builder builder = new Builder(buf.readResourceLocation())
                .title(buf.readComponent())
                .description(buf.readComponent())
                .rank(buf.readEnum(QuestRank.class))
                .primaryType(buf.readEnum(QuestType.class));
        
        int objCount = buf.readVarInt();
        for (int i = 0; i < objCount; i++) {
            QuestObjective obj = ObjectiveRegistry.fromNetwork(buf);
            if (obj != null) builder.addObjective(obj);
        }
        
        int rewardCount = buf.readVarInt();
        for (int i = 0; i < rewardCount; i++) {
            QuestReward reward = RewardRegistry.fromNetwork(buf);
            if (reward != null) builder.addReward(reward);
        }
        
        builder.timeLimit(buf.readVarInt());
        buf.readList(FriendlyByteBuf::readResourceLocation).forEach(builder::addPrerequisite);
        buf.readList(FriendlyByteBuf::readResourceLocation).forEach(builder::addUnlock);
        builder.repeatable(buf.readBoolean());
        builder.cooldown(buf.readVarInt());
        builder.baseDifficulty(buf.readFloat());
        
        // 声望等级限制
        builder.minReputationLevel(buf.readVarInt());
        if (buf.readBoolean()) {
            builder.reputationFaction(buf.readResourceLocation());
        }
        
        // 每日委托
        builder.dailyQuest(buf.readBoolean());
        builder.dailyWeight(buf.readVarInt());
        
        // 领取限制
        builder.oneTime(buf.readBoolean());
        builder.maxAcceptPerPeriod(buf.readVarInt());
        builder.acceptPeriodSeconds(buf.readVarInt());
        
        return builder.build();
    }
    // endregion
    
    // region JSON解析
    public static QuestDefinition fromJson(ResourceLocation id, JsonObject json) {
        Builder builder = new Builder(id);
        
        if (json.has("title")) {
            builder.title(parseTextComponent(json.get("title")));
        }
        if (json.has("description")) {
            builder.description(parseTextComponent(json.get("description")));
        }
        if (json.has("rank")) {
            builder.rank(QuestRank.fromString(json.get("rank").getAsString()));
        }
        if (json.has("type")) {
            builder.primaryType(QuestType.fromString(json.get("type").getAsString()));
        }
        
        if (json.has("objectives") && json.get("objectives").isJsonArray()) {
            JsonArray objArray = json.getAsJsonArray("objectives");
            int index = 0;
            for (JsonElement elem : objArray) {
                if (elem.isJsonObject()) {
                    String objId = id.getPath() + "_obj_" + index++;
                    QuestObjective obj = ObjectiveRegistry.fromJson(objId, elem.getAsJsonObject());
                    if (obj != null) builder.addObjective(obj);
                }
            }
        }
        
        if (json.has("rewards") && json.get("rewards").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("rewards")) {
                if (elem.isJsonObject()) {
                    QuestReward reward = RewardRegistry.fromJson(elem.getAsJsonObject());
                    if (reward != null) builder.addReward(reward);
                }
            }
        }
        
        if (json.has("time_limit")) builder.timeLimit(json.get("time_limit").getAsInt());
        if (json.has("repeatable")) builder.repeatable(json.get("repeatable").getAsBoolean());
        if (json.has("cooldown")) builder.cooldown(json.get("cooldown").getAsInt());
        if (json.has("difficulty")) builder.baseDifficulty(json.get("difficulty").getAsFloat());
        
        // 声望等级限制
        if (json.has("min_reputation_level")) {
            builder.minReputationLevel(json.get("min_reputation_level").getAsInt());
        }
        if (json.has("reputation_faction")) {
            builder.reputationFaction(new ResourceLocation(json.get("reputation_faction").getAsString()));
        }
        
        // 每日委托
        if (json.has("daily_quest")) builder.dailyQuest(json.get("daily_quest").getAsBoolean());
        if (json.has("daily_weight")) builder.dailyWeight(json.get("daily_weight").getAsInt());
        
        // 领取限制
        if (json.has("one_time")) builder.oneTime(json.get("one_time").getAsBoolean());
        if (json.has("max_accept_per_period")) builder.maxAcceptPerPeriod(json.get("max_accept_per_period").getAsInt());
        if (json.has("accept_period_seconds")) builder.acceptPeriodSeconds(json.get("accept_period_seconds").getAsInt());
        
        if (json.has("prerequisites") && json.get("prerequisites").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("prerequisites")) {
                builder.addPrerequisite(new ResourceLocation(elem.getAsString()));
            }
        }
        if (json.has("unlocks") && json.get("unlocks").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("unlocks")) {
                builder.addUnlock(new ResourceLocation(elem.getAsString()));
            }
        }
        
        return builder.build();
    }
    
    private static Component parseTextComponent(JsonElement element) {
        if (element.isJsonObject()) {
            return Component.Serializer.fromJson(element);
        }
        String text = element.getAsString();
        if (text.startsWith("translate:")) {
            return Component.translatable(text.substring(10));
        }
        return Component.literal(text);
    }
    // endregion
    
    public static class Builder {
        private final ResourceLocation id;
        private Component title;
        private Component description;
        private QuestRank rank = QuestRank.D;
        private QuestType primaryType = QuestType.COLLECT;
        private final List<QuestObjective> objectives = new ArrayList<>();
        private final List<QuestReward> rewards = new ArrayList<>();
        private int timeLimit = 0;
        private final List<ResourceLocation> prerequisites = new ArrayList<>();
        private final List<ResourceLocation> unlocks = new ArrayList<>();
        private boolean repeatable = false;
        private int cooldown = 0;
        private float baseDifficulty = 1.0f;
        
        // 声望等级限制
        private int minReputationLevel = 0;
        private ResourceLocation reputationFaction = null;
        
        // 每日委托
        private boolean dailyQuest = false;
        private int dailyWeight = 1;
        
        // 领取限制
        private boolean oneTime = false;
        private int maxAcceptPerPeriod = 0;
        private int acceptPeriodSeconds = 0;
        
        public Builder(ResourceLocation id) {
            this.id = id;
            this.title = Component.literal("Unnamed Quest");
            this.description = Component.literal("");
        }
        
        public Builder title(Component title) { this.title = title; return this; }
        public Builder description(Component desc) { this.description = desc; return this; }
        public Builder rank(QuestRank rank) { this.rank = rank; return this; }
        public Builder primaryType(QuestType type) { this.primaryType = type; return this; }
        public Builder addObjective(QuestObjective obj) { this.objectives.add(obj); return this; }
        public Builder addReward(QuestReward reward) { this.rewards.add(reward); return this; }
        public Builder timeLimit(int seconds) { this.timeLimit = seconds; return this; }
        public Builder addPrerequisite(ResourceLocation id) { this.prerequisites.add(id); return this; }
        public Builder addUnlock(ResourceLocation id) { this.unlocks.add(id); return this; }
        public Builder repeatable(boolean val) { this.repeatable = val; return this; }
        public Builder cooldown(int seconds) { this.cooldown = seconds; return this; }
        public Builder baseDifficulty(float val) { this.baseDifficulty = val; return this; }
        
        // 声望等级限制
        public Builder minReputationLevel(int level) { this.minReputationLevel = level; return this; }
        public Builder reputationFaction(ResourceLocation faction) { this.reputationFaction = faction; return this; }
        
        // 每日委托
        public Builder dailyQuest(boolean val) { this.dailyQuest = val; return this; }
        public Builder dailyWeight(int weight) { this.dailyWeight = weight; return this; }
        
        // 领取限制
        public Builder oneTime(boolean val) { this.oneTime = val; return this; }
        public Builder maxAcceptPerPeriod(int max) { this.maxAcceptPerPeriod = max; return this; }
        public Builder acceptPeriodSeconds(int seconds) { this.acceptPeriodSeconds = seconds; return this; }
        
        public QuestDefinition build() {
            return new QuestDefinition(this);
        }
    }
}
