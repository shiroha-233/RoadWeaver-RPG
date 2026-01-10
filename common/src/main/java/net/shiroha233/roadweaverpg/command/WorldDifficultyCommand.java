package net.shiroha233.roadweaverpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaverpg.worlddifficulty.*;

import java.util.List;

/**
 * 世界难度调试命令
 */
public final class WorldDifficultyCommand {
    
    private WorldDifficultyCommand() {}
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rwdifficulty")
                .requires(source -> source.hasPermission(2))
                
                // 查看附近怪物信息
                .then(Commands.literal("info")
                        .executes(ctx -> showNearbyMobsInfo(ctx.getSource())))
                
                // 强制缩放附近怪物
                .then(Commands.literal("scale")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> scaleNearbyMobs(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))))
                
                // 设置怪物稀有度
                .then(Commands.literal("rarity")
                        .then(Commands.argument("rarity", StringArgumentType.word())
                                .executes(ctx -> setNearbyMobsRarity(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "rarity")))))
                
                // 重载配置
                .then(Commands.literal("reload")
                        .executes(ctx -> reloadConfig(ctx.getSource())))
        );
    }
    
    private static int showNearbyMobsInfo(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(16);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, searchBox);
        
        source.sendSuccess(() -> Component.literal("§6===== 附近怪物难度信息 ====="), false);
        
        int count = 0;
        for (Mob mob : mobs) {
            MonsterData data = MonsterData.fromEntity(mob);
            if (data != null && data.isScaled()) {
                ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
                source.sendSuccess(() -> Component.literal(String.format(
                        "§e%s §7- Lv.%d [%s] §f生命: %.1f/%.1f",
                        mobId.getPath(),
                        data.level(),
                        data.rarity().getDisplayName().getString(),
                        mob.getHealth(),
                        mob.getMaxHealth()
                )), false);
                count++;
            }
        }
        
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("§7附近没有已缩放的怪物"), false);
        } else {
            int finalCount = count;
            source.sendSuccess(() -> Component.literal("§a共找到 " + finalCount + " 个已缩放怪物"), false);
        }
        
        return count;
    }
    
    private static int scaleNearbyMobs(CommandSourceStack source, int level) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        ServerLevel serverLevel = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(16);
        List<Mob> mobs = serverLevel.getEntitiesOfClass(Mob.class, searchBox);
        
        int count = 0;
        for (Mob mob : mobs) {
            // 强制重新缩放
            MonsterScalingService.getInstance().onMobSpawn(mob, serverLevel);
            count++;
        }
        
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§a已对 " + finalCount + " 个怪物应用等级 " + level + " 缩放"), false);
        return count;
    }
    
    private static int setNearbyMobsRarity(CommandSourceStack source, String rarityStr) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        MonsterRarity rarity = MonsterRarity.fromId(rarityStr);
        
        ServerLevel level = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(16);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, searchBox);
        
        int count = 0;
        for (Mob mob : mobs) {
            MonsterData oldData = MonsterData.fromEntity(mob);
            if (oldData != null) {
                MonsterData newData = MonsterData.create(oldData.level(), rarity, oldData.stats());
                newData.saveToEntity(mob);
                count++;
            }
        }
        
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§a已将 " + finalCount + " 个怪物设为 " + rarity.getDisplayName().getString()), false);
        return count;
    }
    
    private static int reloadConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§a难度配置将在下次数据包重载时更新"), false);
        source.sendSuccess(() -> Component.literal("§7使用 /reload 命令重载数据包"), false);
        return 1;
    }
}
