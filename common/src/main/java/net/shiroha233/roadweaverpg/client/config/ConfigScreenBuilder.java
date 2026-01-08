package net.shiroha233.roadweaverpg.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.config.RoadWeaverConfig;

/**
 * RoadWeaver RPG 配置界面构建器
 * 使用 Cloth Config API 构建配置界面
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

        // ================= 钱包设置 =================
        ConfigCategory wallet = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver_rpg.category.wallet"));

        wallet.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.roadweaver_rpg.option.auto_deposit"),
                        config.wallet.autoDepositOnPickup)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.auto_deposit.tooltip"))
                .setSaveConsumer(newValue -> config.wallet.autoDepositOnPickup = newValue)
                .build());

        wallet.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.roadweaver_rpg.option.show_notification"),
                        config.wallet.showCoinNotification)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.show_notification.tooltip"))
                .setSaveConsumer(newValue -> config.wallet.showCoinNotification = newValue)
                .build());

        wallet.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.roadweaver_rpg.option.notification_duration"),
                        config.wallet.notificationDuration / 1000, 1, 10)
                .setDefaultValue(3)
                .setTooltip(Component.translatable("config.roadweaver_rpg.option.notification_duration.tooltip"))
                .setSaveConsumer(newValue -> config.wallet.notificationDuration = newValue * 1000)
                .setTextGetter(value -> Component.literal(value + " 秒"))
                .build());

        // ================= 调试设置 =================
        ConfigCategory debug = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver_rpg.category.debug"));

        debug.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.roadweaver_rpg.option.enable_verbose_logging"),
                        config.debug.enableVerboseLogging)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.debug.enableVerboseLogging = newValue)
                .build());

        debug.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.roadweaver_rpg.option.enable_performance_monitoring"),
                        config.debug.enablePerformanceMonitoring)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.debug.enablePerformanceMonitoring = newValue)
                .build());

        debug.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.roadweaver_rpg.option.enable_consistency_check"),
                        config.debug.enableConsistencyCheck)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.debug.enableConsistencyCheck = newValue)
                .build());

        return builder.build();
    }
}
