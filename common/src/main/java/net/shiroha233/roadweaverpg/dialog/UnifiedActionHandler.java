package net.shiroha233.roadweaverpg.dialog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.init.QuestSystemInitializer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 统一动作处理器
 * 职责：统一管理对话动作和交互功能处理器
 * 原理：通过回调机制和注册表模式实现可扩展的动作处理
 * 
 * 改进点：
 * - 移除对弃用 DialogEvents 的依赖
 * - 使用回调机制实现平台特定的功能
 * - 支持自定义动作处理器注册
 */
public final class UnifiedActionHandler {
    
    private UnifiedActionHandler() {}
    
    // 功能处理器映射
    private static final ConcurrentHashMap<ResourceLocation, BiConsumer<ServerPlayer, Integer>> 
            HANDLERS = new ConcurrentHashMap<>();
    
    // 动作别名映射（字符串ID -> ResourceLocation）
    private static final ConcurrentHashMap<String, ResourceLocation> ALIASES = new ConcurrentHashMap<>();
    
    /**
     * 初始化默认处理器
     * 
     * 原理：注册内置的委托系统和商店功能处理器
     * 这些处理器通过回调机制与平台特定代码交互
     */
    public static void init() {
        // 委托系统功能 - 显示委托看板
        registerWithAlias("show_quests", 
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "show_quests"),
                (player, npcId) -> {
                    var callback = QuestSystemInitializer.getOnShowQuestsBoard();
                    if (callback != null) {
                        var quests = net.shiroha233.roadweaverpg.quest.service.PlayerQuestService
                                .getInstance().getAvailableQuests(player);
                        callback.accept(player, quests);
                    }
                });
        
        // 委托系统功能 - 提交委托
        registerWithAlias("complete_quest",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "complete_quest"),
                (player, npcId) -> {
                    var entity = player.level().getEntity(npcId);
                    if (entity instanceof net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity maid) {
                        net.shiroha233.roadweaverpg.network.QuestPacketHandler.handleQuestTurnIn(player, maid);
                    }
                });
        
        // 委托系统功能 - 找回委托书
        registerWithAlias("retrieve_scroll",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "retrieve_scroll"),
                (player, npcId) -> {
                    var entity = player.level().getEntity(npcId);
                    if (entity instanceof net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity maid) {
                        net.shiroha233.roadweaverpg.quest.service.PlayerQuestService
                                .getInstance().retrieveLostScrolls(player, maid);
                    }
                });
        
        // 委托系统功能 - 查看声望
        registerWithAlias("view_reputation",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "view_reputation"),
                (player, npcId) -> {
                    net.shiroha233.roadweaverpg.quest.event.QuestEventHandler.syncReputationDefinitions(player);
                    net.shiroha233.roadweaverpg.quest.event.QuestEventHandler.syncPlayerReputation(player);
                    net.shiroha233.roadweaverpg.network.QuestPacketHandler.openReputationGui(player);
                });
        
        // 冒险等级功能 - 查看冒险等级
        registerWithAlias("view_adventure_level",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "view_adventure_level"),
                (player, npcId) -> {
                    // 同步冒险等级数据到客户端
                    net.shiroha233.roadweaverpg.adventure.AdventureEventHandler.syncAdventureLevelDefinitions(player);
                    net.shiroha233.roadweaverpg.adventure.AdventureEventHandler.syncPlayerAdventureData(player);
                    // 打开冒险等级GUI
                    net.shiroha233.roadweaverpg.network.QuestPacketHandler.openAdventureLevelGui(player);
                });
        
        // 商店功能 - 打开商店
        registerWithAlias("open_shop",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_shop"),
                (player, npcId) -> {
                    var callback = QuestSystemInitializer.getOnOpenShop();
                    if (callback != null) {
                        callback.accept(player, npcId);
                    }
                });
        
        // 职业系统功能 - 冒险家注册（打开职业选择界面）
        registerWithAlias("register_adventurer",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "register_adventurer"),
                (player, npcId) -> {
                    // 检查是否已注册
                    if (net.shiroha233.roadweaverpg.profession.ProfessionDataService.getInstance().hasProfession(player)) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.roadweaver_rpg.already_adventurer"));
                        return;
                    }
                    // 同步职业定义到客户端
                    net.shiroha233.roadweaverpg.profession.ProfessionEventHandler.syncProfessionDefinitions(player);
                    // 打开职业选择界面
                    var callback = QuestSystemInitializer.getOnOpenProfessionSelection();
                    if (callback != null) {
                        callback.accept(player, npcId);
                    }
                });
        
        RoadWeaverRPG.LOGGER.info("已注册 {} 个统一动作处理器", HANDLERS.size());
    }
    
    /**
     * 注册处理器（带别名）
     */
    public static void registerWithAlias(String alias, ResourceLocation id, BiConsumer<ServerPlayer, Integer> handler) {
        HANDLERS.put(id, handler);
        ALIASES.put(alias, id);
    }
    
    /**
     * 注册处理器
     */
    public static void register(ResourceLocation id, BiConsumer<ServerPlayer, Integer> handler) {
        HANDLERS.put(id, handler);
    }
    
    /**
     * 通过ResourceLocation执行动作
     */
    public static boolean execute(ServerPlayer player, int npcEntityId, ResourceLocation actionId) {
        BiConsumer<ServerPlayer, Integer> handler = HANDLERS.get(actionId);
        if (handler != null) {
            try {
                handler.accept(player, npcEntityId);
                return true;
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("执行动作失败: {}", actionId, e);
            }
        } else {
            RoadWeaverRPG.LOGGER.warn("未找到动作处理器: {}", actionId);
        }
        return false;
    }
    
    /**
     * 通过别名执行动作
     */
    public static boolean executeByAlias(ServerPlayer player, int npcEntityId, String alias) {
        ResourceLocation id = ALIASES.get(alias);
        if (id != null) {
            return execute(player, npcEntityId, id);
        }
        RoadWeaverRPG.LOGGER.warn("未找到动作别名: {}", alias);
        return false;
    }
    
    /**
     * 获取处理器
     */
    public static BiConsumer<ServerPlayer, Integer> getHandler(ResourceLocation id) {
        return HANDLERS.get(id);
    }
    
    /**
     * 通过别名获取处理器
     */
    public static BiConsumer<ServerPlayer, Integer> getHandlerByAlias(String alias) {
        ResourceLocation id = ALIASES.get(alias);
        return id != null ? HANDLERS.get(id) : null;
    }
    
    /**
     * 检查处理器是否存在
     */
    public static boolean hasHandler(ResourceLocation id) {
        return HANDLERS.containsKey(id);
    }
    
    /**
     * 检查别名是否存在
     */
    public static boolean hasAlias(String alias) {
        return ALIASES.containsKey(alias);
    }
    
    /**
     * 获取所有已注册的动作ID
     */
    public static Set<ResourceLocation> getAllActionIds() {
        return Collections.unmodifiableSet(HANDLERS.keySet());
    }
    
    /**
     * 获取所有别名
     */
    public static Set<String> getAllAliases() {
        return Collections.unmodifiableSet(ALIASES.keySet());
    }
    
    /**
     * 移除处理器
     */
    public static void unregister(ResourceLocation id) {
        HANDLERS.remove(id);
        ALIASES.entrySet().removeIf(e -> e.getValue().equals(id));
    }
    
    /**
     * 清空所有处理器
     */
    public static void clear() {
        HANDLERS.clear();
        ALIASES.clear();
    }
}
