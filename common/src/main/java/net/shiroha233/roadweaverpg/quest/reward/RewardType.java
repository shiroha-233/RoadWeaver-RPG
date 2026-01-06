package net.shiroha233.roadweaverpg.quest.reward;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 奖励类型枚举
 */
public enum RewardType {
    ITEM("item"),
    EXPERIENCE("experience"),
    REPUTATION("reputation"),
    UNLOCK_QUEST("unlock_quest"),
    COMMAND("command"),
    COIN("coin");
    
    private final String serializedName;
    
    RewardType(String serializedName) {
        this.serializedName = serializedName;
    }
    
    public String getSerializedName() { return serializedName; }
    
    public ResourceLocation getRegistryId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, serializedName);
    }
    
    public static RewardType fromString(String name) {
        for (RewardType type : values()) {
            if (type.serializedName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return ITEM;
    }
}
