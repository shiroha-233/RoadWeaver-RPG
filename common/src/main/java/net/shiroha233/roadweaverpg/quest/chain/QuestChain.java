package net.shiroha233.roadweaverpg.quest.chain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 委托链定义
 * 定义一系列相关联的委托，形成故事线或任务线
 */
public class QuestChain {
    
    private final ResourceLocation id;
    private final String nameKey;
    private final String descriptionKey;
    
    // 委托链中的委托（按顺序）
    private final List<ResourceLocation> questSequence;
    
    // 是否为线性链（必须按顺序完成）
    private final boolean linear;
    
    // 完成整个链的额外奖励
    private final List<ResourceLocation> chainRewards;
    
    public QuestChain(ResourceLocation id, String nameKey, String descriptionKey,
                      List<ResourceLocation> questSequence, boolean linear,
                      List<ResourceLocation> chainRewards) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.questSequence = Collections.unmodifiableList(questSequence);
        this.linear = linear;
        this.chainRewards = Collections.unmodifiableList(chainRewards);
    }
    
    // region Getters
    public ResourceLocation getId() { return id; }
    public String getNameKey() { return nameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public List<ResourceLocation> getQuestSequence() { return questSequence; }
    public boolean isLinear() { return linear; }
    public List<ResourceLocation> getChainRewards() { return chainRewards; }
    
    public Component getName() {
        return Component.translatable(nameKey);
    }
    
    public Component getDescription() {
        return Component.translatable(descriptionKey);
    }
    
    public int getQuestCount() {
        return questSequence.size();
    }
    
    /** 获取委托在链中的索引 */
    public int getQuestIndex(ResourceLocation questId) {
        return questSequence.indexOf(questId);
    }
    
    /** 检查委托是否属于此链 */
    public boolean containsQuest(ResourceLocation questId) {
        return questSequence.contains(questId);
    }
    // endregion
    
    // region 序列化
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeUtf(nameKey);
        buf.writeUtf(descriptionKey);
        buf.writeCollection(questSequence, FriendlyByteBuf::writeResourceLocation);
        buf.writeBoolean(linear);
        buf.writeCollection(chainRewards, FriendlyByteBuf::writeResourceLocation);
    }
    
    public static QuestChain fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        String name = buf.readUtf();
        String desc = buf.readUtf();
        List<ResourceLocation> quests = buf.readList(FriendlyByteBuf::readResourceLocation);
        boolean linear = buf.readBoolean();
        List<ResourceLocation> rewards = buf.readList(FriendlyByteBuf::readResourceLocation);
        return new QuestChain(id, name, desc, quests, linear, rewards);
    }
    
    public static QuestChain fromJson(ResourceLocation id, JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() 
                : "chain." + id.getNamespace() + "." + id.getPath() + ".name";
        String desc = json.has("description") ? json.get("description").getAsString() 
                : "chain." + id.getNamespace() + "." + id.getPath() + ".description";
        
        List<ResourceLocation> quests = new ArrayList<>();
        if (json.has("quests") && json.get("quests").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("quests")) {
                quests.add(new ResourceLocation(elem.getAsString()));
            }
        }
        
        boolean linear = !json.has("linear") || json.get("linear").getAsBoolean();
        
        List<ResourceLocation> rewards = new ArrayList<>();
        if (json.has("chain_rewards") && json.get("chain_rewards").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("chain_rewards")) {
                rewards.add(new ResourceLocation(elem.getAsString()));
            }
        }
        
        return new QuestChain(id, name, desc, quests, linear, rewards);
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", nameKey);
        json.addProperty("description", descriptionKey);
        
        JsonArray questArray = new JsonArray();
        questSequence.forEach(q -> questArray.add(q.toString()));
        json.add("quests", questArray);
        
        json.addProperty("linear", linear);
        
        if (!chainRewards.isEmpty()) {
            JsonArray rewardArray = new JsonArray();
            chainRewards.forEach(r -> rewardArray.add(r.toString()));
            json.add("chain_rewards", rewardArray);
        }
        
        return json;
    }
    // endregion
}
