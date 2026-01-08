package net.shiroha233.roadweaverpg.playerlevel.effect;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 等级效果接口
 * 定义等级提升时可获得的各种增益效果
 */
public interface LevelEffect {
    
    /**
     * 获取效果类型ID
     */
    String getTypeId();
    
    /**
     * 获取效果描述
     */
    Component getDescription();
    
    /**
     * 应用效果到玩家
     * @param player 目标玩家
     * @param level 当前等级
     */
    void apply(ServerPlayer player, int level);
    
    /**
     * 移除效果（用于等级重置等场景）
     */
    void remove(ServerPlayer player);
    
    /**
     * 刷新效果（玩家登录或等级变化时调用）
     */
    default void refresh(ServerPlayer player, int level) {
        apply(player, level);
    }
    
    /**
     * 序列化到网络
     */
    void toNetwork(FriendlyByteBuf buf);
    
    /**
     * 序列化到JSON
     */
    JsonObject toJson();
}
