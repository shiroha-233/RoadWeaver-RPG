package net.shiroha233.roadweaverpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家委托数据
 * 
 * 线程安全：
 * - 使用 ConcurrentHashMap 保证并发访问安全
 * - 使用 AtomicInteger/AtomicLong 保证原子操作
 * - 关键方法使用 synchronized 保证原子性
 */
public class PlayerQuestData {
    
    private final UUID playerId;
    
    // 使用线程安全的集合
    private final Map<ResourceLocation, QuestInstance> activeQuests = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> completedQuests = ConcurrentHashMap.newKeySet();
    private final Map<ResourceLocation, Integer> completionCounts = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Integer> reputationXp = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Integer> reputationLevels = new ConcurrentHashMap<>();
    
    // 使用原子类型
    private final AtomicInteger totalQuestsCompleted = new AtomicInteger(0);
    private final AtomicInteger totalQuestsFailed = new AtomicInteger(0);
    
    // 冒险等级系统
    private final AtomicInteger adventureExp = new AtomicInteger(0);
    private final AtomicInteger adventureLevel = new AtomicInteger(0);
    
    // 玩家等级系统
    private final AtomicInteger playerExp = new AtomicInteger(0);
    private final AtomicInteger playerLevel = new AtomicInteger(0);
    
    // 数据版本号（用于同步检测）
    private final AtomicLong version = new AtomicLong(0);
    
    // 钱包金币数量
    private final AtomicLong walletCoins = new AtomicLong(0);
    
    // 每日委托相关（使用 volatile 保证可见性）
    private volatile List<ResourceLocation> dailyQuests = new ArrayList<>();
    private volatile String lastDailyRefreshDate = "";
    
    // 领取记录追踪（用于周期限制）- key: questId, value: 领取时间戳列表
    private final Map<ResourceLocation, List<Long>> acceptanceHistory = new ConcurrentHashMap<>();
    
    public PlayerQuestData(UUID playerId) {
        this.playerId = playerId;
    }
    
    // region Getters
    public UUID getPlayerId() { return playerId; }
    
    /**
     * 获取数据版本号（用于同步检测）
     */
    public long getVersion() { return version.get(); }
    
    /**
     * 增加版本号
     */
    public void incrementVersion() { version.incrementAndGet(); }
    
    public Collection<QuestInstance> getActiveQuests() {
        return Collections.unmodifiableCollection(activeQuests.values());
    }
    
    public QuestInstance getActiveQuest(ResourceLocation questId) {
        return activeQuests.get(questId);
    }
    
    public boolean hasActiveQuest(ResourceLocation questId) {
        return activeQuests.containsKey(questId);
    }
    
