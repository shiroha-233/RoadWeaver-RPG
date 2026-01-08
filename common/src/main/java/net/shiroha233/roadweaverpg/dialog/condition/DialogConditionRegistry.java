package net.shiroha233.roadweaverpg.dialog.condition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.ConditionRegistry;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话条件注册表
 * 
 * 职责：管理和解析对话条件（字符串格式）
 * 原理：适配器模式 - 将字符串条件转换为统一条件系统
 * 
 * 注意：此类保留用于兼容旧的字符串格式条件
 * 新代码建议直接使用 ConditionRegistry（JSON格式）
 * 
 * 支持的条件格式：
 * - quest_completed:modid:quest_id - 检查委托是否完成
 * - quest_active:modid:quest_id - 检查委托是否进行中
 * - reputation_level:modid:faction_id>=5 - 检查声望等级
 * - reputation_xp:modid:faction_id>=100 - 检查声望经验
 * - has_item:minecraft:diamond>=10 - 检查物品数量
 * - in_biome:minecraft:plains - 检查玩家所在群系
 * - in_dimension:minecraft:overworld - 检查玩家所在维度
 * - is_day / is_night - 检查时间
 * - !condition - 取反条件
 * - condition1&&condition2 - AND组合
 * - condition1||condition2 - OR组合
 */
public final class DialogConditionRegistry {
    
    private DialogConditionRegistry() {}
    
    // 条件类型注册表
    private static final ConcurrentHashMap<String, ConditionParser> PARSERS = new ConcurrentHashMap<>();
    
    // 条件解析正则
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("([<>=!]+)(\\d+)$");
    private static final Pattern NEGATION_PATTERN = Pattern.compile("^!(.+)$");
    private static final Pattern AND_PATTERN = Pattern.compile("&&");
    private static final Pattern OR_PATTERN = Pattern.compile("\\|\\|");
    
    /**
     * 条件解析器接口
     */
    @FunctionalInterface
    public interface ConditionParser {
        DialogCondition parse(String args);
    }
    
    /**
     * 初始化默认条件
     */
    public static void init() {
        // 先初始化统一条件系统
        ConditionRegistry.init();
        
        // 委托完成条件
        register("quest_completed", args -> {
            ResourceLocation questId = new ResourceLocation(args);
            return (player, npcId) -> {
                var data = QuestDataAccessor.getInstance().getPlayerData(player);
                return data != null && data.hasCompletedQuest(questId);
            };
        });
        
        // 委托进行中条件
        register("quest_active", args -> {
            ResourceLocation questId = new ResourceLocation(args);
            return (player, npcId) -> {
                var data = QuestDataAccessor.getInstance().getPlayerData(player);
                return data != null && data.hasActiveQuest(questId);
            };
        });
        
        // 声望等级条件
        register("reputation_level", args -> parseComparisonCondition(args, (player, factionId, value) -> {
            var data = QuestDataAccessor.getInstance().getPlayerData(player);
            return data != null ? data.getReputationLevel(factionId) : 0;
        }));
        
        // 声望经验条件
        register("reputation_xp", args -> parseComparisonCondition(args, (player, factionId, value) -> {
            var data = QuestDataAccessor.getInstance().getPlayerData(player);
            return data != null ? data.getReputationXp(factionId) : 0;
        }));
        
        // 委托完成次数条件
        register("quest_count", args -> parseComparisonCondition(args, (player, questId, value) -> {
            var data = QuestDataAccessor.getInstance().getPlayerData(player);
            return data != null ? data.getCompletionCount(questId) : 0;
        }));
        
        // 物品持有条件
        register("has_item", args -> {
            Matcher matcher = COMPARISON_PATTERN.matcher(args);
            int requiredCount = 1;
            String itemIdStr = args;
            String operator = ">=";
            
            if (matcher.find()) {
                operator = matcher.group(1);
                requiredCount = Integer.parseInt(matcher.group(2));
                itemIdStr = args.substring(0, matcher.start());
            }
            
            ResourceLocation itemId = new ResourceLocation(itemIdStr);
            final int finalCount = requiredCount;
            final String finalOp = operator;
            
            return (player, npcId) -> {
                int count = countItems(player, itemId);
                return compareValues(count, finalOp, finalCount);
            };
        });
        
        // 新增：群系条件（使用统一条件系统）
        register("in_biome", args -> {
            var cond = net.shiroha233.roadweaverpg.condition.impl.BiomeCondition.of(args);
            return (player, npcId) -> cond.evaluate(player, ConditionContext.empty());
        });
        
        // 新增：维度条件
        register("in_dimension", args -> {
            var cond = net.shiroha233.roadweaverpg.condition.impl.DimensionCondition.of(args);
            return (player, npcId) -> cond.evaluate(player, ConditionContext.empty());
        });
        
        // 新增：时间条件
        register("is_day", args -> {
            var cond = net.shiroha233.roadweaverpg.condition.impl.TimeCondition.day();
            return (player, npcId) -> cond.evaluate(player, ConditionContext.empty());
        });
        
        register("is_night", args -> {
            var cond = net.shiroha233.roadweaverpg.condition.impl.TimeCondition.night();
            return (player, npcId) -> cond.evaluate(player, ConditionContext.empty());
        });
        
        // 新增：天气条件
        register("is_raining", args -> {
            var cond = net.shiroha233.roadweaverpg.condition.impl.WeatherCondition.rain();
            return (player, npcId) -> cond.evaluate(player, ConditionContext.empty());
        });
        
        register("is_thundering", args -> {
            var cond = net.shiroha233.roadweaverpg.condition.impl.WeatherCondition.thunder();
            return (player, npcId) -> cond.evaluate(player, ConditionContext.empty());
        });
        
        RoadWeaverRPG.LOGGER.info("已注册 {} 个对话条件类型", PARSERS.size());
    }
    
