package net.shiroha233.roadweaverpg.init;

import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.condition.ConditionRegistry;
import net.shiroha233.roadweaverpg.dialog.DialogActionRegistry;
import net.shiroha233.roadweaverpg.dialog.DialogManager;
import net.shiroha233.roadweaverpg.dialog.UnifiedActionHandler;
import net.shiroha233.roadweaverpg.quest.event.QuestEventBus;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionHandler;
import net.shiroha233.roadweaverpg.interaction.NPCInteractionRegistry;
import net.shiroha233.roadweaverpg.quest.service.PlayerQuestService;

/**
 * 委托系统初始化器
 * 统一管理所有组件的初始化和关闭
 */
public final class QuestSystemInitializer {
    
    private static boolean initialized = false;
    
    private QuestSystemInitializer() {}
    
    public static void initialize() {
        if (initialized) {
            RoadWeaverRPG.LOGGER.warn("Quest system already initialized!");
            return;
        }
        
        RoadWeaverRPG.LOGGER.info("Initializing Quest System...");
        
        initializeConditionSystem();
        initializeRegistries();
        initializeServices();
        initializeEventBus();
        initializeCache();
        initializeInteractionSystem();
        
        initialized = true;
        RoadWeaverRPG.LOGGER.info("Quest System initialized successfully!");
    }
    
    /**
     * 初始化统一条件系统
     * 
     * 原理：条件系统是其他系统的基础，需要最先初始化
     * 提供位置、时间、天气、玩家状态等多种条件判定
     */
    private static void initializeConditionSystem() {
        ConditionRegistry.init();
        RoadWeaverRPG.LOGGER.debug("Condition system initialized");
    }
    
    private static void initializeRegistries() {
        // 显式初始化目标注册表（V2重构）
        net.shiroha233.roadweaverpg.quest.objective.ObjectiveRegistry.init();
        
        // 加载奖励注册表
        try {
            Class.forName("net.shiroha233.roadweaverpg.quest.reward.RewardRegistry");
        } catch (ClassNotFoundException e) {
            RoadWeaverRPG.LOGGER.error("Failed to load RewardRegistry", e);
        }
        RoadWeaverRPG.LOGGER.debug("Registries initialized");
    }
    
    private static void initializeServices() {
        PlayerQuestService.getInstance();
        RoadWeaverRPG.LOGGER.debug("Services initialized");
    }
    
    private static void initializeEventBus() {
        QuestEventBus.getInstance();
        RoadWeaverRPG.LOGGER.debug("Event bus initialized");
    }
    
    private static void initializeCache() {
        // 缓存初始化由各个服务自行管理
        RoadWeaverRPG.LOGGER.debug("Cache initialized");
    }
    
    /**
     * 初始化NPC交互系统
     */
    private static void initializeInteractionSystem() {
        // 初始化对话动作注册表（包含条件系统）
        DialogActionRegistry.init();
        // 初始化统一动作处理器
        UnifiedActionHandler.init();
        // 初始化NPC交互注册表
        NPCInteractionRegistry.init();
        // 初始化NPC交互处理器
        NPCInteractionHandler.init();
        // 初始化对话事件监听器
        initializeDialogEventListeners();
        
        RoadWeaverRPG.LOGGER.debug("Interaction system initialized");
    }
    
    /**
     * 初始化对话事件监听器
     * 
     * 原理：使用 QuestEventBus 统一事件系统，支持优先级和异步处理
     */
    private static void initializeDialogEventListeners() {
        QuestEventBus eventBus = QuestEventBus.getInstance();
        
        // 订阅对话选择事件
        eventBus.subscribe(
                net.shiroha233.roadweaverpg.dialog.event.DialogChoiceEvent.class,
                event -> handleDialogChoice(event),
                QuestEventBus.Priority.NORMAL
        );
        
        // 订阅对话结束事件
        eventBus.subscribe(
                net.shiroha233.roadweaverpg.dialog.event.DialogEndEvent.class,
                event -> handleDialogEnd(event),
                QuestEventBus.Priority.LOW
        );
        
        RoadWeaverRPG.LOGGER.debug("Dialog event listeners initialized");
    }
    
