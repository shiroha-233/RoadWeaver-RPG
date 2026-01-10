package net.shiroha233.roadweaverpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;
import net.shiroha233.roadweaverpg.stats.StatEffectService;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 属性效果设置指令
 * 用法：
 * /rwstat set <player> <effect_type> <value> - 设置指定效果值
 * /rwstat get <player> <effect_type> - 获取当前效果值
 * /rwstat clear <player> - 清除所有效果
 * /rwstat list - 列出所有可用效果类型
 */
public class StatEffectCommand {
    
    // 效果类型建议提供器
    private static final SuggestionProvider<CommandSourceStack> EFFECT_TYPE_SUGGESTIONS = 
            (context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(LevelEffectType.values())
                            .filter(t -> t.isVanillaAttribute() || t.isMagicAttribute())
                            .map(LevelEffectType::getId)
                            .collect(Collectors.toList()),
                    builder
            );
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rwstat")
                .requires(source -> source.hasPermission(2))
                // 设置效果值
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("effect_type", StringArgumentType.word())
                                        .suggests(EFFECT_TYPE_SUGGESTIONS)
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(-1000, 1000))
                                                .executes(ctx -> setEffect(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "effect_type"),
                                                        DoubleArgumentType.getDouble(ctx, "value")
                                                ))))))
                // 清除所有效果
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clearEffects(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                ))))
                // 列出所有效果类型
                .then(Commands.literal("list")
                        .executes(ctx -> listEffectTypes(ctx.getSource())))
                // 刷新玩家效果（重新应用等级效果）
                .then(Commands.literal("refresh")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> refreshEffects(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")
                                ))))
        );
    }
    
    /**
     * 设置指定效果值
     */
    private static int setEffect(CommandSourceStack source, ServerPlayer player, 
                                  String effectType, double value) {
        LevelEffectType type = LevelEffectType.fromString(effectType);
        if (type == null) {
            source.sendFailure(Component.literal("§c未知的效果类型: " + effectType));
            return 0;
        }
        
        try {
            applyEffectByType(player, type, value);
            
            final String playerName = player.getName().getString();
            final String typeName = type.getId();
            source.sendSuccess(() -> Component.literal(String.format(
                    "§a已设置 %s 的 %s 效果为 %.2f",
                    playerName, typeName, value
            )), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c设置效果失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 根据效果类型应用效果
     */
    private static void applyEffectByType(ServerPlayer player, LevelEffectType type, double value) {
        switch (type) {
            // 原版属性
            case MAX_HEALTH -> StatEffectService.applyMaxHealth(player, value);
            case ATTACK_DAMAGE -> StatEffectService.applyAttackDamage(player, value);
            case ARMOR -> StatEffectService.applyArmor(player, value);
            case ARMOR_TOUGHNESS -> StatEffectService.applyArmorToughness(player, value);
            case MOVEMENT_SPEED -> StatEffectService.applyMovementSpeed(player, value);
            case ATTACK_SPEED -> StatEffectService.applyAttackCooldown(player, value); // 攻击速度通过冷却缩减实现
            case KNOCKBACK_RESIST -> StatEffectService.applyKnockbackResistance(player, value);
            case LUCK -> StatEffectService.applyLuck(player, value);
            // 魔法属性
            case SPELL_POWER -> StatEffectService.applySpellPower(player, value);
            case MAX_MANA -> StatEffectService.applyMaxMana(player, value);
            case MANA_REGEN -> StatEffectService.applyManaRegen(player, value);
            case COOLDOWN_REDUCTION -> StatEffectService.applyCooldownReduction(player, value);
            case SPELL_RESIST -> StatEffectService.applySpellResist(player, value);
            default -> throw new IllegalArgumentException("不支持的效果类型: " + type.getId());
        }
    }
    
    /**
     * 清除所有效果
     */
    private static int clearEffects(CommandSourceStack source, ServerPlayer player) {
        StatEffectService.removeAllModifiers(player);
        
        final String playerName = player.getName().getString();
        source.sendSuccess(() -> Component.literal(String.format(
                "§a已清除 %s 的所有属性效果",
                playerName
        )), true);
        return 1;
    }
    
    /**
     * 列出所有可用效果类型
     */
    private static int listEffectTypes(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("§6可用的效果类型:\n");
        
        sb.append("§e原版属性:\n");
        for (LevelEffectType type : LevelEffectType.values()) {
            if (type.isVanillaAttribute()) {
                sb.append(String.format("  §7- §f%s\n", type.getId()));
            }
        }
        
        sb.append("§e魔法属性:\n");
        for (LevelEffectType type : LevelEffectType.values()) {
            if (type.isMagicAttribute()) {
                sb.append(String.format("  §7- §b%s\n", type.getId()));
            }
        }
        
        final String result = sb.toString();
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }
    
    /**
     * 刷新玩家效果（重新应用等级效果）
     */
    private static int refreshEffects(CommandSourceStack source, ServerPlayer player) {
        // 先清除所有效果
        StatEffectService.removeAllModifiers(player);
        
        // 重新应用等级效果
        net.shiroha233.roadweaverpg.playerlevel.PlayerLevelDataService.getInstance()
                .refreshEffects(player);
        
        final String playerName = player.getName().getString();
        source.sendSuccess(() -> Component.literal(String.format(
                "§a已刷新 %s 的所有属性效果",
                playerName
        )), true);
        return 1;
    }
}
