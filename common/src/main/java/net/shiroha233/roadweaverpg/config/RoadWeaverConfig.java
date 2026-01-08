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

    public Wallet wallet = new Wallet();
    public Debug debug = new Debug();

    // ================= 子配置类 =================

    /**
     * 钱包系统配置
     */
    public static class Wallet {
        /** 拾取金币时自动存入钱包 */
        public boolean autoDepositOnPickup = true;
        /** 显示金币获取提示 */
        public boolean showCoinNotification = true;
        /** 金币提示显示时长（毫秒） */
        public int notificationDuration = 3000;
    }

    /**
     * 调试配置
     */
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
                if (INSTANCE == null) {
                    INSTANCE = new RoadWeaverConfig();
                }
            } catch (IOException e) {
                e.printStackTrace();
                INSTANCE = new RoadWeaverConfig();
            }
        } else {
            INSTANCE = new RoadWeaverConfig();
            save();
        }
        syncToLegacyConfig();
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        syncToLegacyConfig();
    }

    private static void syncToLegacyConfig() {
        // 同步 Debug
        QuestSystemConfig.ENABLE_VERBOSE_LOGGING = INSTANCE.debug.enableVerboseLogging;
        QuestSystemConfig.ENABLE_PERFORMANCE_MONITORING = INSTANCE.debug.enablePerformanceMonitoring;
        QuestSystemConfig.ENABLE_CONSISTENCY_CHECK = INSTANCE.debug.enableConsistencyCheck;
        QuestSystemConfig.ENABLE_TRANSACTION_ROLLBACK_LOG = INSTANCE.debug.enableTransactionRollbackLog;
    }
}
