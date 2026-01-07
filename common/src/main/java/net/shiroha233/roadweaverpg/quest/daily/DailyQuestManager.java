package net.shiroha233.roadweaverpg.quest.daily;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.service.QuestDefinitionLoader;
import net.shiroha233.roadweaverpg.quest.type.QuestRank;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 每日委托管理器
 * 负责每日委托的随机刷新和管理
 * 
 * 设计原则：
 * - 单一职责：只负责每日委托的选择和刷新
 * - 开闭原则：通过配置扩展每日委托数量
 * - 线程安全：使用双重检查锁定单例模式
 */
public class DailyQuestManager {
    
    private static volatile DailyQuestManager instance;
    private static final Object LOCK = new Object();
    
    // 每个等级每日刷新的委托数量
    private static final Map<QuestRank, Integer> DAILY_QUEST_COUNT = new EnumMap<>(QuestRank.class);
    
    static {
        DAILY_QUEST_COUNT.put(QuestRank.D, net.shiroha233.roadweaverpg.config.QuestSystemConfig.DAILY_QUEST_COUNT_D);
        DAILY_QUEST_COUNT.put(QuestRank.C, net.shiroha233.roadweaverpg.config.QuestSystemConfig.DAILY_QUEST_COUNT_C);
        DAILY_QUEST_COUNT.put(QuestRank.B, net.shiroha233.roadweaverpg.config.QuestSystemConfig.DAILY_QUEST_COUNT_B);
        DAILY_QUEST_COUNT.put(QuestRank.A, net.shiroha233.roadweaverpg.config.QuestSystemConfig.DAILY_QUEST_COUNT_A);
        DAILY_QUEST_COUNT.put(QuestRank.S, net.shiroha233.roadweaverpg.config.QuestSystemConfig.DAILY_QUEST_COUNT_S);
    }
    
    private final QuestDataAccessor dataAccessor;
    
    private DailyQuestManager() {
        this.dataAccessor = QuestDataAccessor.getInstance();
    }
    
    /**
     * 获取单例实例（双重检查锁定）
     */
    public static DailyQuestManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DailyQuestManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取今日日期字符串（用于判断是否需要刷新）
     */
    public static String getTodayDateString() {
        return LocalDate.now(ZoneId.systemDefault()).toString();
    }
    
    /**
     * 检查并刷新玩家的每日委托
     * @return 是否进行了刷新
     */
    public boolean checkAndRefreshDailyQuests(ServerPlayer player) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        String today = getTodayDateString();
        
        if (!today.equals(data.getLastDailyRefreshDate())) {
            refreshDailyQuests(player, data, today);
            return true;
        }
        return false;
    }
    
    /**
     * 刷新每日委托
     */
    private void refreshDailyQuests(ServerPlayer player, PlayerQuestData data, String today) {
        // 使用日期+玩家UUID作为种子，保证同一天同一玩家结果一致
        long seed = today.hashCode() + player.getUUID().hashCode();
        Random random = new Random(seed);
        
        List<ResourceLocation> newDailyQuests = new ArrayList<>();
        QuestDefinitionLoader loader = QuestDefinitionLoader.getInstance();
        
        // 按等级分别选择每日委托
        for (QuestRank rank : QuestRank.values()) {
            int count = DAILY_QUEST_COUNT.getOrDefault(rank, 2);
            List<QuestDefinition> candidates = getDailyQuestCandidates(loader, rank, data);
            
            List<ResourceLocation> selected = selectWeightedRandom(candidates, count, random);
            newDailyQuests.addAll(selected);
        }
        
        data.setDailyQuests(newDailyQuests);
        data.setLastDailyRefreshDate(today);
        dataAccessor.markDirty(player);
        
        RoadWeaverRPG.LOGGER.info("Refreshed daily quests for player {}: {} quests", 
                player.getName().getString(), newDailyQuests.size());
    }
    
    /**
     * 获取指定等级的每日委托候选列表
     */
    private List<QuestDefinition> getDailyQuestCandidates(QuestDefinitionLoader loader, 
                                                          QuestRank rank, PlayerQuestData data) {
        List<QuestDefinition> candidates = new ArrayList<>();
        
        for (QuestDefinition def : loader.getDefinitionsByRank(rank)) {
            // 只选择标记为每日委托的
            if (!def.isDailyQuest()) continue;
            
            // 检查前置条件
            if (!loader.checkPrerequisites(def.getId(), data.getCompletedQuests())) continue;
            
            // 检查是否已完成且不可重复
            if (data.hasCompletedQuest(def.getId()) && !def.isRepeatable()) continue;
            
            // 单次任务完成后不再刷新
            if (!net.shiroha233.roadweaverpg.common.util.ValidationUtils.canQuestBeRefreshed(def, data)) continue;
            
            candidates.add(def);
        }
        
        return candidates;
    }
    
    /**
     * 带权重的随机选择
     */
    private List<ResourceLocation> selectWeightedRandom(List<QuestDefinition> candidates, 
                                                         int count, Random random) {
        if (candidates.isEmpty()) return Collections.emptyList();
        
        List<ResourceLocation> selected = new ArrayList<>();
        List<QuestDefinition> pool = new ArrayList<>(candidates);
        
        while (selected.size() < count && !pool.isEmpty()) {
            // 计算总权重
            int totalWeight = pool.stream().mapToInt(QuestDefinition::getDailyWeight).sum();
            if (totalWeight <= 0) break;
            
            // 随机选择
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            
            for (int i = 0; i < pool.size(); i++) {
                cumulative += pool.get(i).getDailyWeight();
                if (roll < cumulative) {
                    selected.add(pool.get(i).getId());
                    pool.remove(i);
                    break;
                }
            }
        }
        
        return selected;
    }
    
    /**
     * 获取玩家当前的每日委托列表
     */
    public List<QuestDefinition> getDailyQuests(ServerPlayer player) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        QuestDefinitionLoader loader = QuestDefinitionLoader.getInstance();
        
        List<QuestDefinition> result = new ArrayList<>();
        for (ResourceLocation id : data.getDailyQuests()) {
            QuestDefinition def = loader.getDefinition(id);
            if (def != null) {
                result.add(def);
            }
        }
        return result;
    }
    
    /**
     * 检查委托是否在今日每日委托列表中
     */
    public boolean isDailyQuestAvailable(ServerPlayer player, ResourceLocation questId) {
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        return data.getDailyQuests().contains(questId);
    }
    
    /**
     * 获取距离下次刷新的剩余时间（秒）
     */
    public int getTimeUntilRefresh() {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        LocalDate tomorrow = now.plusDays(1);
        long nowMillis = System.currentTimeMillis();
        long tomorrowMillis = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return (int) ((tomorrowMillis - nowMillis) / 1000);
    }
    
    /**
     * 设置每个等级的每日委托数量
     */
    public static void setDailyQuestCount(QuestRank rank, int count) {
        DAILY_QUEST_COUNT.put(rank, count);
    }
    
    /**
     * 获取每个等级的每日委托数量
     */
    public static int getDailyQuestCount(QuestRank rank) {
        return DAILY_QUEST_COUNT.getOrDefault(rank, 2);
    }
}
