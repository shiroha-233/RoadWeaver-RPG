package net.shiroha233.roadweaverpg.quest.sync;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.config.QuestSystemConfig;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.item.QuestScrollItem;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.ObjectiveProgress;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 委托同步验证器
 * 
 * 改进点：
 * - 扩展验证规则（目标进度、时间戳、难度系数等）
 * - 动态验证间隔（发现问题后缩短间隔）
 * - 验证日志统计
 * - 验证规则优先级
 */
public class QuestSyncValidator {
    
    private static QuestSyncValidator instance;
    
    // 每个玩家的上次验证时间
    private final Map<UUID, Long> lastValidationTime = new ConcurrentHashMap<>();
    
    // 每个玩家的验证间隔（动态调整）
    private final Map<UUID, Long> validationIntervals = new ConcurrentHashMap<>();
    
    // 验证统计
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong totalIssuesFound = new AtomicLong(0);
    private final AtomicLong totalIssuesFixed = new AtomicLong(0);
    
    // 默认验证间隔
    private static final long DEFAULT_INTERVAL = QuestSystemConfig.SYNC_VALIDATION_INTERVAL;
    // 发现问题后的短间隔
    private static final long SHORT_INTERVAL = 60_000L; // 1分钟
    // 难度系数有效范围
    private static final float MIN_DIFFICULTY = 0.5f;
    private static final float MAX_DIFFICULTY = 5.0f;
    
    private QuestSyncValidator() {}
    
    public static QuestSyncValidator getInstance() {
        if (instance == null) {
            instance = new QuestSyncValidator();
        }
        return instance;
    }
    
    /**
     * 检查是否需要验证（动态间隔）
     */
    public boolean shouldValidate(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
        Long lastTime = lastValidationTime.get(playerId);
        long interval = validationIntervals.getOrDefault(playerId, DEFAULT_INTERVAL);
        
        if (lastTime == null || now - lastTime >= interval) {
            lastValidationTime.put(playerId, now);
            return true;
        }
        return false;
    }
    
    /**
     * 强制验证（玩家登录时调用）
     */
    public int forceValidate(ServerPlayer player, PlayerQuestData playerData) {
        lastValidationTime.put(player.getUUID(), System.currentTimeMillis());
        return validateAndFix(player, playerData);
    }
    
    /**
     * 验证并修复玩家的委托数据
     * @return 修复的问题数量
     */
    public int validateAndFix(ServerPlayer player, PlayerQuestData playerData) {
        totalValidations.incrementAndGet();
        int fixCount = 0;
        
        // 按优先级执行验证规则
        // 优先级1：数据完整性
        fixCount += validateDataIntegrity(player, playerData);
        
        // 优先级2：数据一致性
        fixCount += validateScrollStates(player, playerData);
        fixCount += validateMissingScrolls(player, playerData);
        fixCount += validateOrphanedScrolls(player, playerData);
        
        // 优先级3：数据合法性
        fixCount += validateObjectiveProgress(player, playerData);
        fixCount += validateTimestamps(player, playerData);
        fixCount += validateDifficultyMultipliers(player, playerData);
        fixCount += validateExpiredQuests(player, playerData);
        
        // 优先级4：数据优化
        fixCount += cleanupStaleData(player, playerData);
        
        // 更新统计和动态间隔
        if (fixCount > 0) {
            totalIssuesFound.addAndGet(fixCount);
            totalIssuesFixed.addAndGet(fixCount);
            // 发现问题，缩短验证间隔
            validationIntervals.put(player.getUUID(), SHORT_INTERVAL);
            RoadWeaverRPG.LOGGER.info("Fixed {} quest sync issues for player {}", 
                    fixCount, player.getName().getString());
        } else {
            // 无问题，恢复默认间隔
            validationIntervals.put(player.getUUID(), DEFAULT_INTERVAL);
        }
        
        return fixCount;
    }
    
