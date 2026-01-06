package net.shiroha233.roadweaverpg.quest.reward;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 委托奖励接口
 */
public interface QuestReward {
    
    RewardType getType();
    Component getDescription();
    void grant(ServerPlayer player);
    boolean canGrant(ServerPlayer player);
    void toNetwork(FriendlyByteBuf buf);
    JsonObject toJson();
}
