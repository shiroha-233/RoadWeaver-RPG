package net.shiroha233.roadweaverpg.dialog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.dialog.condition.DialogConditionRegistry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话动作注册表
 * 职责：管理所有可用的对话动作
 * 
 * 支持的动作格式：
 * - none                           - 无动作
 * - close                          - 关闭对话
 * - show_quests                    - 显示任务列表
 * - complete_quest                 - 提交任务
 * - retrieve_scroll                - 领取卷轴
 * - view_reputation                - 查看声望
 * - open_shop                      - 打开商店
 * - accept_quest:modid:quest_id    - 接取指定任务
 * - give_item:modid:item_id:count  - 给予物品
 * - take_item:modid:item_id:count  - 扣除物品
 * - add_xp:amount                  - 给予经验
 * - run_command:/command           - 执行命令
 * - jump:modid:dialog_id           - 跳转到指定对话
 */
public final class DialogActionRegistry {
    
    private static final ConcurrentHashMap<String, DialogAction> ACTIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ActionParser> PARSERS = new ConcurrentHashMap<>();
    
    // 动作参数解析正则
    private static final Pattern PARAM_PATTERN = Pattern.compile("^([^:]+):(.+)$");
    
    private static boolean initialized = false;
    
    private DialogActionRegistry() {}
    
    /**
     * 动作解析器接口
     */
    @FunctionalInterface
    public interface ActionParser {
        DialogAction parse(String args);
    }
    
    /**
     * 初始化默认动作
     */
    public static void init() {
        if (initialized) {
            RoadWeaverRPG.LOGGER.warn("DialogActionRegistry 已初始化，跳过重复初始化");
            return;
        }
        
        // 初始化条件系统
        DialogConditionRegistry.init();
        
        // 基础动作
        register("none", DialogAction.NONE);
        register("close", DialogAction.CLOSE);
        
        // 委托系统动作 - 直接调用 UnifiedActionHandler
        register("show_quests", (player, session, npcEntityId) -> {
            RoadWeaverRPG.LOGGER.debug("玩家 {} 请求查看委托", player.getName().getString());
            UnifiedActionHandler.executeByAlias(player, npcEntityId, "show_quests");
        });
        
        register("complete_quest", (player, session, npcEntityId) -> {
            RoadWeaverRPG.LOGGER.debug("玩家 {} 请求完成委托", player.getName().getString());
            UnifiedActionHandler.executeByAlias(player, npcEntityId, "complete_quest");
        });
        
        register("retrieve_scroll", (player, session, npcEntityId) -> {
            RoadWeaverRPG.LOGGER.debug("玩家 {} 请求取回卷轴", player.getName().getString());
            UnifiedActionHandler.executeByAlias(player, npcEntityId, "retrieve_scroll");
        });
        
        register("view_reputation", (player, session, npcEntityId) -> {
            RoadWeaverRPG.LOGGER.debug("玩家 {} 请求查看声望", player.getName().getString());
            UnifiedActionHandler.executeByAlias(player, npcEntityId, "view_reputation");
        });
        
        register("open_shop", (player, session, npcEntityId) -> {
            RoadWeaverRPG.LOGGER.debug("玩家 {} 请求打开商店", player.getName().getString());
            UnifiedActionHandler.executeByAlias(player, npcEntityId, "open_shop");
        });
        
        // 注册参数化动作解析器
        registerParser("accept_quest", DialogActionRegistry::parseAcceptQuest);
        registerParser("give_item", DialogActionRegistry::parseGiveItem);
        registerParser("take_item", DialogActionRegistry::parseTakeItem);
        registerParser("add_xp", DialogActionRegistry::parseAddXp);
        registerParser("run_command", DialogActionRegistry::parseRunCommand);
        registerParser("jump", DialogActionRegistry::parseJump);
        registerParser("send_message", DialogActionRegistry::parseSendMessage);
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("已注册 {} 个对话动作, {} 个动作解析器", ACTIONS.size(), PARSERS.size());
    }
    
