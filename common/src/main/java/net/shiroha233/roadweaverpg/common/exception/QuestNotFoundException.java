package net.shiroha233.roadweaverpg.common.exception;

import net.minecraft.resources.ResourceLocation;

/**
 * 委托未找到异常
 */
public class QuestNotFoundException extends QuestException {
    
    public QuestNotFoundException(ResourceLocation questId) {
        super(ErrorCode.DEFINITION_NOT_FOUND, questId, 
              "Quest definition not found: " + questId);
    }
}
