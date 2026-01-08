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
    
    // ========== 奖励系统相关 ==========
    
    /** 奖励队列批量处理大小 */
    public static final int REWARD_BATCH_SIZE = 50;
    
    /** 奖励历史记录保留时间（毫秒） */
    public static final long REWARD_HISTORY_RETENTION = 3600_000L; // 1小时
    
    /** 奖励最大重试次数 */
    public static final int REWARD_MAX_RETRIES = 10;
    
    /** 奖励重试基础间隔（毫秒） */
    public static final long REWARD_RETRY_BASE_INTERVAL = 1000L; // 1秒
    
    /** 奖励重试队列处理间隔（tick） */
    public static final int REWARD_RETRY_CHECK_INTERVAL = 100; // 5秒
    
    /** 是否启用奖励队列持久化 */
    public static boolean ENABLE_REWARD_PERSISTENCE = true;
    
    // ========== 索引系统相关 ==========
    
    /** 目标索引重建阈值（变更数量） */
    public static final int INDEX_REBUILD_THRESHOLD = 10;
    
    /** 索引缓存过期时间（毫秒） */
    public static final long INDEX_CACHE_EXPIRY = 30_000L; // 30秒
    
    // ========== 增量同步相关 ==========
    
    /** 增量同步最大待处理变更数 */
    public static final int MAX_PENDING_SYNC_DELTAS = 50;
    
    /** 完整性校验间隔（毫秒） */
    public static final long INTEGRITY_CHECK_INTERVAL = 60_000L; // 1分钟
    
    /** 是否启用增量同步 */
    public static boolean ENABLE_INCREMENTAL_SYNC = true;
    
    // ========== 状态机相关 ==========
    
    /** 是否启用状态转移日志 */
    public static boolean ENABLE_STATE_TRANSITION_LOG = true;
    
    /** 状态转移日志最大条目数（每个实例） */
    public static final int MAX_STATE_LOG_ENTRIES = 20;
}
