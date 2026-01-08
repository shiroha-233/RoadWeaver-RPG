package net.shiroha233.roadweaverpg.quest.objective;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.quest.type.QuestType;

/**
 * 委托目标接口
 * 
 * 设计原理：
 * - 定义目标的基本属性和序列化方法
 * - 进度检查逻辑由 ObjectiveProgressChecker 统一处理
 * - 各目标类只需提供必要的配置信息
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
}
