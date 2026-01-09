package net.shiroha233.roadweaverpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.stats.StatAllocationData;
import net.shiroha233.roadweaverpg.stats.StatAllocationService;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 技能点系统调试命令
 * 用法：
 * /rwpoint add <player> <amount> - 添加技能点
 * /rwpoint set <player> <amount> - 设置技能点
 * /rwpoint get <player> - 查看技能点信息
 * /rwpoint allocate <player> <stat_type> <amount> - 分配技能点到指定属性
 * /rwpoint reset <player> - 重置所有分配
 * /rwpoint list - 列出所有可分配属性
 */
public class StatPointCommand {
    
    // 属性类型建议
    private static final SuggestionProvider<CommandSourceStack> STAT_TYPE_SUGGESTIONS = 
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(StatType.values())
                            .filter(StatType::isAllocatable)
                            .map(StatType::getId)
                            .collect(Collectors.toList()),
                    builder
            );
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rwpoint")
                .requires(source -> source.hasPermission(2))
                
                // 添加技能点
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> addPoints(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount")
                                        )))))
                
                // 设置技能点
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10000))
                                        .executes(ctx -> setPoints(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount")
                                        )))))
                
                // 查看技能点信息
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> getPoints(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                ))))
                
                // 分配技能点到指定属性
                .then(Commands.literal("allocate")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stat_type", StringArgumentType.word())
                                        .suggests(STAT_TYPE_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> allocatePoints(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "stat_type"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")
                                                ))))))
                
                // 重置所有分配
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> resetAllocation(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                ))))
                
                // 列出所有可分配属性
                .then(Commands.literal("list")
                        .executes(ctx -> listStatTypes(ctx.getSource())))
        );
    }
    
    private static int addPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        StatAllocationService.getInstance().addSkillPoints(player, amount);
        source.sendSuccess(() -> Component.translatable(
                "command.roadweaver_rpg.point.add.success", player.getName().getString(), amount), true);
        return 1;
    }
    
    private static int setPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        try {
            PlayerQuestData questData = QuestDataAccessor.getInstance().getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            allocData.setAvailablePoints(amount);
            QuestDataAccessor.getInstance().markDirty(player);
            StatAllocationService.getInstance().syncToClient(player);
            
            source.sendSuccess(() -> Component.translatable(
                    "command.roadweaver_rpg.point.set.success", player.getName().getString(), amount), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
        return 1;
    }
    
    private static int getPoints(CommandSourceStack source, ServerPlayer player) {
        try {
            PlayerQuestData questData = QuestDataAccessor.getInstance().getPlayerData(player);
            StatAllocationData allocData = questData.getStatAllocationData();
            
            int available = allocData.getAvailablePoints();
            int totalAllocated = allocData.getTotalAllocatedPoints();
            
            source.sendSuccess(() -> Component.literal("§6=== " + player.getName().getString() + " 技能点信息 ==="), false);
            source.sendSuccess(() -> Component.literal("§e可用技能点: §f" + available), false);
            source.sendSuccess(() -> Component.literal("§e已分配总计: §f" + totalAllocated), false);
            source.sendSuccess(() -> Component.literal("§7--- 分配详情 ---"), false);
            
            for (StatType type : StatType.values()) {
                if (!type.isAllocatable()) continue;
                int points = allocData.getAllocatedPoints(type);
                if (points > 0) {
                    source.sendSuccess(() -> Component.literal("§a" + type.getId() + ": §f" + points), false);
                }
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed: " + e.getMessage()));
            return 0;
        }
        return 1;
    }
    
    private static int allocatePoints(CommandSourceStack source, ServerPlayer player, 
            String statTypeId, int amount) {
        StatType type = StatType.fromId(statTypeId);
        if (type == null || !type.isAllocatable()) {
            source.sendFailure(Component.literal("Invalid stat type: " + statTypeId));
            return 0;
        }
        
        int success = 0;
        for (int i = 0; i < amount; i++) {
            if (StatAllocationService.getInstance().allocatePoint(player, type)) {
                success++;
            } else {
                break;
            }
        }
        
        final int allocated = success;
        source.sendSuccess(() -> Component.translatable(
                "command.roadweaver_rpg.point.allocate.success", 
                allocated, type.getId(), player.getName().getString()), true);
        return 1;
    }
    
    private static int resetAllocation(CommandSourceStack source, ServerPlayer player) {
        StatAllocationService.getInstance().resetAllocation(player);
        source.sendSuccess(() -> Component.translatable(
                "command.roadweaver_rpg.point.reset.success", player.getName().getString()), true);
        return 1;
    }
    
    private static int listStatTypes(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== 可分配属性列表 ==="), false);
        for (StatType type : StatType.values()) {
            if (type.isAllocatable()) {
                source.sendSuccess(() -> Component.literal("§e" + type.getId() + " §7- " + type.getCategory().name()), false);
            }
        }
        return 1;
    }
}
