package net.shiroha233.roadweaverpg.common.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.common.exception.QuestException;
import net.shiroha233.roadweaverpg.common.result.Result;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;

import java.util.Optional;

/**
 * 验证工具类
 */
public final class ValidationUtils {
    
    private ValidationUtils() {}
    
    public static Result<QuestDefinition> validateDefinitionExists(ResourceLocation questId) {
        QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(questId);
        if (def == null) {
            return Result.failure(QuestException.ErrorCode.DEFINITION_NOT_FOUND,
                    "Quest definition not found: " + questId);
        }
        return Result.success(def);
    }
    
    public static Result<Void> validateNotActiveQuest(PlayerQuestData data, ResourceLocation questId) {
        if (data.hasActiveQuest(questId)) {
            return Result.failure(QuestException.ErrorCode.QUEST_ALREADY_ACTIVE,
                    "Player already has active quest: " + questId);
        }
        return Result.success(null);
    }
    
    public static Result<QuestInstance> validateHasActiveQuest(PlayerQuestData data, ResourceLocation questId) {
        QuestInstance instance = data.getActiveQuest(questId);
        if (instance == null) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_ACTIVE,
                    "Player does not have active quest: " + questId);
        }
        return Result.success(instance);
    }
    
    public static Result<QuestInstance> validateQuestState(QuestInstance instance, QuestState expected) {
        if (instance.getState() != expected) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_COMPLETED,
                    String.format("Quest %s is in state %s, expected %s", 
                            instance.getQuestId(), instance.getState(), expected));
        }
        return Result.success(instance);
    }
    
    public static Result<Void> validatePrerequisites(ResourceLocation questId, PlayerQuestData data) {
        QuestDefinitionLoader defManager = QuestDefinitionLoader.getInstance();
        if (!defManager.checkPrerequisites(questId, data.getCompletedQuests())) {
            return Result.failure(QuestException.ErrorCode.QUEST_PREREQUISITES_NOT_MET,
                    "Prerequisites not met for quest: " + questId);
        }
        return Result.success(null);
    }
    
    public static Result<Void> validateCooldown(PlayerQuestData data, ResourceLocation questId) {
        if (data.isOnCooldown(questId)) {
            int remaining = data.getCooldownRemaining(questId);
            return Result.failure(QuestException.ErrorCode.QUEST_ON_COOLDOWN,
                    String.format("Quest %s is on cooldown for %d seconds", questId, remaining));
        }
        return Result.success(null);
    }
    
    public static Result<Void> validateRepeatable(QuestDefinition def, PlayerQuestData data) {
        if (data.hasCompletedQuest(def.getId()) && !def.isRepeatable()) {
            return Result.failure(QuestException.ErrorCode.QUEST_NOT_REPEATABLE,
                    "Quest is not repeatable: " + def.getId());
        }
        return Result.success(null);
    }
    
    public static Result<ResourceLocation> validateQuestScroll(ItemStack scroll) {
        if (!(scroll.getItem() instanceof QuestScrollItem) || !QuestScrollItem.hasQuest(scroll)) {
            return Result.failure(QuestException.ErrorCode.SCROLL_INVALID, "Invalid quest scroll");
        }
        Optional<ResourceLocation> questId = QuestScrollItem.getQuestId(scroll);
        if (questId.isEmpty()) {
            return Result.failure(QuestException.ErrorCode.SCROLL_INVALID, "Quest scroll has no quest ID");
        }
        return Result.success(questId.get());
    }
    
    public static Result<QuestInstance> validateNotExpired(QuestInstance instance) {
        if (instance.isExpired()) {
            return Result.failure(QuestException.ErrorCode.QUEST_EXPIRED,
                    "Quest has expired: " + instance.getQuestId());
        }
        return Result.success(instance);
    }
}
