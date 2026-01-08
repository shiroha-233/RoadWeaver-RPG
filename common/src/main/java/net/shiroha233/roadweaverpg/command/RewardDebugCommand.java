package net.shiroha233.roadweaverpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.quest.reward.RewardPerformanceMonitor;
import net.shiroha233.roadweaverpg.quest.reward.RewardQueueManager;

/**
 * 奖励系统调试命令
 * 
 * 用途：
 * - 查看奖励队列状态
 * - 查看性能统计
 * - 重置统计数据
 */
public class RewardDebugCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("roadweaver")
                .then(Commands.literal("reward")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("stats")
                        .executes(RewardDebugCommand::showStats))
                    .then(Commands.literal("performance")
                        .executes(RewardDebugCommand::showPerformance))
                    .then(Commands.literal("reset")
                        .executes(RewardDebugCommand::resetStats))
                )
        );
    }
    
    /**
     * 显示奖励队列统计
     */
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        RewardQueueManager.RewardStats stats = RewardQueueManager.getInstance().getStats();
        
        source.sendSuccess(() -> Component.literal("=== 奖励系统统计 ===")
                .withStyle(style -> style.withColor(0x55FF55).withBold(true)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("已发放: %d", stats.granted()))
                .withStyle(style -> style.withColor(0xFFFFFF)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("失败: %d", stats.failed()))
                .withStyle(style -> style.withColor(0xFF5555)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("重试: %d", stats.retried()))
                .withStyle(style -> style.withColor(0xFFAA00)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("入队: %d", stats.enqueued()))
                .withStyle(style -> style.withColor(0xFFFFFF)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("待发放: %d", stats.pending()))
                .withStyle(style -> style.withColor(0x55FFFF)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("重试队列: %d", stats.pendingRetries()))
                .withStyle(style -> style.withColor(0xFFAA00)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("历史记录: %d", stats.historySize()))
                .withStyle(style -> style.withColor(0xAAAAAA)), false);
        
        // 计算成功率
        long total = stats.granted() + stats.failed();
        if (total > 0) {
            double successRate = (stats.granted() * 100.0) / total;
            source.sendSuccess(() -> Component.literal(String.format("成功率: %.2f%%", successRate))
                    .withStyle(style -> style.withColor(0x55FF55)), false);
        }
        
        return 1;
    }
    
    /**
     * 显示性能统计
     */
    private static int showPerformance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        RewardPerformanceMonitor.PerformanceReport report = 
                RewardPerformanceMonitor.getInstance().getReport();
        
        if (report.totalCount() == 0) {
            source.sendSuccess(() -> Component.literal("暂无性能数据")
                    .withStyle(style -> style.withColor(0xAAAAAA)), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("=== 奖励性能统计 ===")
                .withStyle(style -> style.withColor(0x55FF55).withBold(true)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("总处理数: %d", report.totalCount()))
                .withStyle(style -> style.withColor(0xFFFFFF)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("平均耗时: %dms", report.avgTime()))
                .withStyle(style -> style.withColor(0xFFFFFF)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("最大耗时: %dms", report.maxTime()))
                .withStyle(style -> style.withColor(0xFF5555)), false);
        
        source.sendSuccess(() -> Component.literal(String.format("最小耗时: %dms", report.minTime()))
                .withStyle(style -> style.withColor(0x55FF55)), false);
        
        // 性能警告
        if (report.avgTime() > 100) {
            source.sendSuccess(() -> Component.literal("⚠ 警告: 平均处理时间过长")
                    .withStyle(style -> style.withColor(0xFFAA00)), false);
        }
        
        return 1;
    }
    
    /**
     * 重置统计数据
     */
    private static int resetStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        RewardQueueManager.getInstance().resetStats();
        RewardPerformanceMonitor.getInstance().reset();
        
        source.sendSuccess(() -> Component.literal("已重置奖励系统统计数据")
                .withStyle(style -> style.withColor(0x55FF55)), false);
        
        return 1;
    }
}
