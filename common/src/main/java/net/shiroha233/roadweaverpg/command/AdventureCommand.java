package net.shiroha233.roadweaverpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.adventure.AdventureDataService;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;

/**
 * 冒险等级调试命令
 * 
 * 用法：
 * /rwadventure set <玩家> <等级>     - 设置玩家冒险等级
 * /rwadventure add <玩家> <经验>     - 增加玩家冒险经验
 * /rwadventure info [玩家]           - 查看冒险等级信息
 */
public final class AdventureCommand {
    
    private AdventureCommand() {}
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rwadventure")
                .requires(source -> source.hasPermission(2))
                
                // 设置冒险等级
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> setAdventureLevel(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "level"))))))
                
                // 增加冒险经验
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("exp", IntegerArgumentType.integer(1, 1000000))
                                        .executes(ctx -> addAdventureExp(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "exp"))))))
                
                // 查看冒险等级信息
                .then(Commands.literal("info")
                        .executes(ctx -> showAdventureInfo(ctx.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> showAdventureInfo(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                
                // 重置冒险等级
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> resetAdventureLevel(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
        );
    }
    
    /**
     * 设置玩家冒险等级
     */
    private static int setAdventureLevel(CommandSourceStack source, ServerPlayer player, int level) {
        try {
            QuestDataAccessor accessor = QuestDataAccessor.getInstance();
            var data = accessor.getPlayerData(player);
            
            // 获取当前经验，计算需要增加的经验
            int currentExp = data.getAdventureExp();
            int requiredExp = calculateExpForLevel(level);
            int expToAdd = requiredExp - currentExp;
            
            // 通过 addAdventureExp 增加经验（会自动处理等级提升）
            if (expToAdd > 0) {
                AdventureDataService.getInstance().addAdventureExp(player, expToAdd);
            } else if (expToAdd < 0) {
                // 如果需要减少经验，直接修改数据
                data.addAdventureExp(expToAdd);
                data.setAdventureLevel(level);
                accessor.markDirty(player);
                AdventureDataService.getInstance().syncToClient(player);
            }
            
            source.sendSuccess(() -> Component.literal(String.format(
                    "§a已将 %s 的冒险等级设置为 %d",
                    player.getName().getString(), level)), true);
            
            player.sendSystemMessage(Component.literal(String.format(
                    "§6你的冒险等级已被设置为 §e%d", level)));
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c设置失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 增加冒险经验
     */
    private static int addAdventureExp(CommandSourceStack source, ServerPlayer player, int exp) {
        try {
            AdventureDataService.getInstance().addAdventureExp(player, exp);
            
            source.sendSuccess(() -> Component.literal(String.format(
                    "§a已为 %s 增加 %d 冒险经验",
                    player.getName().getString(), exp)), true);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c增加经验失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 查看冒险等级信息
     */
    private static int showAdventureInfo(CommandSourceStack source, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = targetPlayer != null ? targetPlayer : 
                    (source.getEntity() instanceof ServerPlayer ? (ServerPlayer) source.getEntity() : null);
            
            if (player == null) {
                source.sendFailure(Component.literal("§c无法确定目标玩家"));
                return 0;
            }
            
            AdventureDataService service = AdventureDataService.getInstance();
            int level = service.getAdventureLevel(player);
            int exp = service.getAdventureExp(player);
            int nextLevelExp = calculateExpForLevel(level + 1);
            int currentLevelExp = calculateExpForLevel(level);
            int expInLevel = exp - currentLevelExp;
            int expToNextLevel = nextLevelExp - exp;
            
            source.sendSuccess(() -> Component.literal("§6===== 冒险等级信息 ====="), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "§e玩家: §f%s", player.getName().getString())), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "§e当前等级: §f%d", level)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "§e总经验: §f%d", exp)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "§e本级进度: §f%d / %d (%.1f%%)",
                    expInLevel, nextLevelExp - currentLevelExp,
                    (expInLevel * 100.0) / (nextLevelExp - currentLevelExp))), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    "§e升级还需: §f%d 经验", expToNextLevel)), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c查询失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 重置冒险等级
     */
    private static int resetAdventureLevel(CommandSourceStack source, ServerPlayer player) {
        try {
            QuestDataAccessor accessor = QuestDataAccessor.getInstance();
            var data = accessor.getPlayerData(player);
            
            // 获取当前经验，减少到 0
            int currentExp = data.getAdventureExp();
            if (currentExp > 0) {
                data.addAdventureExp(-currentExp);
            }
            data.setAdventureLevel(1);
            
            accessor.markDirty(player);
            AdventureDataService.getInstance().syncToClient(player);
            
            source.sendSuccess(() -> Component.literal(String.format(
                    "§a已重置 %s 的冒险等级",
                    player.getName().getString())), true);
            
            player.sendSystemMessage(Component.literal("§6你的冒险等级已被重置"));
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c重置失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 计算指定等级所需的总经验
     * 这里使用简单的线性公式，实际应该从 AdventureLevelManager 获取
     */
    private static int calculateExpForLevel(int level) {
        // 简单公式：每级需要 1000 * 等级 的经验
        // 例如：1级=1000, 2级=3000, 3级=6000...
        int totalExp = 0;
        for (int i = 1; i < level; i++) {
            totalExp += 1000 * i;
        }
        return totalExp;
    }
}
