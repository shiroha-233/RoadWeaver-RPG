package net.shiroha233.roadweaverpg.worlddifficulty;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 难度缩放配置
 * 
 * 设计原理：
 * - 定义每级冒险等级对怪物属性的影响
 * - 支持全局配置和指定怪物配置
 * - 数据驱动，从JSON加载
 */
public record DifficultyScaling(
        ResourceLocation id,
        boolean isGlobal,                    // 是否为全局配置
        List<ResourceLocation> targetMobs,  // 指定怪物列表（非全局时使用）
        
        // 每级属性增量
        float armorPerLevel,
        float healthPerLevel,
        float resistancePerLevel,
        float healthRegenPerLevel,
        float physicalResistPerLevel,
        float magicResistPerLevel,
        float damagePerLevel,
        
        // 稀有度生成权重
        float eliteChance,
        float bossChance,
        float legendaryChance,
        
        // 环境加成列表
        List<EnvironmentBonus> environmentBonuses
) {
    
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeBoolean(isGlobal);
        buf.writeVarInt(targetMobs.size());
        for (ResourceLocation mob : targetMobs) {
            buf.writeResourceLocation(mob);
        }
        buf.writeFloat(armorPerLevel);
        buf.writeFloat(healthPerLevel);
        buf.writeFloat(resistancePerLevel);
        buf.writeFloat(healthRegenPerLevel);
        buf.writeFloat(physicalResistPerLevel);
        buf.writeFloat(magicResistPerLevel);
        buf.writeFloat(damagePerLevel);
        buf.writeFloat(eliteChance);
        buf.writeFloat(bossChance);
        buf.writeFloat(legendaryChance);
        buf.writeVarInt(environmentBonuses.size());
        for (EnvironmentBonus bonus : environmentBonuses) {
            bonus.toNetwork(buf);
        }
    }
    
    public static DifficultyScaling fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        boolean isGlobal = buf.readBoolean();
        int mobCount = buf.readVarInt();
        List<ResourceLocation> mobs = new ArrayList<>();
        for (int i = 0; i < mobCount; i++) {
            mobs.add(buf.readResourceLocation());
        }
        float armor = buf.readFloat();
        float health = buf.readFloat();
        float resistance = buf.readFloat();
        float healthRegen = buf.readFloat();
        float physResist = buf.readFloat();
        float magicResist = buf.readFloat();
        float damage = buf.readFloat();
        float elite = buf.readFloat();
        float boss = buf.readFloat();
        float legendary = buf.readFloat();
        int bonusCount = buf.readVarInt();
        List<EnvironmentBonus> bonuses = new ArrayList<>();
        for (int i = 0; i < bonusCount; i++) {
            bonuses.add(EnvironmentBonus.fromNetwork(buf));
        }
        return new DifficultyScaling(id, isGlobal, mobs, armor, health, resistance,
                healthRegen, physResist, magicResist, damage, elite, boss, legendary, bonuses);
    }
    
    public static DifficultyScaling fromJson(ResourceLocation id, JsonObject json) {
        boolean isGlobal = json.has("global") && json.get("global").getAsBoolean();
        
        List<ResourceLocation> mobs = new ArrayList<>();
        if (json.has("target_mobs") && json.get("target_mobs").isJsonArray()) {
            for (var elem : json.getAsJsonArray("target_mobs")) {
                mobs.add(new ResourceLocation(elem.getAsString()));
            }
        }
        
        // 每级属性增量
        JsonObject perLevel = json.has("per_level") ? json.getAsJsonObject("per_level") : new JsonObject();
        float armor = perLevel.has("armor") ? perLevel.get("armor").getAsFloat() : 0.5f;
        float health = perLevel.has("health") ? perLevel.get("health").getAsFloat() : 2.0f;
        float resistance = perLevel.has("resistance") ? perLevel.get("resistance").getAsFloat() : 0.02f;
        float healthRegen = perLevel.has("health_regen") ? perLevel.get("health_regen").getAsFloat() : 0.1f;
        float physResist = perLevel.has("physical_resistance") ? perLevel.get("physical_resistance").getAsFloat() : 0.01f;
        float magicResist = perLevel.has("magic_resistance") ? perLevel.get("magic_resistance").getAsFloat() : 0.01f;
        float damage = perLevel.has("damage") ? perLevel.get("damage").getAsFloat() : 0.5f;
        
        // 稀有度生成概率
        JsonObject rarityChance = json.has("rarity_chance") ? json.getAsJsonObject("rarity_chance") : new JsonObject();
        float elite = rarityChance.has("elite") ? rarityChance.get("elite").getAsFloat() : 0.1f;
        float boss = rarityChance.has("boss") ? rarityChance.get("boss").getAsFloat() : 0.02f;
        float legendary = rarityChance.has("legendary") ? rarityChance.get("legendary").getAsFloat() : 0.005f;
        
        // 环境加成
        List<EnvironmentBonus> bonuses = new ArrayList<>();
        if (json.has("environment_bonuses") && json.get("environment_bonuses").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("environment_bonuses");
            for (var elem : arr) {
                if (elem.isJsonObject()) {
                    bonuses.add(EnvironmentBonus.fromJson(elem.getAsJsonObject()));
                }
            }
        }
        
        return new DifficultyScaling(id, isGlobal, Collections.unmodifiableList(mobs),
                armor, health, resistance, healthRegen, physResist, magicResist, damage,
                elite, boss, legendary, Collections.unmodifiableList(bonuses));
    }
    
    /**
     * 创建默认全局配置
     */
    public static DifficultyScaling createDefault() {
        return new DifficultyScaling(
                new ResourceLocation("roadweaver_rpg", "default"),
                true, List.of(),
                0.5f, 2.0f, 0.02f, 0.1f, 0.01f, 0.01f, 0.5f,
                0.1f, 0.02f, 0.005f, List.of()
        );
    }
}
