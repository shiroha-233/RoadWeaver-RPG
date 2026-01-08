package net.shiroha233.roadweaverpg.entity.npc.action;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * NPC动作类型枚举
 * 职责：定义所有可用的NPC动作类型
 * 原理：基于TouhouLittleMaid的Task系统，映射到对应的动画
 */
public enum NPCActionType {
    
    // 基础动作
    IDLE("idle", "touhoulittlemaid:idle", 0),
    WALK("walk", "touhoulittlemaid:idle", 0),
    
    // 交互动作
    GREETING("greeting", "touhoulittlemaid:idle", 60),      // 打招呼
    BOW("bow", "touhoulittlemaid:idle", 40),                // 鞠躬
    WAVE("wave", "touhoulittlemaid:idle", 30),              // 挥手
    NOD("nod", "touhoulittlemaid:idle", 20),                // 点头
    SHAKE_HEAD("shake_head", "touhoulittlemaid:idle", 20),  // 摇头
    
    // 情感动作
    HAPPY("happy", "touhoulittlemaid:idle", 40),            // 开心
    SAD("sad", "touhoulittlemaid:idle", 60),                // 悲伤
    ANGRY("angry", "touhoulittlemaid:idle", 40),            // 生气
    SURPRISED("surprised", "touhoulittlemaid:idle", 30),    // 惊讶
    THINKING("thinking", "touhoulittlemaid:idle", 80),      // 思考
    
    // 工作动作
    WORKING("working", "touhoulittlemaid:idle", 0),         // 工作中（循环）
    CRAFTING("crafting", "touhoulittlemaid:idle", 0),       // 制作中（循环）
    
    // 战斗动作
    ATTACK("attack", "touhoulittlemaid:attack", 20),        // 攻击
    DEFEND("defend", "touhoulittlemaid:idle", 30),          // 防御
    DRAW_WEAPON("draw_weapon", "touhoulittlemaid:attack", 15), // 拔武器
    SHEATHE_WEAPON("sheathe_weapon", "touhoulittlemaid:idle", 15); // 收武器
    
    private final String id;
    private final String taskId;  // 对应TouhouLittleMaid的Task ID
    private final int durationTicks; // 动作持续时间（0表示循环）
    
    NPCActionType(String id, String taskId, int durationTicks) {
        this.id = id;
        this.taskId = taskId;
        this.durationTicks = durationTicks;
    }
    
    public String getId() {
        return id;
    }
    
    public ResourceLocation getResourceLocation() {
        return new ResourceLocation(RoadWeaverRPG.MOD_ID, "action/" + id);
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public ResourceLocation getTaskResourceLocation() {
        return new ResourceLocation(taskId);
    }
    
    public int getDurationTicks() {
        return durationTicks;
    }
    
    public boolean isLooping() {
        return durationTicks == 0;
    }
    
    /**
     * 根据ID查找动作类型
     */
    public static NPCActionType fromId(String id) {
        for (NPCActionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return IDLE;
    }
}