    /**
     * 注册动作
     */
    public static void register(String id, DialogAction action) {
        if (id == null || id.isBlank()) {
            RoadWeaverRPG.LOGGER.warn("尝试注册空ID的对话动作");
            return;
        }
        if (action == null) {
            RoadWeaverRPG.LOGGER.warn("尝试注册空动作: {}", id);
            return;
        }
        ACTIONS.put(id, action);
    }
    
    /**
     * 注册动作解析器
     */
    public static void registerParser(String type, ActionParser parser) {
        PARSERS.put(type, parser);
    }
    
    /**
     * 获取动作（支持参数化动作）
     */
    public static DialogAction getAction(String actionStr) {
        if (actionStr == null || actionStr.isBlank()) {
            return DialogAction.NONE;
        }
        
        // 先尝试直接查找
        DialogAction direct = ACTIONS.get(actionStr);
        if (direct != null) {
            return direct;
        }
        
        // 尝试解析参数化动作
        Matcher matcher = PARAM_PATTERN.matcher(actionStr);
        if (matcher.matches()) {
            String type = matcher.group(1);
            String args = matcher.group(2);
            
            ActionParser parser = PARSERS.get(type);
            if (parser != null) {
                try {
                    return parser.parse(args);
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.error("解析动作失败: {} - {}", actionStr, e.getMessage());
                }
            }
        }
        
        RoadWeaverRPG.LOGGER.warn("未知的动作类型: {}", actionStr);
        return DialogAction.NONE;
    }
    
    // ==================== 参数化动作解析器 ====================
    
