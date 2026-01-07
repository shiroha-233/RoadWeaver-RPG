package net.shiroha233.roadweaverpg.config;

/**
 * 委托系统配置
 * 
 * 设计原则：
 * - 集中管理所有配置常量
 * - 避免魔法数字分散在代码中
 * - 便于调整和维护
 */
public final class QuestSystemConfig {
    
    private QuestSystemConfig() {}
    
    // ========== 性能相关 ==========
    
    /** 目标检查间隔（tick） */
    public static final int OBJECTIVE_CHECK_INTERVAL = 20; // 每秒
    
    /** 数据同步验证间隔（毫秒） */
    public static final long SYNC_VALIDATION_INTERVAL = 60000; // 1分钟
    
    /** 收集类目标缓存时间（毫秒） */
    public static final long COLLECT_CACHE_DURATION = 1000; // 1秒
    
    // ========== 每日委托相关 ==========
    
    /** D级每日委托数量 */
    public static final int DAILY_QUEST_COUNT_D = 3;
    
    /** C级每日委托数量 */
    public static final int DAILY_QUEST_COUNT_C = 3;
    
    /** B级每日委托数量 */
    public static final int DAILY_QUEST_COUNT_B = 2;
    
    /** A级每日委托数量 */
    public static final int DAILY_QUEST_COUNT_A = 2;
    
    /** S级每日委托数量 */
    public static final int DAILY_QUEST_COUNT_S = 1;
    
    // ========== 委托限制 ==========
    
    /** 最大同时活跃委托数量 */
    public static final int MAX_ACTIVE_QUESTS = 10;
    
    /** 默认委托时间限制（秒，0表示无限制） */
    public static final int DEFAULT_TIME_LIMIT = 0;
    
    /** 默认委托冷却时间（秒） */
    public static final int DEFAULT_COOLDOWN = 0;
    
    // ========== 难度系统 ==========
    
    /** 基础难度倍数 */
    public static final float BASE_DIFFICULTY = 1.0f;
    
    /** 每级增加的难度百分比 */
    public static final float DIFFICULTY_PER_LEVEL = 0.01f; // 1%
    
    /** 每次重复增加的难度百分比 */
    public static final float DIFFICULTY_PER_REPEAT = 0.1f; // 10%
    
    // ========== 声望系统 ==========
    
    /** 默认声望派系ID */
    public static final String DEFAULT_FACTION = "roadweaver_rpg:guild";
    
    /** 声望等级升级时的通知范围（格） */
    public static final int REPUTATION_LEVEL_UP_NOTIFY_RANGE = 32;
    
    // ========== 调试相关 ==========
    
    /** 是否启用详细日志 */
    public static boolean ENABLE_VERBOSE_LOGGING = false;
    
    /** 是否启用性能监控 */
    public static boolean ENABLE_PERFORMANCE_MONITORING = false;
    
    /** 是否启用数据一致性检查 */
    public static boolean ENABLE_CONSISTENCY_CHECK = true;
    
    // ========== 网络相关 ==========
    
    /** 数据包发送超时（毫秒） */
    public static final int PACKET_TIMEOUT = 5000;
    
    /** 最大数据包大小（字节） */
    public static final int MAX_PACKET_SIZE = 32768; // 32KB
    
    // ========== 事务相关 ==========
    
    /** 事务超时时间（毫秒） */
    public static final long TRANSACTION_TIMEOUT = 10000; // 10秒
    
    /** 是否启用事务回滚日志 */
    public static boolean ENABLE_TRANSACTION_ROLLBACK_LOG = true;
}
