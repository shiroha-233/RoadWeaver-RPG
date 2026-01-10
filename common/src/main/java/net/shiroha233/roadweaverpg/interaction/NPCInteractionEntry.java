package net.shiroha233.roadweaverpg.interaction;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * NPC交互入口
 * 职责：定义单个交互菜单项
 * 原理：数据驱动的交互入口，支持JSON配置和条件过滤
 */
public record NPCInteractionEntry(
        String id,                    // 唯一标识
        Component displayName,        // 显示名称
        Component description,        // 描述（可选）
        String iconType,              // 图标类型
        int priority,                 // 排序优先级（越小越靠前）
        String actionType,            // 动作类型
        ResourceLocation actionData,  // 动作数据（如对话ID）
        String condition              // 显示条件（可选）
) {
    
    /**
     * 动作类型常量
     */
    public static final String ACTION_DIALOG = "dialog";      // 打开对话
    public static final String ACTION_FUNCTION = "function";  // 执行功能
    public static final String ACTION_MENU = "menu";          // 打开子菜单
    
    /**
     * 图标类型常量
     */
    public static final String ICON_CHAT = "chat";            // 对话图标
    public static final String ICON_QUEST = "quest";          // 委托图标
    public static final String ICON_SHOP = "shop";            // 商店图标
    public static final String ICON_INFO = "info";            // 信息图标
    public static final String ICON_REPUTATION = "reputation"; // 声望图标
    public static final String ICON_PROFESSION = "profession"; // 职业图标
    
    /**
     * 条件类型常量
     */
    public static final String CONDITION_NONE = "";                    // 无条件
    public static final String CONDITION_IS_ADVENTURER = "is_adventurer";      // 已注册冒险家
    public static final String CONDITION_NOT_ADVENTURER = "not_adventurer";    // 未注册冒险家
    
    /**
     * 创建对话入口
     */
    public static NPCInteractionEntry dialog(String id, String nameKey, ResourceLocation dialogId, int priority) {
        return new NPCInteractionEntry(
                id,
                Component.translatable(nameKey),
                Component.empty(),
                ICON_CHAT,
                priority,
                ACTION_DIALOG,
                dialogId,
                CONDITION_NONE
        );
    }
    
    /**
     * 创建功能入口
     */
    public static NPCInteractionEntry function(String id, String nameKey, String iconType, 
                                                ResourceLocation functionId, int priority) {
        return new NPCInteractionEntry(
                id,
                Component.translatable(nameKey),
                Component.empty(),
                iconType,
                priority,
                ACTION_FUNCTION,
                functionId,
                CONDITION_NONE
        );
    }
    
    /**
     * 创建带条件的功能入口
     */
    public static NPCInteractionEntry functionWithCondition(String id, String nameKey, String iconType, 
                                                             ResourceLocation functionId, int priority,
                                                             String condition) {
        return new NPCInteractionEntry(
                id,
                Component.translatable(nameKey),
                Component.empty(),
                iconType,
                priority,
                ACTION_FUNCTION,
                functionId,
                condition
        );
    }
    
    /**
     * 创建带条件的对话入口
     */
    public static NPCInteractionEntry dialogWithCondition(String id, String nameKey, ResourceLocation dialogId, 
                                                           int priority, String condition) {
        return new NPCInteractionEntry(
                id,
                Component.translatable(nameKey),
                Component.empty(),
                ICON_CHAT,
                priority,
                ACTION_DIALOG,
                dialogId,
                condition
        );
    }
    
    /**
     * 创建带描述的入口
     */
    public static NPCInteractionEntry withDescription(String id, String nameKey, String descKey,
                                                       String iconType, String actionType,
                                                       ResourceLocation actionData, int priority) {
        return new NPCInteractionEntry(
                id,
                Component.translatable(nameKey),
                Component.translatable(descKey),
                iconType,
                priority,
                actionType,
                actionData,
                CONDITION_NONE
        );
    }
    
    /**
     * 检查是否有条件限制
     */
    public boolean hasCondition() {
        return condition != null && !condition.isEmpty();
    }
}
