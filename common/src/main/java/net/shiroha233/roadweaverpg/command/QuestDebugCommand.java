package net.shiroha233.roadweaverpg.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;
import net.shiroha233.roadweaverpg.reputation.ReputationManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/**
 * 委托系统调试指令
 * 用法：
 * /rwquest reputation set <player> <faction> <level>
 * /rwquest reputation add <player> <faction> <amount>
 * /rwquest reputation get <player> <faction>
 * /rwquest daily refresh <player>
 * /rwquest daily list <player>
 */
public class QuestDebugCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rwquest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reputation")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", ResourceLocationArgument.id())
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                        .executes(ctx -> setReputationLevel(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                ResourceLocationArgument.getId(ctx, "faction"),
                                                                IntegerArgumentType.getInteger(ctx, "level")
                                                        ))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", ResourceLocationArgument.id())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000))
                                                        .executes(ctx -> addReputationXp(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                ResourceLocationArgument.getId(ctx, "faction"),
                                                                IntegerArgumentType.getInteger(ctx, "amount")
                                                        ))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", ResourceLocationArgument.id())
                                                .executes(ctx -> getReputationInfo(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        ResourceLocationArgument.getId(ctx, "faction")
                                                ))))))
                .then(Commands.literal("daily")
                        .then(Commands.literal("refresh")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> refreshDailyQuests(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")
                                        ))))
                        .then(Commands.literal("list")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> listDailyQuests(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")
                                        )))))
                .then(Commands.literal("data")
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> clearPlayerData(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")
                                        )))))
        );
    }
    
    private static int setReputationLevel(CommandSourceStack source, ServerPlayer player, 
                                          ResourceLocation faction, int level) {
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        data.setReputationLevel(faction, level);
        QuestDataAccessor.getInstance().markDirty(player);
        
        PlayerQuestService.getInstance().syncReputationToClient(player);
        
        final String factionName = faction.getPath();
        final String playerName = player.getName().getString();
        source.sendSuccess(() -> Component.literal(String.format(
                "§a设置 %s 的 %s 声望等级为 %d",
                playerName, factionName, level
        )), true);
        
        return 1;
    }
    
    private static int addReputationXp(CommandSourceStack source, ServerPlayer player,
                                       ResourceLocation faction, int amount) {
        PlayerQuestService.getInstance().addReputationXp(player, faction, amount);
        
        final String factionName = faction.getPath();
        final String playerName = player.getName().getString();
        source.sendSuccess(() -> Component.literal(String.format(
                "§a为 %s 添加 %d 点 %s 声望",
                playerName, amount, factionName
        )), true);
        
        return 1;
    }
    
    private static int getReputationInfo(CommandSourceStack source, ServerPlayer player,
                                         ResourceLocation faction) {
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        int xp = data.getReputationXp(faction);
        int level = data.getReputationLevel(faction);
        
        ReputationManager manager = ReputationManager.getInstance();
        int nextLevelXp = -1;
        if (manager != null) {
            nextLevelXp = manager.getExperienceForNextLevel(level);
        }
        
        final String info;
        if (nextLevelXp > 0) {
            info = String.format(
                    "§6%s 的 %s 声望信息:\n§e等级: %d\n§e当前XP: %d\n§e下级需要XP: %d",
                    player.getName().getString(), faction.getPath(), level, xp, nextLevelXp
            );
        } else {
            info = String.format(
                    "§6%s 的 %s 声望信息:\n§e等级: %d\n§e当前XP: %d",
                    player.getName().getString(), faction.getPath(), level, xp
            );
        }
        
        source.sendSuccess(() -> Component.literal(info), false);
        return 1;
    }
    
    private static int refreshDailyQuests(CommandSourceStack source, ServerPlayer player) {
        net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager.getInstance()
                .checkAndRefreshDailyQuests(player);
        
        final String playerName = player.getName().getString();
        source.sendSuccess(() -> Component.literal(String.format(
                "§a已刷新 %s 的每日委托",
                playerName
        )), true);
        
        return 1;
    }
    
    private static int listDailyQuests(CommandSourceStack source, ServerPlayer player) {
        var dailyQuests = net.shiroha233.roadweaverpg.quest.daily.DailyQuestManager.getInstance()
                .getDailyQuests(player);
        
        final StringBuilder sb = new StringBuilder("§6今日委托列表:\n");
        if (dailyQuests.isEmpty()) {
            sb.append("§c无");
        } else {
            for (var quest : dailyQuests) {
                sb.append(String.format("§e- %s\n", quest.getTitle().getString()));
            }
        }
        
        final String result = sb.toString();
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }
    
    private static int clearPlayerData(CommandSourceStack source, ServerPlayer player) {
        PlayerQuestData data = QuestDataAccessor.getInstance().getPlayerData(player);
        
        // 清除所有数据
        data.getActiveQuestIds().forEach(id -> data.removeActiveQuest(id));
        
        QuestDataAccessor.getInstance().markDirty(player);
        
        final String playerName = player.getName().getString();
        source.sendSuccess(() -> Component.literal(String.format(
                "§a已清除 %s 的所有委托数据",
                playerName
        )), true);
        
        return 1;
    }
}
