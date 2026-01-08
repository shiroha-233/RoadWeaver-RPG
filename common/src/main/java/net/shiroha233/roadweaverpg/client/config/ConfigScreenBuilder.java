package net.shiroha233.roadweaverpg.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.config.RoadWeaverConfig;

/**
 * RoadWeaver RPG 配置界面构建器
 * 使用 Cloth Config API 构建现代化的配置界面。
 */
public class ConfigScreenBuilder {

    public static Screen create(Screen parent) {
        RoadWeaverConfig config = RoadWeaverConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.roadweaver_rpg.title"))
                .setSavingRunnable(RoadWeaverConfig::save)
                .setTransparentBackground(true);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ================= 游戏性设置 (Gameplay) =================
        ConfigCategory gameplay = builder.getOrCreateCategory(Component.translatable("config.roadweaver_rpg.category.gameplay"));

        // 每日委托子分类
        SubCategoryBuilder dailyQuests = entryBuilder.startSubCategory(Component.translatable("config.roadweaver_rpg.subcategory.daily_quest"));
        dailyQuests.add(entryBuilder.startIntSlider(Component.translatable("config.roadweaver_rpg.option.daily_count_d"), config.dailyQuest.countD, 0, 10)
                .setDefaultValue(3)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.daily_count_d.tooltip"))
                .setSaveConsumer(newValue -> config.dailyQuest.countD = newValue)
                .build());
        dailyQuests.add(entryBuilder.startIntSlider(Component.translatable("config.roadweaver_rpg.option.daily_count_c"), config.dailyQuest.countC, 0, 10)
                .setDefaultValue(3)
                .setSaveConsumer(newValue -> config.dailyQuest.countC = newValue)
                .build());
        dailyQuests.add(entryBuilder.startIntSlider(Component.translatable("config.roadweaver_rpg.option.daily_count_b"), config.dailyQuest.countB, 0, 10)
                .setDefaultValue(2)
                .setSaveConsumer(newValue -> config.dailyQuest.countB = newValue)
                .build());
        dailyQuests.add(entryBuilder.startIntSlider(Component.translatable("config.roadweaver_rpg.option.daily_count_a"), config.dailyQuest.countA, 0, 10)
                .setDefaultValue(2)
                .setSaveConsumer(newValue -> config.dailyQuest.countA = newValue)
                .build());
        dailyQuests.add(entryBuilder.startIntSlider(Component.translatable("config.roadweaver_rpg.option.daily_count_s"), config.dailyQuest.countS, 0, 10)
                .setDefaultValue(1)
                .setSaveConsumer(newValue -> config.dailyQuest.countS = newValue)
                .build());
        dailyQuests.setExpanded(false);
        gameplay.addEntry(dailyQuests.build());

        // 委托限制子分类
        SubCategoryBuilder limits = entryBuilder.startSubCategory(Component.translatable("config.roadweaver_rpg.subcategory.quest_limits"));
        limits.add(entryBuilder.startIntField(Component.translatable("config.roadweaver_rpg.option.max_active_quests"), config.questLimit.maxActiveQuests)
                .setDefaultValue(10)
                .setMin(1)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.max_active_quests.tooltip"))
                .setSaveConsumer(newValue -> config.questLimit.maxActiveQuests = newValue)
                .build());
        limits.add(entryBuilder.startIntField(Component.translatable("config.roadweaver_rpg.option.default_time_limit"), config.questLimit.defaultTimeLimit)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.default_time_limit.tooltip"))
                .setSaveConsumer(newValue -> config.questLimit.defaultTimeLimit = newValue)
                .build());
        gameplay.addEntry(limits.build());

        // 难度设置子分类
        SubCategoryBuilder difficulty = entryBuilder.startSubCategory(Component.translatable("config.roadweaver_rpg.subcategory.difficulty"));
        difficulty.add(entryBuilder.startFloatField(Component.translatable("config.roadweaver_rpg.option.base_difficulty"), config.difficulty.baseDifficulty)
                .setDefaultValue(1.0f)
                .setMin(0.1f)
                .setSaveConsumer(newValue -> config.difficulty.baseDifficulty = newValue)
                .build());
        difficulty.add(entryBuilder.startFloatField(Component.translatable("config.roadweaver_rpg.option.difficulty_per_level"), config.difficulty.difficultyPerLevel)
                .setDefaultValue(0.01f)
                .setSaveConsumer(newValue -> config.difficulty.difficultyPerLevel = newValue)
                .build());
        gameplay.addEntry(difficulty.build());

        // ================= 性能设置 (Performance) =================
        ConfigCategory performance = builder.getOrCreateCategory(Component.translatable("config.roadweaver_rpg.category.performance"));

        performance.addEntry(entryBuilder.startIntField(Component.translatable("config.roadweaver_rpg.option.objective_check_interval"), config.performance.objectiveCheckInterval)
                .setDefaultValue(20)
                .setMin(1)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.objective_check_interval.tooltip"))
                .setSaveConsumer(newValue -> config.performance.objectiveCheckInterval = newValue)
                .build());

        performance.addEntry(entryBuilder.startLongField(Component.translatable("config.roadweaver_rpg.option.sync_validation_interval"), config.performance.syncValidationInterval)
                .setDefaultValue(60000L)
                .setSaveConsumer(newValue -> config.performance.syncValidationInterval = newValue)
                .build());

        performance.addEntry(entryBuilder.startIntField(Component.translatable("config.roadweaver_rpg.option.packet_timeout"), config.performance.packetTimeout)
                .setDefaultValue(5000)
                .setSaveConsumer(newValue -> config.performance.packetTimeout = newValue)
                .build());

        // ================= 调试设置 (Debug) =================
        ConfigCategory debug = builder.getOrCreateCategory(Component.translatable("config.roadweaver_rpg.category.debug"));

        debug.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.roadweaver_rpg.option.enable_verbose_logging"), config.debug.enableVerboseLogging)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.debug.enableVerboseLogging = newValue)
                .build());

        debug.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.roadweaver_rpg.option.enable_performance_monitoring"), config.debug.enablePerformanceMonitoring)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.debug.enablePerformanceMonitoring = newValue)
                .build());

        debug.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.roadweaver_rpg.option.enable_consistency_check"), config.debug.enableConsistencyCheck)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.debug.enableConsistencyCheck = newValue)
                .build());

        return builder.build();
    }
}