    /**
     * 注册条件解析器
     */
    public static void register(String type, ConditionParser parser) {
        PARSERS.put(type, parser);
    }
    
    /**
     * 评估条件字符串
     * @param conditionStr 条件字符串
     * @return 条件对象
     */
    public static DialogCondition parse(String conditionStr) {
        if (conditionStr == null || conditionStr.isBlank()) {
            return DialogCondition.ALWAYS;
        }
        
        try {
            return parseInternal(conditionStr.trim());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.warn("解析条件失败: {} - {}", conditionStr, e.getMessage());
            return DialogCondition.ALWAYS;
        }
    }
    
    /**
     * 评估条件（便捷方法）
     */
    public static boolean evaluate(Optional<String> condition, ServerPlayer player, int npcEntityId) {
        if (condition.isEmpty() || condition.get().isBlank()) {
            return true;
        }
        return parse(condition.get()).evaluate(player, npcEntityId);
    }
    
    /**
     * 内部解析方法
     */
    private static DialogCondition parseInternal(String str) {
        // 处理OR组合（优先级最低）
        String[] orParts = OR_PATTERN.split(str, 2);
        if (orParts.length == 2) {
            return parseInternal(orParts[0]).or(parseInternal(orParts[1]));
        }
        
        // 处理AND组合
        String[] andParts = AND_PATTERN.split(str, 2);
        if (andParts.length == 2) {
            return parseInternal(andParts[0]).and(parseInternal(andParts[1]));
        }
        
        // 处理取反
        Matcher negMatcher = NEGATION_PATTERN.matcher(str);
        if (negMatcher.matches()) {
            return parseInternal(negMatcher.group(1)).negate();
        }
        
        // 解析单个条件
        return parseSingleCondition(str);
    }
    
    /**
     * 解析单个条件
     */
    private static DialogCondition parseSingleCondition(String str) {
        int colonIndex = str.indexOf(':');
        if (colonIndex == -1) {
            RoadWeaverRPG.LOGGER.warn("无效的条件格式: {}", str);
            return DialogCondition.ALWAYS;
        }
        
        String type = str.substring(0, colonIndex);
        String args = str.substring(colonIndex + 1);
        
        ConditionParser parser = PARSERS.get(type);
        if (parser == null) {
            RoadWeaverRPG.LOGGER.warn("未知的条件类型: {}", type);
            return DialogCondition.ALWAYS;
        }
        
        return parser.parse(args);
    }
    
    /**
     * 解析比较条件
     */
    private static DialogCondition parseComparisonCondition(String args, ValueGetter getter) {
        Matcher matcher = COMPARISON_PATTERN.matcher(args);
        if (!matcher.find()) {
            RoadWeaverRPG.LOGGER.warn("无效的比较条件格式: {}", args);
            return DialogCondition.ALWAYS;
        }
        
        String operator = matcher.group(1);
        int targetValue = Integer.parseInt(matcher.group(2));
        String idStr = args.substring(0, matcher.start());
        ResourceLocation id = new ResourceLocation(idStr);
        
        return (player, npcId) -> {
            int actualValue = getter.getValue(player, id, targetValue);
            return compareValues(actualValue, operator, targetValue);
        };
    }
    
    /**
     * 比较值
     */
    private static boolean compareValues(int actual, String operator, int target) {
        return switch (operator) {
            case ">=" -> actual >= target;
            case "<=" -> actual <= target;
            case ">" -> actual > target;
            case "<" -> actual < target;
            case "==" -> actual == target;
            case "!=" -> actual != target;
            default -> actual >= target;
        };
    }
    
    /**
     * 统计玩家物品数量
     */
    private static int countItems(ServerPlayer player, ResourceLocation itemId) {
        int count = 0;
        var registry = player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
        var item = registry.get(itemId);
        
        if (item != null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.is(item)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }
    
    @FunctionalInterface
    private interface ValueGetter {
        int getValue(ServerPlayer player, ResourceLocation id, int targetValue);
    }
}
