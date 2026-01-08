package net.shiroha233.roadweaverpg.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * RoadWeaver RPG 核心配置类
 * 负责管理所有可调节的游戏参数，并支持 JSON 持久化。
 */
public class RoadWeaverConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/roadweaver_rpg.json");
    
    private static RoadWeaverConfig INSTANCE;

    // ================= 配置分类 =================

    public Performance performance = new Performance();
    public DailyQuest dailyQuest = new DailyQuest();
    public QuestLimit questLimit = new QuestLimit();
    public Difficulty difficulty = new Difficulty();
    public Debug debug = new Debug();

    // ================= 子配置类 =================

    public static class Performance {
        public int objectiveCheckInterval = 20;
        public long syncValidationInterval = 60000;
        public long collectCacheDuration = 1000;
        public int packetTimeout = 5000;
        public int maxPacketSize = 32768;
    }

    public static class DailyQuest {
        public int countD = 3;
        public int countC = 3;
        public int countB = 2;
        public int countA = 2;
        public int countS = 1;
    }

    public static class QuestLimit {
        public int maxActiveQuests = 10;
        public int defaultTimeLimit = 0;
        public int defaultCooldown = 0;
    }

    public static class Difficulty {
        public float baseDifficulty = 1.0f;
        public float difficultyPerLevel = 0.01f;
        public float difficultyPerRepeat = 0.1f;
    }

    public static class Debug {
        public boolean enableVerboseLogging = false;
        public boolean enablePerformanceMonitoring = false;
        public boolean enableConsistencyCheck = true;
        public boolean enableTransactionRollbackLog = true;
    }

    // ================= 管理方法 =================

    public static RoadWeaverConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, RoadWeaverConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new RoadWeaverConfig();
            }
        } else {
            INSTANCE = new RoadWeaverConfig();
            save();
        }
        // 同步到旧的配置类以保持兼容性
        syncToLegacyConfig();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 同步到旧的配置类
        syncToLegacyConfig();
    }

    private static void syncToLegacyConfig() {
        // 同步 Performance
        QuestSystemConfig.OBJECTIVE_CHECK_INTERVAL = INSTANCE.performance.objectiveCheckInterval;
        QuestSystemConfig.SYNC_VALIDATION_INTERVAL = INSTANCE.performance.syncValidationInterval;
        QuestSystemConfig.COLLECT_CACHE_DURATION = INSTANCE.performance.collectCacheDuration;
        QuestSystemConfig.PACKET_TIMEOUT = INSTANCE.performance.packetTimeout;
        QuestSystemConfig.MAX_PACKET_SIZE = INSTANCE.performance.maxPacketSize;

        // 同步 DailyQuest
        QuestSystemConfig.DAILY_QUEST_COUNT_D = INSTANCE.dailyQuest.countD;
        QuestSystemConfig.DAILY_QUEST_COUNT_C = INSTANCE.dailyQuest.countC;
        QuestSystemConfig.DAILY_QUEST_COUNT_B = INSTANCE.dailyQuest.countB;
        QuestSystemConfig.DAILY_QUEST_COUNT_A = INSTANCE.dailyQuest.countA;
        QuestSystemConfig.DAILY_QUEST_COUNT_S = INSTANCE.dailyQuest.countS;

        // 同步 QuestLimit
        QuestSystemConfig.MAX_ACTIVE_QUESTS = INSTANCE.questLimit.maxActiveQuests;
        QuestSystemConfig.DEFAULT_TIME_LIMIT = INSTANCE.questLimit.defaultTimeLimit;
        QuestSystemConfig.DEFAULT_COOLDOWN = INSTANCE.questLimit.defaultCooldown;

        // 同步 Difficulty
        QuestSystemConfig.BASE_DIFFICULTY = INSTANCE.difficulty.baseDifficulty;
        QuestSystemConfig.DIFFICULTY_PER_LEVEL = INSTANCE.difficulty.difficultyPerLevel;
        QuestSystemConfig.DIFFICULTY_PER_REPEAT = INSTANCE.difficulty.difficultyPerRepeat;

        // 同步 Debug
        QuestSystemConfig.ENABLE_VERBOSE_LOGGING = INSTANCE.debug.enableVerboseLogging;
        QuestSystemConfig.ENABLE_PERFORMANCE_MONITORING = INSTANCE.debug.enablePerformanceMonitoring;
        QuestSystemConfig.ENABLE_CONSISTENCY_CHECK = INSTANCE.debug.enableConsistencyCheck;
        QuestSystemConfig.ENABLE_TRANSACTION_ROLLBACK_LOG = INSTANCE.debug.enableTransactionRollbackLog;
    }
}
