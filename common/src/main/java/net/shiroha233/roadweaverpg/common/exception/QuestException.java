package net.shiroha233.roadweaverpg.common.exception;

import net.minecraft.resources.ResourceLocation;

/**
 * 委托系统异常基类
 */
public class QuestException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final ResourceLocation questId;
    
    public QuestException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.questId = null;
    }
    
    public QuestException(ErrorCode errorCode, ResourceLocation questId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.questId = questId;
    }
    
    public QuestException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.questId = null;
    }
    
    public ErrorCode getErrorCode() { return errorCode; }
    public ResourceLocation getQuestId() { return questId; }
    
    /** 错误码枚举 */
    public enum ErrorCode {
        DEFINITION_NOT_FOUND("quest.error.definition_not_found"),
        DEFINITION_INVALID("quest.error.definition_invalid"),
        QUEST_ALREADY_ACTIVE("quest.error.already_active"),
        QUEST_NOT_ACTIVE("quest.error.not_active"),
        QUEST_NOT_COMPLETED("quest.error.not_completed"),
        QUEST_ON_COOLDOWN("quest.error.on_cooldown"),
        QUEST_PREREQUISITES_NOT_MET("quest.error.prerequisites_not_met"),
        QUEST_NOT_REPEATABLE("quest.error.not_repeatable"),
        QUEST_EXPIRED("quest.error.expired"),
        REPUTATION_TOO_LOW("quest.error.reputation_too_low"),
        NOT_DAILY_QUEST("quest.error.not_daily_quest"),
        QUEST_ONE_TIME_COMPLETED("quest.error.one_time_completed"),
        QUEST_ACCEPT_LIMIT_REACHED("quest.error.accept_limit_reached"),
        SCROLL_INVALID("quest.error.scroll_invalid"),
        SCROLL_NOT_FOUND("quest.error.scroll_not_found"),
        ITEM_NOT_REGISTERED("quest.error.item_not_registered"),
        REWARD_CANNOT_GRANT("quest.error.reward_cannot_grant"),
        DATA_LOAD_FAILED("quest.error.data_load_failed"),
        DATA_SAVE_FAILED("quest.error.data_save_failed"),
        SERIALIZATION_FAILED("quest.error.serialization_failed"),
        NETWORK_SYNC_FAILED("quest.error.network_sync_failed"),
        // 事务相关
        TRANSACTION_ALREADY_COMMITTED("quest.error.transaction_already_committed"),
        TRANSACTION_EXECUTION_FAILED("quest.error.transaction_execution_failed"),
        TRANSACTION_ROLLBACK_FAILED("quest.error.transaction_rollback_failed"),
        UNKNOWN_ERROR("quest.error.unknown");
        
        private final String translationKey;
        
        ErrorCode(String translationKey) {
            this.translationKey = translationKey;
        }
        
        public String getTranslationKey() { return translationKey; }
    }
}
