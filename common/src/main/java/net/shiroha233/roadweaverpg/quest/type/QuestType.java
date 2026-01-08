package net.shiroha233.roadweaverpg.quest.type;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 委托类型枚举
 */
public enum QuestType {
    COLLECT("collect", true, false),
    KILL("kill", true, false),
    LOCATION_KILL("location_kill", true, true),  // 定点击杀
    EXPLORE("explore", false, true),
    ESCORT("escort", false, true),
    BUILD("build", true, false),
    DELIVERY("delivery", true, false),
    TALK("talk", false, false),
    COMPOSITE("composite", false, false);
    
    private final String serializedName;
    private final boolean requiresCount;
    private final boolean requiresLocation;
    
    QuestType(String serializedName, boolean requiresCount, boolean requiresLocation) {
        this.serializedName = serializedName;
        this.requiresCount = requiresCount;
        this.requiresLocation = requiresLocation;
    }
    
    public String getSerializedName() { return serializedName; }
    public boolean requiresCount() { return requiresCount; }
    public boolean requiresLocation() { return requiresLocation; }
    
    public ResourceLocation getRegistryId() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, serializedName);
    }
    
    public static QuestType fromString(String name) {
        for (QuestType type : values()) {
            if (type.serializedName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return COLLECT;
    }
}
