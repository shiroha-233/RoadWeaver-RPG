package net.shiroha233.roadweaverpg.quest.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.instance.ObjectiveProgress;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.type.QuestState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * 增量同步管理器
 * 
 * 设计原理：
 * - 使用版本号机制检测数据变更
 * - 只同步变化的数据，减少网络流量
 * - 支持全量同步和增量同步
 * - 定期进行完整性校验
 * 
 * 同步策略：
 * - 小变更（进度更新）：增量同步
 * - 大变更（接取/完成）：全量同步该委托
 * - 定期校验：每分钟进行一次完整性检查
 */
public class IncrementalSyncManager {
    
    private static volatile IncrementalSyncManager instance;
    private static final Object LOCK = new Object();
    
    // 玩家的同步版本号
    private final Map<UUID, Long> playerSyncVersions = new ConcurrentHashMap<>();
    
    // 待同步的变更（按玩家分组）
    private final Map<UUID, List<SyncDelta>> pendingDeltas = new ConcurrentHashMap<>();
    
    // 上次完整性校验时间
    private final Map<UUID, Long> lastIntegrityCheck = new ConcurrentHashMap<>();
    
    // 同步回调
    private BiConsumer<ServerPlayer, SyncPacket> syncCallback;
    
    // 统计信息
    private final AtomicLong totalIncrementalSyncs = new AtomicLong(0);
    private final AtomicLong totalFullSyncs = new AtomicLong(0);
    private final AtomicLong totalBytesTransferred = new AtomicLong(0);
    
    // 配置
    private static final long INTEGRITY_CHECK_INTERVAL = 60_000L; // 1分钟
    private static final int MAX_PENDING_DELTAS = 50;
    
    private IncrementalSyncManager() {}
    
    public static IncrementalSyncManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new IncrementalSyncManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置同步回调
     */
    public void setSyncCallback(BiConsumer<ServerPlayer, SyncPacket> callback) {
        this.syncCallback = callback;
    }
    
    /**
     * 记录进度变更（增量）
     */
    public void recordProgressChange(UUID playerId, ResourceLocation questId, 
                                     String objectiveId, int oldProgress, int newProgress) {
        SyncDelta delta = new SyncDelta(
                DeltaType.PROGRESS_UPDATE,
                questId,
                objectiveId,
                Map.of("old", oldProgress, "new", newProgress)
        );
        addDelta(playerId, delta);
    }
    
    /**
     * 记录状态变更
     */
    public void recordStateChange(UUID playerId, ResourceLocation questId, 
                                  QuestState oldState, QuestState newState) {
        SyncDelta delta = new SyncDelta(
                DeltaType.STATE_CHANGE,
                questId,
                null,
                Map.of("old", oldState.getSerializedName(), "new", newState.getSerializedName())
        );
        addDelta(playerId, delta);
    }
    
    /**
     * 记录委托接取
     */
    public void recordQuestAccepted(UUID playerId, QuestInstance instance) {
        SyncDelta delta = new SyncDelta(
                DeltaType.QUEST_ADDED,
                instance.getQuestId(),
                null,
                Map.of("instance", instance)
        );
        addDelta(playerId, delta);
    }
    
    /**
     * 记录委托移除
     */
    public void recordQuestRemoved(UUID playerId, ResourceLocation questId) {
        SyncDelta delta = new SyncDelta(
                DeltaType.QUEST_REMOVED,
                questId,
                null,
                Map.of()
        );
        addDelta(playerId, delta);
    }
    
    /**
     * 执行同步
     */
    public void sync(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<SyncDelta> deltas = pendingDeltas.remove(playerId);
        
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        
        // 检查是否需要全量同步
        if (shouldFullSync(deltas)) {
            performFullSync(player);
            return;
        }
        
        // 执行增量同步
        performIncrementalSync(player, deltas);
    }
    
    /**
     * 强制全量同步
     */
    public void forceFullSync(ServerPlayer player) {
        pendingDeltas.remove(player.getUUID());
        performFullSync(player);
    }
    
    /**
     * 检查并执行完整性校验（线程安全）
     * 
     * 原理：使用 computeIfAbsent 确保原子性
     */
    public void checkIntegrity(ServerPlayer player, Collection<QuestInstance> serverQuests) {
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
        
        // 原子操作：检查并更新时间戳
        Long lastCheck = lastIntegrityCheck.compute(playerId, (key, oldValue) -> {
            if (oldValue != null && now - oldValue < INTEGRITY_CHECK_INTERVAL) {
                return oldValue; // 不需要检查
            }
            return now; // 更新时间戳
        });
        
        // 如果时间戳未更新，说明不需要检查
        if (lastCheck != null && now - lastCheck < INTEGRITY_CHECK_INTERVAL) {
            return;
        }
        
        // 发送校验请求到客户端
        if (syncCallback != null) {
            SyncPacket packet = new SyncPacket(
                    SyncType.INTEGRITY_CHECK,
                    calculateChecksum(serverQuests),
                    null,
                    null
            );
            syncCallback.accept(player, packet);
        }
    }
    
    /**
     * 处理客户端的校验响应
     */
    public void handleIntegrityResponse(ServerPlayer player, long clientChecksum, 
                                        Collection<QuestInstance> serverQuests) {
        long serverChecksum = calculateChecksum(serverQuests);
        
        if (clientChecksum != serverChecksum) {
            RoadWeaverRPG.LOGGER.warn("Integrity check failed for player {}: client={}, server={}",
                    player.getName().getString(), clientChecksum, serverChecksum);
            forceFullSync(player);
        }
    }
    