    public Set<ResourceLocation> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }
    
    public boolean hasCompletedQuest(ResourceLocation questId) {
        return completedQuests.contains(questId);
    }
    
    public int getCompletionCount(ResourceLocation questId) {
        return completionCounts.getOrDefault(questId, 0);
    }
    
    public Set<ResourceLocation> getActiveQuestIds() {
        return Collections.unmodifiableSet(activeQuests.keySet());
    }
    
    public int getReputationXp(ResourceLocation factionId) {
        return reputationXp.getOrDefault(factionId, 0);
    }
    
    public int getReputationLevel(ResourceLocation factionId) {
        return reputationLevels.getOrDefault(factionId, 0);
    }
    
    public Map<ResourceLocation, Integer> getAllReputationXp() {
        return Collections.unmodifiableMap(reputationXp);
    }
    
    public Map<ResourceLocation, Integer> getAllReputationLevels() {
        return Collections.unmodifiableMap(reputationLevels);
    }
    
    public int getTotalQuestsCompleted() { return totalQuestsCompleted.get(); }
    public int getTotalQuestsFailed() { return totalQuestsFailed.get(); }
    
    // 冒险等级相关
    public int getAdventureExp() { return adventureExp.get(); }
    public int getAdventureLevel() { return adventureLevel.get(); }
    
    // 玩家等级相关
    public int getPlayerExp() { return playerExp.get(); }
    public int getPlayerLevel() { return playerLevel.get(); }
    
    // 钱包金币相关
    public long getWalletCoins() { return walletCoins.get(); }
    
    // 每日委托相关
    public List<ResourceLocation> getDailyQuests() {
        return Collections.unmodifiableList(dailyQuests);
    }
    
    public String getLastDailyRefreshDate() {
        return lastDailyRefreshDate;
    }
    
    public synchronized void setDailyQuests(List<ResourceLocation> quests) {
        this.dailyQuests = new ArrayList<>(quests);
        incrementVersion();
    }
    
    public synchronized void setLastDailyRefreshDate(String date) {
        this.lastDailyRefreshDate = date;
        incrementVersion();
    }
    // endregion
    
    // region 领取记录操作（线程安全）
    
    /**
     * 记录委托领取时间
     */
    public synchronized void recordAcceptance(ResourceLocation questId) {
        acceptanceHistory.computeIfAbsent(questId, k -> new ArrayList<>())
                .add(System.currentTimeMillis());
        incrementVersion();
    }
    
    /**
     * 获取指定周期内的领取次数
     * @param questId 委托ID
     * @param periodSeconds 周期（秒）
     * @return 周期内领取次数
     */
    public int getAcceptCountInPeriod(ResourceLocation questId, int periodSeconds) {
        List<Long> history = acceptanceHistory.get(questId);
        if (history == null || history.isEmpty()) return 0;
        
        long cutoffTime = System.currentTimeMillis() - (periodSeconds * 1000L);
        return (int) history.stream().filter(t -> t >= cutoffTime).count();
    }
    
    /**
     * 检查是否达到周期内领取上限
     */
    public boolean hasReachedAcceptLimit(ResourceLocation questId, int maxPerPeriod, int periodSeconds) {
        if (maxPerPeriod <= 0 || periodSeconds <= 0) return false;
        return getAcceptCountInPeriod(questId, periodSeconds) >= maxPerPeriod;
    }
    
    /**
     * 获取下次可领取的剩余时间（秒）
     * @return 剩余秒数，0表示可立即领取
     */
    public int getNextAcceptCooldown(ResourceLocation questId, int maxPerPeriod, int periodSeconds) {
        if (maxPerPeriod <= 0 || periodSeconds <= 0) return 0;
        
        List<Long> history = acceptanceHistory.get(questId);
        if (history == null || history.isEmpty()) return 0;
        
        long cutoffTime = System.currentTimeMillis() - (periodSeconds * 1000L);
        List<Long> recentAccepts = history.stream()
                .filter(t -> t >= cutoffTime)
                .sorted()
                .toList();
        
        if (recentAccepts.size() < maxPerPeriod) return 0;
        
        // 最早的一次领取过期后即可再次领取
        long earliestInPeriod = recentAccepts.get(0);
        long nextAvailable = earliestInPeriod + (periodSeconds * 1000L);
        long remaining = nextAvailable - System.currentTimeMillis();
        
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }
    
    /**
     * 清理过期的领取记录（防止内存泄漏）
     */
    public synchronized void cleanupExpiredAcceptanceHistory(int maxPeriodSeconds) {
        long cutoffTime = System.currentTimeMillis() - (maxPeriodSeconds * 1000L * 2); // 保留2倍周期
        
        acceptanceHistory.forEach((questId, history) -> {
            history.removeIf(t -> t < cutoffTime);
        });
        
        // 移除空列表
        acceptanceHistory.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
    // endregion
    
    // region 声望数据操作（线程安全）
    public void addReputationXp(ResourceLocation factionId, int amount) {
        reputationXp.merge(factionId, amount, Integer::sum);
        incrementVersion();
    }

    public void setReputationLevel(ResourceLocation factionId, int level) {
        reputationLevels.put(factionId, level);
        incrementVersion();
    }
    // endregion
    
    // region 冒险等级操作（线程安全）
    public void addAdventureExp(int amount) {
        adventureExp.addAndGet(amount);
        incrementVersion();
    }
    
    public void setAdventureLevel(int level) {
        adventureLevel.set(level);
        incrementVersion();
    }
    // endregion
    
    // region 玩家等级操作（线程安全）
    public void addPlayerExp(int amount) {
        playerExp.addAndGet(amount);
        incrementVersion();
    }
    
    public void setPlayerLevel(int level) {
        playerLevel.set(level);
        incrementVersion();
    }
    // endregion
    
    // region 钱包金币操作（线程安全）
    
    /**
     * 添加金币到钱包
     * @return 添加后的总金币数
     */
    public long addWalletCoins(long amount) {
        long result = walletCoins.addAndGet(amount);
        incrementVersion();
        return result;
    }
    
    /**
     * 从钱包扣除金币
     * @return 是否扣除成功
     */
    public synchronized boolean removeWalletCoins(long amount) {
        if (walletCoins.get() < amount) return false;
        walletCoins.addAndGet(-amount);
        incrementVersion();
        return true;
    }
    
    /**
     * 设置钱包金币数量
     */
    public void setWalletCoins(long amount) {
        walletCoins.set(Math.max(0, amount));
        incrementVersion();
    }
    
    // region 委托操作（线程安全）
    public synchronized void addActiveQuest(QuestInstance instance) {
        activeQuests.put(instance.getQuestId(), instance);
        incrementVersion();
    }
    
    public synchronized QuestInstance removeActiveQuest(ResourceLocation questId) {
        QuestInstance removed = activeQuests.remove(questId);
        if (removed != null) {
            incrementVersion();
        }
        return removed;
    }
    
    public synchronized void markQuestCompleted(ResourceLocation questId) {
        completedQuests.add(questId);
        completionCounts.merge(questId, 1, Integer::sum);
        totalQuestsCompleted.incrementAndGet();
        incrementVersion();
    }
    
    public void markQuestFailed(ResourceLocation questId) {
        totalQuestsFailed.incrementAndGet();
        incrementVersion();
    }
    
    public void setCooldown(ResourceLocation questId, int seconds) {
        cooldowns.put(questId, System.currentTimeMillis() + (seconds * 1000L));
        incrementVersion();
    }
    
    /**
     * 检查冷却（原子操作）
     */
    public synchronized boolean isOnCooldown(ResourceLocation questId) {
        Long endTime = cooldowns.get(questId);
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(questId);
            return false;
        }
        return true;
    }
    
    public int getCooldownRemaining(ResourceLocation questId) {
        Long endTime = cooldowns.get(questId);
        if (endTime == null) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }
    // endregion
    
    // region NBT序列化
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        
        ListTag activeList = new ListTag();
        for (QuestInstance instance : activeQuests.values()) {
            activeList.add(instance.toNbt());
        }
        tag.put("activeQuests", activeList);
        
        ListTag completedList = new ListTag();
        for (ResourceLocation id : completedQuests) {
            completedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("completedQuests", completedList);
        
        CompoundTag countsTag = new CompoundTag();
        completionCounts.forEach((id, count) -> countsTag.putInt(id.toString(), count));
        tag.put("completionCounts", countsTag);
        
        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach((id, time) -> cooldownTag.putLong(id.toString(), time));
        tag.put("cooldowns", cooldownTag);
        
        CompoundTag xpTag = new CompoundTag();
        reputationXp.forEach((id, val) -> xpTag.putInt(id.toString(), val));
        tag.put("reputationXp", xpTag);
        
        CompoundTag levelTag = new CompoundTag();
        reputationLevels.forEach((id, val) -> levelTag.putInt(id.toString(), val));
        tag.put("reputationLevels", levelTag);
        
        tag.putInt("totalCompleted", totalQuestsCompleted.get());
        tag.putInt("totalFailed", totalQuestsFailed.get());
        
        // 冒险等级数据
        tag.putInt("adventureExp", adventureExp.get());
        tag.putInt("adventureLevel", adventureLevel.get());
        
        // 玩家等级数据
        tag.putInt("playerExp", playerExp.get());
        tag.putInt("playerLevel", playerLevel.get());
        
        // 钱包金币数据
        tag.putLong("walletCoins", walletCoins.get());
        
        // 每日委托数据
        ListTag dailyList = new ListTag();
        for (ResourceLocation id : dailyQuests) {
            dailyList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("dailyQuests", dailyList);
        tag.putString("lastDailyRefresh", lastDailyRefreshDate);
        
        // 领取记录
        CompoundTag historyTag = new CompoundTag();
        acceptanceHistory.forEach((questId, timestamps) -> {
            long[] arr = timestamps.stream().mapToLong(Long::longValue).toArray();
            historyTag.putLongArray(questId.toString(), arr);
        });
        tag.put("acceptanceHistory", historyTag);
        
        return tag;
    }
    
    /**
     * 从 NBT 反序列化（带异常处理）
     */
    public static PlayerQuestData fromNbt(CompoundTag tag) {
        UUID playerId = tag.getUUID("playerId");
        PlayerQuestData data = new PlayerQuestData(playerId);
        
        try {
            // 活跃委托
            ListTag activeList = tag.getList("activeQuests", Tag.TAG_COMPOUND);
            for (int i = 0; i < activeList.size(); i++) {
                try {
                    QuestInstance instance = QuestInstance.fromNbt(activeList.getCompound(i));
                    data.activeQuests.put(instance.getQuestId(), instance);
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.warn("Failed to load quest instance at index {}: {}", i, e.getMessage());
                }
            }
            
            // 已完成委托
            ListTag completedList = tag.getList("completedQuests", Tag.TAG_STRING);
            for (int i = 0; i < completedList.size(); i++) {
                try {
                    data.completedQuests.add(new ResourceLocation(completedList.getString(i)));
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.warn("Failed to load completed quest at index {}: {}", i, e.getMessage());
                }
            }
            
            // 完成次数
            CompoundTag countsTag = tag.getCompound("completionCounts");
            for (String key : countsTag.getAllKeys()) {
                try {
                    data.completionCounts.put(new ResourceLocation(key), countsTag.getInt(key));
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.warn("Failed to load completion count for {}: {}", key, e.getMessage());
                }
            }
            
            // 冷却时间
            CompoundTag cooldownTag = tag.getCompound("cooldowns");
            for (String key : cooldownTag.getAllKeys()) {
                try {
                    data.cooldowns.put(new ResourceLocation(key), cooldownTag.getLong(key));
                } catch (Exception e) {
                    RoadWeaverRPG.LOGGER.warn("Failed to load cooldown for {}: {}", key, e.getMessage());
                }
            }
            
            // 声望经验
            if (tag.contains("reputationXp")) {
                CompoundTag xpTag = tag.getCompound("reputationXp");
                for (String key : xpTag.getAllKeys()) {
                    try {
                        data.reputationXp.put(new ResourceLocation(key), xpTag.getInt(key));
                    } catch (Exception e) {
                        RoadWeaverRPG.LOGGER.warn("Failed to load reputation xp for {}: {}", key, e.getMessage());
                    }
                }
            }
            
            // 声望等级
            if (tag.contains("reputationLevels")) {
                CompoundTag levelTag = tag.getCompound("reputationLevels");
                for (String key : levelTag.getAllKeys()) {
                    try {
                        data.reputationLevels.put(new ResourceLocation(key), levelTag.getInt(key));
                    } catch (Exception e) {
                        RoadWeaverRPG.LOGGER.warn("Failed to load reputation level for {}: {}", key, e.getMessage());
                    }
                }
            }
            
            // 统计数据
            data.totalQuestsCompleted.set(tag.getInt("totalCompleted"));
            data.totalQuestsFailed.set(tag.getInt("totalFailed"));
            
            // 冒险等级数据
            if (tag.contains("adventureExp")) {
                data.adventureExp.set(tag.getInt("adventureExp"));
            }
            if (tag.contains("adventureLevel")) {
                data.adventureLevel.set(tag.getInt("adventureLevel"));
            }
            
            // 玩家等级数据
            if (tag.contains("playerExp")) {
                data.playerExp.set(tag.getInt("playerExp"));
            }
            if (tag.contains("playerLevel")) {
                data.playerLevel.set(tag.getInt("playerLevel"));
            }
            
            // 钱包金币数据
            if (tag.contains("walletCoins")) {
                data.walletCoins.set(tag.getLong("walletCoins"));
            }
            
            // 每日委托数据
            if (tag.contains("dailyQuests")) {
                ListTag dailyList = tag.getList("dailyQuests", Tag.TAG_STRING);
                List<ResourceLocation> dailyQuests = new ArrayList<>();
                for (int i = 0; i < dailyList.size(); i++) {
                    try {
                        dailyQuests.add(new ResourceLocation(dailyList.getString(i)));
                    } catch (Exception e) {
                        RoadWeaverRPG.LOGGER.warn("Failed to load daily quest at index {}: {}", i, e.getMessage());
                    }
                }
                data.dailyQuests = dailyQuests;
            }
            if (tag.contains("lastDailyRefresh")) {
                data.lastDailyRefreshDate = tag.getString("lastDailyRefresh");
            }
            
            // 领取记录
            if (tag.contains("acceptanceHistory")) {
                CompoundTag historyTag = tag.getCompound("acceptanceHistory");
                for (String key : historyTag.getAllKeys()) {
                    try {
                        long[] arr = historyTag.getLongArray(key);
                        List<Long> timestamps = new ArrayList<>();
                        for (long t : arr) {
                            timestamps.add(t);
                        }
                        data.acceptanceHistory.put(new ResourceLocation(key), timestamps);
                    } catch (Exception e) {
                        RoadWeaverRPG.LOGGER.warn("Failed to load acceptance history for {}: {}", key, e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Critical error loading player quest data for {}: {}", playerId, e.getMessage());
        }
        
        return data;
    }
    // endregion
}
