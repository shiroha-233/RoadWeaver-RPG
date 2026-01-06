package net.shiroha233.roadweaverpg.quest.type;

/**
 * 委托状态枚举
 */
public enum QuestState {
    AVAILABLE("available"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    TURNED_IN("turned_in"),
    FAILED("failed"),
    EXPIRED("expired"),
    ABANDONED("abandoned");
    
    private final String serializedName;
    
    QuestState(String serializedName) {
        this.serializedName = serializedName;
    }
    
    public String getSerializedName() { return serializedName; }
    
    public boolean isTerminal() {
        return this == TURNED_IN || this == FAILED || this == EXPIRED || this == ABANDONED;
    }
    
    public boolean isActive() {
        return this == IN_PROGRESS || this == COMPLETED;
    }
    
    public static QuestState fromString(String name) {
        for (QuestState state : values()) {
            if (state.serializedName.equalsIgnoreCase(name) || state.name().equalsIgnoreCase(name)) {
                return state;
            }
        }
        return AVAILABLE;
    }
}