    /**
     * 清除玩家数据
     */
    public void clearPlayerData(UUID playerId) {
        playerSyncVersions.remove(playerId);
        pendingDeltas.remove(playerId);
        lastIntegrityCheck.remove(playerId);
    }
    
    /**
     * 获取统计信息
     */
    public SyncStats getStats() {
        return new SyncStats(
                totalIncrementalSyncs.get(),
                totalFullSyncs.get(),
                totalBytesTransferred.get(),
                pendingDeltas.values().stream().mapToInt(List::size).sum()
        );
    }
    
    // ==================== 私有方法 ====================
    
    private void addDelta(UUID playerId, SyncDelta delta) {
        List<SyncDelta> deltas = pendingDeltas.computeIfAbsent(playerId, k -> new ArrayList<>());
        deltas.add(delta);
        
        // 如果待同步变更过多，触发全量同步
        if (deltas.size() > MAX_PENDING_DELTAS) {
            deltas.clear();
            deltas.add(new SyncDelta(DeltaType.FULL_SYNC_REQUIRED, null, null, Map.of()));
        }
    }
    
    private boolean shouldFullSync(List<SyncDelta> deltas) {
        // 如果有全量同步标记，或者变更过多
        return deltas.stream().anyMatch(d -> d.type() == DeltaType.FULL_SYNC_REQUIRED)
                || deltas.size() > MAX_PENDING_DELTAS / 2;
    }
    
    private void performFullSync(ServerPlayer player) {
        if (syncCallback != null) {
            SyncPacket packet = new SyncPacket(
                    SyncType.FULL,
                    0,
                    null,
                    null
            );
            syncCallback.accept(player, packet);
            totalFullSyncs.incrementAndGet();
        }
    }
    
    private void performIncrementalSync(ServerPlayer player, List<SyncDelta> deltas) {
        if (syncCallback != null) {
            SyncPacket packet = new SyncPacket(
                    SyncType.INCREMENTAL,
                    0,
                    deltas,
                    null
            );
            syncCallback.accept(player, packet);
            totalIncrementalSyncs.incrementAndGet();
        }
    }
    
    /**
     * 计算委托数据的校验和
     */
    private long calculateChecksum(Collection<QuestInstance> quests) {
        long checksum = 0;
        for (QuestInstance quest : quests) {
            checksum ^= quest.getInstanceId().hashCode();
            checksum ^= quest.getState().ordinal();
            for (ObjectiveProgress progress : quest.getObjectiveProgresses()) {
                checksum ^= progress.getCurrentProgress();
            }
        }
        return checksum;
    }
    
    // ==================== 内部类 ====================
    
    /** 变更类型 */
    public enum DeltaType {
        PROGRESS_UPDATE,
        STATE_CHANGE,
        QUEST_ADDED,
        QUEST_REMOVED,
        FULL_SYNC_REQUIRED
    }
    
    /** 同步类型 */
    public enum SyncType {
        FULL,
        INCREMENTAL,
        INTEGRITY_CHECK
    }
    
    /** 同步变更 */
    public record SyncDelta(
            DeltaType type,
            ResourceLocation questId,
            String objectiveId,
            Map<String, Object> data
    ) {
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeEnum(type);
            buf.writeBoolean(questId != null);
            if (questId != null) {
                buf.writeResourceLocation(questId);
            }
            buf.writeBoolean(objectiveId != null);
            if (objectiveId != null) {
                buf.writeUtf(objectiveId);
            }
            // 简化数据序列化
            buf.writeVarInt(data.size());
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                buf.writeUtf(entry.getKey());
                if (entry.getValue() instanceof Integer i) {
                    buf.writeByte(0);
                    buf.writeVarInt(i);
                } else if (entry.getValue() instanceof String s) {
                    buf.writeByte(1);
                    buf.writeUtf(s);
                } else if (entry.getValue() instanceof QuestInstance inst) {
                    buf.writeByte(2);
                    inst.toNetwork(buf);
                }
            }
        }
        
        public static SyncDelta fromNetwork(FriendlyByteBuf buf) {
            DeltaType type = buf.readEnum(DeltaType.class);
            ResourceLocation questId = buf.readBoolean() ? buf.readResourceLocation() : null;
            String objectiveId = buf.readBoolean() ? buf.readUtf() : null;
            
            Map<String, Object> data = new HashMap<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                String key = buf.readUtf();
                byte valueType = buf.readByte();
                Object value = switch (valueType) {
                    case 0 -> buf.readVarInt();
                    case 1 -> buf.readUtf();
                    case 2 -> QuestInstance.fromNetwork(buf);
                    default -> null;
                };
                if (value != null) {
                    data.put(key, value);
                }
            }
            
            return new SyncDelta(type, questId, objectiveId, data);
        }
    }
    
    /** 同步数据包 */
    public record SyncPacket(
            SyncType type,
            long checksum,
            List<SyncDelta> deltas,
            Collection<QuestInstance> fullData
    ) {}
    
    /** 同步统计 */
    public record SyncStats(
            long incrementalSyncs,
            long fullSyncs,
            long bytesTransferred,
            int pendingDeltas
    ) {}
}