    /**
     * 初始化对话网络处理器
     * 
     * 注意：此方法由平台特定代码（Fabric/Forge）调用，用于注入网络发送器
     */
    public static void initializeDialogNetworkHandler(
            java.util.function.BiConsumer<ServerPlayer, net.shiroha233.roadweaverpg.dialog.DialogNetworkHandler.DialogPacketData> dataSender,
            java.util.function.BiConsumer<ServerPlayer, net.shiroha233.roadweaverpg.dialog.DialogNetworkHandler.DialogLinePacketData> lineSender,
            java.util.function.BiConsumer<ServerPlayer, net.shiroha233.roadweaverpg.dialog.DialogNetworkHandler.DialogChoicesPacketData> choicesSender,
            java.util.function.Consumer<ServerPlayer> sessionCloseSender
    ) {
        net.shiroha233.roadweaverpg.dialog.DialogNetworkHandler.init(dataSender, lineSender, choicesSender);
        net.shiroha233.roadweaverpg.dialog.DialogNetworkHandler.setSessionCloseSender(sessionCloseSender);
        RoadWeaverRPG.LOGGER.debug("Dialog network handler initialized");
    }
    
    /**
     * 处理对话选择事件
     */
    private static void handleDialogChoice(net.shiroha233.roadweaverpg.dialog.event.DialogChoiceEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;
        
        var choice = event.getChoice();
        
        // 根据选择的动作类型处理
        if (choice == null) return;
        
        // 这里可以根据选择的内容进行相应的处理
        // 具体逻辑由 DialogChoiceEvent 的实现决定
    }
    
    /**
     * 处理对话结束事件
     */
    private static void handleDialogEnd(net.shiroha233.roadweaverpg.dialog.event.DialogEndEvent event) {
        // 对话结束时的清理工作
        RoadWeaverRPG.LOGGER.debug("Dialog ended for player: {}", event.getPlayerId());
    }
    
    // ==================== 平台特定回调 ====================
    
    /**
     * 显示委托看板的回调（由 Fabric/Forge 实现）
     */
    private static java.util.function.BiConsumer<ServerPlayer, java.util.List<net.shiroha233.roadweaverpg.quest.definition.QuestDefinition>> onShowQuestsBoard;
    
    /**
     * 打开商店的回调（由 Fabric/Forge 实现）
     */
    private static java.util.function.BiConsumer<ServerPlayer, Integer> onOpenShop;
    
    /**
     * 设置显示委托看板的回调
     */
    public static void setOnShowQuestsBoard(java.util.function.BiConsumer<ServerPlayer, java.util.List<net.shiroha233.roadweaverpg.quest.definition.QuestDefinition>> callback) {
        onShowQuestsBoard = callback;
    }
    
    /**
     * 设置打开商店的回调
     */
    public static void setOnOpenShop(java.util.function.BiConsumer<ServerPlayer, Integer> callback) {
        onOpenShop = callback;
    }
    
    /**
     * 获取显示委托看板的回调
     */
    public static java.util.function.BiConsumer<ServerPlayer, java.util.List<net.shiroha233.roadweaverpg.quest.definition.QuestDefinition>> getOnShowQuestsBoard() {
        return onShowQuestsBoard;
    }
    
    /**
     * 获取打开商店的回调
     */
    public static java.util.function.BiConsumer<ServerPlayer, Integer> getOnOpenShop() {
        return onOpenShop;
    }
    
    /**
     * 关闭系统
     */
    public static void shutdown() {
        if (!initialized) return;
        
        RoadWeaverRPG.LOGGER.info("Shutting down Quest System...");
        
        // 关闭对话管理器
        DialogManager.getInstance().shutdown();
        
        // 关闭事件总线
        QuestEventBus.getInstance().shutdown();
        
        // 清理动作处理器
        UnifiedActionHandler.clear();
        DialogActionRegistry.clear();
        
        initialized = false;
        RoadWeaverRPG.LOGGER.info("Quest System shutdown complete");
    }
    
    /**
     * 玩家断线时清理
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        DialogManager.getInstance().onPlayerDisconnect(player.getUUID());
    }
    
    public static void setDebugMode(boolean enabled) {
        if (enabled) {
            RoadWeaverRPG.LOGGER.info("Quest System debug mode enabled");
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
