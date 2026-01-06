package net.shiroha233.roadweaverpg.quest.service;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.common.result.Result;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * 玩家委托服务（门面类）
 * 统一对外接口，内部委托给专职处理器
 */
public class PlayerQuestService {
    
    private static PlayerQuestService instance;
    
    private final QuestDataAccessor dataAccessor;
    private final QuestAcceptanceService acceptanceService;
    private final QuestProgressService progressService;
    private final QuestRewardService rewardService;
    private final QuestSyncService syncService;
    
    private PlayerQuestService() {
        this.dataAccessor = QuestDataAccessor.getInstance();
        this.acceptanceService = new QuestAcceptanceService(dataAccessor);
        this.progressService = new QuestProgressService(dataAccessor);
        this.rewardService = new QuestRewardService(dataAccessor);
        this.syncService = new QuestSyncService(dataAccessor);
    }
    
    public static PlayerQuestService getInstance() {
        if (instance == null) {
            instance = new PlayerQuestService();
        }
        return instance;
    }
    
    // region 回调设置
    public void setOnQuestAccepted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        acceptanceService.setOnQuestAccepted(callback);
    }
    
    public void setOnQuestUpdated(BiConsumer<ServerPlayer, QuestInstance> callback) {
        progressService.setOnQuestUpdated(callback);
        syncService.setOnSyncQuest(callback);
    }
    
    public void setOnQuestCompleted(BiConsumer<ServerPlayer, QuestInstance> callback) {
        progressService.setOnQuestCompleted(callback);
    }
    
    public void setOnQuestTurnedIn(BiConsumer<ServerPlayer, QuestInstance> callback) {
        rewardService.setOnQuestTurnedIn(callback);
    }
    
    public void setOnSyncAllQuests(BiConsumer<ServerPlayer, Collection<QuestInstance>> callback) {
        syncService.setOnSyncAllQuests(callback);
    }
    
    public void setOnSyncAllDefinitions(BiConsumer<ServerPlayer, Collection<QuestDefinition>> callback) {
        syncService.setOnSyncAllDefinitions(callback);
    }
    
    public void setOnSyncReputation(BiConsumer<ServerPlayer, PlayerQuestData> callback) {
        rewardService.setOnSyncReputation(callback);
    }
    // endregion
    
    // region 委托接取/放弃
    public Optional<QuestInstance> acceptQuest(ServerPlayer player, ResourceLocation questId) {
        Result<QuestInstance> result = acceptanceService.acceptQuest(player, questId);
        return result.getValue();
    }
    
    public boolean abandonQuest(ServerPlayer player, ResourceLocation questId) {
        return acceptanceService.abandonQuest(player, questId).isSuccess();
    }
    // endregion
    
    // region 进度更新
    public void updateProgress(ServerPlayer player, String eventType, Object eventData) {
        progressService.updateProgress(player, eventType, eventData);
    }
    
    public void checkCollectObjectives(ServerPlayer player) {
        progressService.checkCollectObjectives(player);
    }
    // endregion
    
    // region 奖励发放
    public boolean turnInQuestByScroll(ServerPlayer player, ItemStack scroll) {
        return rewardService.turnInQuestByScroll(player, scroll).isSuccess();
    }
    
    public boolean turnInQuest(ServerPlayer player, ResourceLocation questId) {
        return rewardService.turnInQuest(player, questId).isSuccess();
    }
    
    public void addReputationXp(ServerPlayer player, ResourceLocation factionId, int amount) {
        rewardService.addReputationXp(player, factionId, amount);
    }
    
    public void syncReputationToClient(ServerPlayer player) {
        rewardService.syncReputationToClient(player);
    }
    // endregion
    
    // region 数据同步
    public void syncAllQuestsToClient(ServerPlayer player) {
        syncService.syncAllQuestsToClient(player);
    }
    
    public void syncAllDefinitionsToClient(ServerPlayer player) {
        syncService.syncAllDefinitionsToClient(player);
    }
    
    public void syncQuestToClient(ServerPlayer player, ResourceLocation questId) {
        syncService.syncQuestToClient(player, questId);
    }
    
    public void validateInventoryScrolls(ServerPlayer player) {
        syncService.validateInventoryScrolls(player);
    }
    
    public void retrieveLostScrolls(ServerPlayer player) {
        syncService.retrieveLostScrolls(player);
    }
    // endregion
    
    // region 查询方法
    public List<QuestDefinition> getAvailableQuests(ServerPlayer player) {
        return syncService.getAvailableQuests(player);
    }
    
    public Optional<QuestInstance> getQuestInstance(ServerPlayer player, ResourceLocation questId) {
        return syncService.getQuestInstance(player, questId);
    }
    
    public Collection<QuestInstance> getAllActiveQuests(ServerPlayer player) {
        return syncService.getAllActiveQuests(player);
    }
    
    public Optional<QuestInstance> getQuestByScroll(ServerPlayer player, ItemStack scroll) {
        return syncService.getQuestByScroll(player, scroll);
    }
    // endregion
}
