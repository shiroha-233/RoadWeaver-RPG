package net.shiroha233.roadweaverpg.common.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
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
    
    // 默认声望派系
    private static final ResourceLocation DEFAULT_FACTION = new ResourceLocation(RoadWeaverRPG.MOD_ID, "guild");
    
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
    
    /**
     * 验证声望等级是否满足委托要求
     */
    public static Result<Void> validateReputationLevel(QuestDefinition def, PlayerQuestData data) {
        if (!def.hasReputationRequirement()) {
            return Result.success(null);
        }
        
        ResourceLocation faction = def.getReputationFaction() != null 
                ? def.getReputationFaction() 
                : DEFAULT_FACTION;
        
        int playerLevel = data.getReputationLevel(faction);
        int requiredLevel = def.getMinReputationLevel();
        
        if (playerLevel < requiredLevel) {
            return Result.failure(QuestException.ErrorCode.REPUTATION_TOO_LOW,
                    String.format("Reputation level %d required, player has %d", requiredLevel, playerLevel));
        }
        return Result.success(null);
    }
    
    /**
     * 检查玩家是否满足委托的声望等级要求
     */
    public static boolean meetsReputationRequirement(QuestDefinition def, PlayerQuestData data) {
        return validateReputationLevel(def, data).isSuccess();
    }
    
    /**
     * 验证单次任务是否可领取（完成后不可再领取）
     */
    public static Result<Void> validateOneTimeQuest(QuestDefinition def, PlayerQuestData data) {
        if (def.isOneTime() && data.hasCompletedQuest(def.getId())) {
            return Result.failure(QuestException.ErrorCode.QUEST_ONE_TIME_COMPLETED,
                    "One-time quest already completed: " + def.getId());
        }
        return Result.success(null);
    }
    
    /**
     * 验证周期内领取次数限制
     */
    public static Result<Void> validateAcceptLimit(QuestDefinition def, PlayerQuestData data) {
        if (!def.hasAcceptLimit()) {
            return Result.success(null);
        }
        
        if (data.hasReachedAcceptLimit(def.getId(), def.getMaxAcceptPerPeriod(), def.getAcceptPeriodSeconds())) {
            int remaining = data.getNextAcceptCooldown(def.getId(), def.getMaxAcceptPerPeriod(), def.getAcceptPeriodSeconds());
            return Result.failure(QuestException.ErrorCode.QUEST_ACCEPT_LIMIT_REACHED,
                    String.format("Accept limit reached for quest %s, next available in %d seconds", 
                            def.getId(), remaining));
        }
        return Result.success(null);
    }
    
    /**
     * 检查委托是否可被刷新出来（用于每日委托刷新）
     */
    public static boolean canQuestBeRefreshed(QuestDefinition def, PlayerQuestData data) {
        // 单次任务完成后不再刷新
        if (def.isOneTime() && data.hasCompletedQuest(def.getId())) {
            return false;
        }
        return true;
    }
}
