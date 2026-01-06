package net.shiroha233.roadweaverpg.common;

/**
 * 模组常量定义
 */
public final class Constants {
    
    private Constants() {}
    
    public static final String MOD_ID = "roadweaver_rpg";
    public static final String MOD_NAME = "RoadWeaver RPG";
    
    public static final String QUESTS_DIRECTORY = "quests";
    public static final String REPUTATION_DIRECTORY = "reputation_levels";
    public static final String NETWORK_PROTOCOL_VERSION = "1";
    
    public static final int TICKS_PER_SECOND = 20;
    public static final int QUEST_CHECK_INTERVAL = TICKS_PER_SECOND;
    public static final int CACHE_TTL_TICKS = TICKS_PER_SECOND * 5;
    
    public static final float DEFAULT_DIFFICULTY = 1.0f;
    public static final int DEFAULT_COOLDOWN = 0;
    public static final int DEFAULT_TIME_LIMIT = 0;
    
    public static final class NBT {
        public static final String QUEST_ID = "QuestId";
        public static final String INSTANCE_ID = "InstanceId";
        public static final String STATE = "State";
        public static final String PLAYER_ID = "PlayerId";
        public static final String ACCEPTED_TIME = "AcceptedTime";
        public static final String COMPLETED_TIME = "CompletedTime";
        public static final String EXPIRATION_TIME = "ExpirationTime";
        public static final String DIFFICULTY = "Difficulty";
        public static final String PROGRESS = "Progress";
        public static final String OBJECTIVES = "Objectives";
        public static final String REWARDS = "Rewards";
        private NBT() {}
    }
    
    public static final class Events {
        public static final String ENTITY_KILL = "entity_kill";
        public static final String BLOCK_BREAK = "block_break";
        public static final String ITEM_PICKUP = "item_pickup";
        public static final String INVENTORY_CHECK = "inventory_check";
        public static final String PLAYER_MOVE = "player_move";
        private Events() {}
    }
    
    public static final class TranslationKeys {
        public static final String QUEST_PREFIX = "quest." + MOD_ID + ".";
        public static final String GUI_PREFIX = "gui." + MOD_ID + ".";
        public static final String MESSAGE_PREFIX = "message." + MOD_ID + ".";
        public static final String ITEM_PREFIX = "item." + MOD_ID + ".";
        private TranslationKeys() {}
    }
}
