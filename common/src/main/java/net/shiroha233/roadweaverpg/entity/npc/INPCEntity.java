package net.shiroha233.roadweaverpg.entity.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * NPC实体接口
 * 职责：定义所有NPC实体的通用行为
 * 原理：接口隔离原则 - 只定义必要的方法
 */
public interface INPCEntity {
    
    /**
     * 获取NPC类型
     */
    NPCType getNPCType();
    
    /**
     * 获取NPC显示名称
     */
    Component getNPCDisplayName();
    
    /**
     * 获取NPC使用的模型ID
     */
    String getModelId();
    
    /**
     * 处理玩家交互
     * @param player 交互的玩家
     */
    void handlePlayerInteraction(ServerPlayer player);
    
    /**
     * 获取实体引用（用于渲染等）
     */
    com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid asEntity();
    
    /**
     * NPC类型枚举
     */
    enum NPCType {
        GUILD_MAID("guild_maid"),      // 公会女仆
        SHOP_MAID("shop_maid"),        // 商店女仆
        QUEST_GIVER("quest_giver"),    // 委托发布者
        MERCHANT("merchant"),          // 商人
        GUARD("guard");                // 守卫
        
        private final String id;
        
        NPCType(String id) {
            this.id = id;
        }
        
        public String getId() {
            return id;
        }
    }
}