    /**
     * 验证委托书状态是否与实例一致
     */
    private int validateScrollStates(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof QuestScrollItem)) continue;
            if (!QuestScrollItem.hasQuest(stack)) continue;
            
            Optional<ResourceLocation> questIdOpt = QuestScrollItem.getQuestId(stack);
            if (questIdOpt.isEmpty()) continue;
            
            ResourceLocation questId = questIdOpt.get();
            QuestInstance instance = playerData.getActiveQuest(questId);
            
            if (instance != null) {
                QuestState scrollState = QuestScrollItem.getQuestState(stack);
                if (scrollState != instance.getState()) {
                    QuestScrollItem.updateState(stack, instance.getState());
                    fixCount++;
                    RoadWeaverRPG.LOGGER.debug("Fixed scroll state for quest {}: {} -> {}", 
                            questId, scrollState, instance.getState());
                }
            } else if (playerData.hasCompletedQuest(questId)) {
                // 委托已完成但委托书还在
                QuestScrollItem.updateState(stack, QuestState.TURNED_IN);
                fixCount++;
            }
        }
        
        return fixCount;
    }
    
    /**
     * 验证是否有活跃委托缺少委托书
     */
    private int validateMissingScrolls(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        Set<ResourceLocation> scrollQuests = getScrollQuestIds(player);
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            if (instance.getState() == QuestState.TURNED_IN) continue;
            
            if (!scrollQuests.contains(instance.getQuestId())) {
                // 缺少委托书，但不自动补发（避免刷物品）
                // 只记录日志，玩家需要通过NPC对话找回
                RoadWeaverRPG.LOGGER.warn("Player {} is missing scroll for quest {}", 
                        player.getName().getString(), instance.getQuestId());
            }
        }
        
        return fixCount;
    }
    
    /**
     * 验证过期的委托
     */
    private int validateExpiredQuests(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        List<QuestInstance> toExpire = new ArrayList<>();
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            if (instance.getState() == QuestState.IN_PROGRESS && instance.isExpired()) {
                toExpire.add(instance);
            }
        }
        
        for (QuestInstance instance : toExpire) {
            instance.setState(QuestState.EXPIRED);
            playerData.markQuestFailed(instance.getQuestId());
            updateScrollState(player, instance);
            fixCount++;
            RoadWeaverRPG.LOGGER.info("Expired quest {} for player {}", 
                    instance.getQuestId(), player.getName().getString());
        }
        
        return fixCount;
    }
    
    // ==================== 新增验证规则 ====================
    
    /**
     * 验证数据完整性（优先级1）
     * 检查必要字段是否存在
     */
    private int validateDataIntegrity(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        List<ResourceLocation> toRemove = new ArrayList<>();
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            // 检查委托定义是否存在
            QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(instance.getQuestId());
            if (def == null) {
                toRemove.add(instance.getQuestId());
                RoadWeaverRPG.LOGGER.warn("Removing quest with missing definition: {} for player {}",
                        instance.getQuestId(), player.getName().getString());
                continue;
            }
            
            // 检查目标进度是否完整
            for (var objective : def.getObjectives()) {
                if (instance.getObjectiveProgress(objective.getId()) == null) {
                    RoadWeaverRPG.LOGGER.warn("Quest {} missing objective progress for {}",
                            instance.getQuestId(), objective.getId());
                    // 这种情况比较严重，标记为失败
                    instance.setState(QuestState.FAILED);
                    playerData.markQuestFailed(instance.getQuestId());
                    fixCount++;
                    break;
                }
            }
        }
        
        // 移除无效委托
        for (ResourceLocation questId : toRemove) {
            playerData.removeActiveQuest(questId);
            fixCount++;
        }
        
        return fixCount;
    }
    
    /**
     * 验证目标进度合法性（优先级3）
     * 确保进度不超过需求量
     */
    private int validateObjectiveProgress(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            QuestDefinition def = QuestDefinitionLoader.getInstance().getDefinition(instance.getQuestId());
            if (def == null) continue;
            
            for (var objective : def.getObjectives()) {
                ObjectiveProgress progress = instance.getObjectiveProgress(objective.getId());
                if (progress == null) continue;
                
                int current = progress.getCurrentProgress();
                int required = objective.getRequiredAmount();
                
                // 进度不能为负数
                if (current < 0) {
                    progress.setProgress(0);
                    fixCount++;
                    RoadWeaverRPG.LOGGER.debug("Fixed negative progress for quest {} objective {}",
                            instance.getQuestId(), objective.getId());
                }
                
                // 进度不能超过需求量的2倍（允许一定的溢出）
                if (current > required * 2) {
                    progress.setProgress(required);
                    fixCount++;
                    RoadWeaverRPG.LOGGER.debug("Fixed excessive progress for quest {} objective {}",
                            instance.getQuestId(), objective.getId());
                }
            }
        }
        
        return fixCount;
    }
    
    /**
     * 验证时间戳合理性（优先级3）
     */
    private int validateTimestamps(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        long now = System.currentTimeMillis();
        // 允许1分钟的时间偏差
        long maxFutureTime = now + 60_000L;
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            // 接取时间不能在未来
            if (instance.getAcceptedTime() > maxFutureTime) {
                RoadWeaverRPG.LOGGER.warn("Quest {} has future accepted time, marking as failed",
                        instance.getQuestId());
                instance.setState(QuestState.FAILED);
                playerData.markQuestFailed(instance.getQuestId());
                fixCount++;
            }
            
            // 过期时间检查（如果有）
            long expTime = instance.getExpirationTime();
            if (expTime > 0 && expTime < instance.getAcceptedTime()) {
                RoadWeaverRPG.LOGGER.warn("Quest {} has invalid expiration time",
                        instance.getQuestId());
                instance.setState(QuestState.FAILED);
                playerData.markQuestFailed(instance.getQuestId());
                fixCount++;
            }
        }
        
        return fixCount;
    }
    
    /**
     * 验证难度系数范围（优先级3）
     */
    private int validateDifficultyMultipliers(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        
        for (QuestInstance instance : playerData.getActiveQuests()) {
            float difficulty = instance.getDifficultyMultiplier();
            
            if (difficulty < MIN_DIFFICULTY) {
                instance.setDifficultyMultiplier(MIN_DIFFICULTY);
                fixCount++;
                RoadWeaverRPG.LOGGER.debug("Fixed low difficulty for quest {}: {} -> {}",
                        instance.getQuestId(), difficulty, MIN_DIFFICULTY);
            } else if (difficulty > MAX_DIFFICULTY) {
                instance.setDifficultyMultiplier(MAX_DIFFICULTY);
                fixCount++;
                RoadWeaverRPG.LOGGER.debug("Fixed high difficulty for quest {}: {} -> {}",
                        instance.getQuestId(), difficulty, MAX_DIFFICULTY);
            }
        }
        
        return fixCount;
    }
    
    /**
     * 清理过期数据（优先级4）
     */
    private int cleanupStaleData(ServerPlayer player, PlayerQuestData playerData) {
        // 目前只做日志记录，不实际清理
        // 可以在这里添加清理过期冷却时间等逻辑
        return 0;
    }
    
    /**
     * 验证孤立的委托书（没有对应的活跃委托）
     */
    private int validateOrphanedScrolls(ServerPlayer player, PlayerQuestData playerData) {
        int fixCount = 0;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof QuestScrollItem)) continue;
            if (!QuestScrollItem.hasQuest(stack)) continue;
            
            Optional<ResourceLocation> questIdOpt = QuestScrollItem.getQuestId(stack);
            if (questIdOpt.isEmpty()) continue;
            
            ResourceLocation questId = questIdOpt.get();
            QuestInstance instance = playerData.getActiveQuest(questId);
            
            if (instance == null && !playerData.hasCompletedQuest(questId)) {
                // 孤立的委托书，移除
                player.getInventory().setItem(i, ItemStack.EMPTY);
                fixCount++;
                RoadWeaverRPG.LOGGER.debug("Removed orphaned scroll for quest {} from player {}", 
                        questId, player.getName().getString());
            }
        }
        
        return fixCount;
    }
    
    /**
     * 获取玩家背包中所有委托书的委托ID
     */
    private Set<ResourceLocation> getScrollQuestIds(ServerPlayer player) {
        Set<ResourceLocation> questIds = new HashSet<>();
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem && QuestScrollItem.hasQuest(stack)) {
                QuestScrollItem.getQuestId(stack).ifPresent(questIds::add);
            }
        }
        
        return questIds;
    }
    
    /**
     * 更新委托书状态
     */
    private void updateScrollState(ServerPlayer player, QuestInstance instance) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof QuestScrollItem) {
                Optional<UUID> scrollInstanceId = QuestScrollItem.getInstanceId(stack);
                if (scrollInstanceId.isPresent() && scrollInstanceId.get().equals(instance.getInstanceId())) {
                    QuestScrollItem.updateState(stack, instance.getState());
                    break;
                }
            }
        }
    }
    
    /**
     * 清除玩家的所有数据（登出时调用）
     */
    public void clearPlayerData(UUID playerId) {
        lastValidationTime.remove(playerId);
        validationIntervals.remove(playerId);
    }
    
    /**
     * 获取验证统计信息
     */
    public ValidationStats getStats() {
        return new ValidationStats(
                totalValidations.get(),
                totalIssuesFound.get(),
                totalIssuesFixed.get()
        );
    }
    
    public record ValidationStats(long validations, long issuesFound, long issuesFixed) {}
}
