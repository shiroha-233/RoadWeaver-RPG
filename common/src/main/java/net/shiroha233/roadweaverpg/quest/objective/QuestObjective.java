package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

/**
 * 委托目标接口
 */
public interface QuestObjective {
    
    QuestType getType();
    String getId();
    Component getDescription();
    Component getDisplayText(int currentProgress);
    int getRequiredAmount();
    
    default boolean isComplete(int currentProgress) {
        return currentProgress >= getRequiredAmount();
    }
    
    ResourceLocation getTargetResource();
    void toNetwork(FriendlyByteBuf buf);
    JsonObject toJson();
    
    /**
     * 检查事件是否匹配此目标
     * @return 匹配则返回增加的进度，否则返回0
     */
    int checkProgress(ServerPlayer player, String eventType, Object eventData);
}