    /**
     * 解析接取任务动作
     * 格式: accept_quest:modid:quest_id
     */
    private static DialogAction parseAcceptQuest(String args) {
        ResourceLocation questId = new ResourceLocation(args);
        return (player, session, npcEntityId) -> {
            try {
                var questService = net.shiroha233.roadweaverpg.quest.service.PlayerQuestService.getInstance();
                var result = questService.acceptQuest(player, questId);
                if (result.isPresent()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§a已接取任务！"), false);
                } else {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c无法接取任务"), false);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("接取任务失败: {}", questId, e);
            }
        };
    }
    
    /**
     * 解析给予物品动作
     * 格式: give_item:modid:item_id:count 或 give_item:modid:item_id
     */
    private static DialogAction parseGiveItem(String args) {
        String[] parts = args.split(":");
        if (parts.length < 2) {
            RoadWeaverRPG.LOGGER.warn("无效的give_item参数: {}", args);
            return DialogAction.NONE;
        }
        
        String itemIdStr;
        int count = 1;
        
        if (parts.length >= 3) {
            // 尝试解析最后一部分为数量
            try {
                count = Integer.parseInt(parts[parts.length - 1]);
                itemIdStr = args.substring(0, args.lastIndexOf(':'));
            } catch (NumberFormatException e) {
                // 最后一部分不是数字，整个字符串都是物品ID
                itemIdStr = args;
            }
        } else {
            itemIdStr = args;
        }
        
        ResourceLocation itemId = new ResourceLocation(itemIdStr);
        final int finalCount = count;
        
        return (player, session, npcEntityId) -> {
            try {
                var registry = player.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
                var item = registry.get(itemId);
                
                if (item != null) {
                    ItemStack stack = new ItemStack(item, finalCount);
                    if (!player.getInventory().add(stack)) {
                        // 背包满了，掉落在地上
                        player.drop(stack, false);
                    }
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§a获得了 " + finalCount + " 个 " + stack.getHoverName().getString()), false);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("给予物品失败: {}", itemId, e);
            }
        };
    }
    
    /**
     * 解析扣除物品动作
     * 格式: take_item:modid:item_id:count
     */
    private static DialogAction parseTakeItem(String args) {
        String[] parts = args.split(":");
        if (parts.length < 2) {
            RoadWeaverRPG.LOGGER.warn("无效的take_item参数: {}", args);
            return DialogAction.NONE;
        }
        
        String itemIdStr;
        int count = 1;
        
        if (parts.length >= 3) {
            try {
                count = Integer.parseInt(parts[parts.length - 1]);
                itemIdStr = args.substring(0, args.lastIndexOf(':'));
            } catch (NumberFormatException e) {
                itemIdStr = args;
            }
        } else {
            itemIdStr = args;
        }
        
        ResourceLocation itemId = new ResourceLocation(itemIdStr);
        final int finalCount = count;
        
        return (player, session, npcEntityId) -> {
            try {
                var registry = player.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ITEM);
                var item = registry.get(itemId);
                
                if (item != null) {
                    int remaining = finalCount;
                    for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (stack.is(item)) {
                            int toRemove = Math.min(remaining, stack.getCount());
                            stack.shrink(toRemove);
                            remaining -= toRemove;
                        }
                    }
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("扣除物品失败: {}", itemId, e);
            }
        };
    }
    
    /**
     * 解析给予经验动作
     * 格式: add_xp:amount
     */
    private static DialogAction parseAddXp(String args) {
        int xp;
        try {
            xp = Integer.parseInt(args);
        } catch (NumberFormatException e) {
            RoadWeaverRPG.LOGGER.warn("无效的add_xp参数: {}", args);
            return DialogAction.NONE;
        }
        
        return (player, session, npcEntityId) -> {
            player.giveExperiencePoints(xp);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§a获得了 " + xp + " 点经验"), false);
        };
    }
    
    /**
     * 解析执行命令动作
     * 格式: run_command:/command args
     */
    private static DialogAction parseRunCommand(String args) {
        // 移除开头的斜杠（如果有）
        String command = args.startsWith("/") ? args.substring(1) : args;
        
        return (player, session, npcEntityId) -> {
            try {
                var server = player.getServer();
                if (server != null) {
                    // 替换占位符
                    String finalCommand = command
                            .replace("{player}", player.getName().getString())
                            .replace("{npc}", String.valueOf(npcEntityId));
                    
                    server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack().withSuppressedOutput(),
                            finalCommand);
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("执行命令失败: {}", command, e);
            }
        };
    }
    
    /**
     * 解析跳转对话动作
     * 格式: jump:modid:dialog_id
     */
    private static DialogAction parseJump(String args) {
        ResourceLocation dialogId = new ResourceLocation(args);
        return (player, session, npcEntityId) -> {
            // 跳转由 DialogManager 处理，这里只记录日志
            RoadWeaverRPG.LOGGER.debug("跳转到对话: {}", dialogId);
        };
    }
    
    /**
     * 解析发送消息动作
     * 格式: send_message:消息内容
     */
    private static DialogAction parseSendMessage(String args) {
        return (player, session, npcEntityId) -> {
            String message = args
                    .replace("{player}", player.getName().getString());
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(message), false);
        };
    }
    
    /**
     * 检查动作是否存在
     */
    public static boolean hasAction(String id) {
        if (id == null) return false;
        if (ACTIONS.containsKey(id)) return true;
        
        // 检查参数化动作
        Matcher matcher = PARAM_PATTERN.matcher(id);
        if (matcher.matches()) {
            return PARSERS.containsKey(matcher.group(1));
        }
        return false;
    }
    
    /**
     * 获取所有已注册的动作ID
     */
    public static Set<String> getAllActionIds() {
        return Collections.unmodifiableSet(ACTIONS.keySet());
    }
    
    /**
     * 获取所有已注册的解析器类型
     */
    public static Set<String> getAllParserTypes() {
        return Collections.unmodifiableSet(PARSERS.keySet());
    }
    
    /**
     * 移除动作
     */
    public static void unregister(String id) {
        ACTIONS.remove(id);
    }
    
    /**
     * 清空所有动作
     */
    public static void clear() {
        ACTIONS.clear();
        PARSERS.clear();
        initialized = false;
    }
    
    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
