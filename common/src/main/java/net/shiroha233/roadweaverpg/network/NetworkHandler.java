package net.shiroha233.roadweaverpg.network;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 网络通信处理器 - 定义通道和消息类型
 */
public class NetworkHandler {
    
    public static final ResourceLocation CHANNEL_ID = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "main");
    
    // 消息类型 ID
    public static final ResourceLocation OPEN_DIALOG = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_dialog");
    public static final ResourceLocation DIALOG_RESPONSE = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "dialog_response");
    public static final ResourceLocation SYNC_QUESTS = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_quests");
    public static final ResourceLocation OPEN_QUEST_BOARD = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_quest_board");
    public static final ResourceLocation ACCEPT_QUEST = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "accept_quest");
    public static final ResourceLocation SYNC_QUEST_INSTANCE = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_quest_instance");
    public static final ResourceLocation TURN_IN_QUEST = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "turn_in_quest");
    public static final ResourceLocation OPEN_QUEST_PROGRESS = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_quest_progress");
    public static final ResourceLocation REQUEST_QUEST_PROGRESS = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "request_quest_progress");
    public static final ResourceLocation SYNC_ALL_QUEST_INSTANCES = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_all_quest_instances");
    public static final ResourceLocation SYNC_REPUTATION_LEVELS = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_reputation_levels");
    public static final ResourceLocation SYNC_PLAYER_REPUTATION = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_player_reputation");
    public static final ResourceLocation OPEN_REPUTATION_GUI = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_reputation_gui");
    
    // 每日委托系统
    public static final ResourceLocation SYNC_DAILY_QUESTS = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_daily_quests");
    
    // 商店系统消息类型
    public static final ResourceLocation OPEN_SHOP_DIALOG = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_shop_dialog");
    public static final ResourceLocation SHOP_DIALOG_RESPONSE = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop_dialog_response");
    public static final ResourceLocation OPEN_SHOP = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_shop");
    public static final ResourceLocation SHOP_PURCHASE = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop_purchase");
    public static final ResourceLocation SYNC_COINS = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, "sync_coins");
    
    private NetworkHandler() {}
}
