package net.shiroha233.roadweaverpg.quest.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective.SpawnConfig;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective.TargetLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 委托怪物刷新管理器
 * 
 * 功能：
 * 1. 管理委托相关的怪物刷新
 * 2. 追踪已刷新的怪物
 * 3. 处理怪物死亡后的重生
 * 4. 清理过期的刷新数据
 * 
 * 设计原则：
 * - 线程安全：使用ConcurrentHashMap
 * - 性能优化：批量处理，避免频繁遍历
 * - 内存管理：定期清理过期数据
 */
public class QuestMobSpawnManager {
    
    private static volatile QuestMobSpawnManager instance;
    private static final Object LOCK = new Object();
    
    // 刷新点数据：key = questId + objectiveId
    private final Map<String, SpawnPointData> spawnPoints = new ConcurrentHashMap<>();
    
    // 已刷新的怪物：key = entityUUID
    private final Map<UUID, SpawnedMobData> spawnedMobs = new ConcurrentHashMap<>();
    
    // 待重生队列：key = spawnPointKey, value = 重生时间
    private final Map<String, Long> respawnQueue = new ConcurrentHashMap<>();
    
    // 上次清理时间
    private volatile long lastCleanupTime = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL = 60_000L; // 1分钟
    
    private QuestMobSpawnManager() {}
    
    public static QuestMobSpawnManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new QuestMobSpawnManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册委托的刷新点
     */
    public void registerSpawnPoint(QuestInstance quest, LocationKillObjective objective) {
        if (objective.getSpawnConfig() == null || !objective.getSpawnConfig().enabled()) {
            return;
        }
        
        String key = makeKey(quest.getQuestId(), objective.getId());
        SpawnPointData data = new SpawnPointData(
                quest.getQuestId(),
                quest.getInstanceId(),
                objective.getId(),
                objective.getTargetResource(),
                objective.getLocations(),
                objective.getSpawnConfig(),
                quest.getPlayerId()
        );
        
        spawnPoints.put(key, data);
        RoadWeaverRPG.LOGGER.debug("Registered spawn point: {}", key);
    }
    
    /**
     * 注销委托的刷新点
     */
    public void unregisterSpawnPoint(ResourceLocation questId, String objectiveId) {
        String key = makeKey(questId, objectiveId);
        SpawnPointData removed = spawnPoints.remove(key);
        
        if (removed != null) {
            // 清理相关的已刷新怪物
            spawnedMobs.entrySet().removeIf(e -> 
                    e.getValue().questId().equals(questId) && 
                    e.getValue().objectiveId().equals(objectiveId));
            
            // 清理重生队列
            respawnQueue.keySet().removeIf(k -> k.startsWith(key));
            
            RoadWeaverRPG.LOGGER.debug("Unregistered spawn point: {}", key);
        }
    }
    
    /**
     * 处理怪物刷新（每tick调用）
     */
    public void tick(ServerLevel level) {
        long currentTime = level.getGameTime();
        
        // 处理重生队列
        processRespawnQueue(level, currentTime);
        
        // 检查并刷新怪物
        for (SpawnPointData data : spawnPoints.values()) {
            processSpawnPoint(level, data, currentTime);
        }
        
        // 定期清理
        cleanupIfNeeded();
    }
    
    /**
     * 处理单个刷新点
     */
    private void processSpawnPoint(ServerLevel level, SpawnPointData data, long currentTime) {
        // 检查玩家是否在线
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(data.playerId());
        if (player == null) return;
        
        // 检查玩家是否在附近（性能优化）
        boolean playerNearby = false;
        for (TargetLocation loc : data.locations()) {
            if (loc.distanceTo(player.blockPosition()) < 128) {
                playerNearby = true;
                break;
            }
        }
        if (!playerNearby) return;
        
        // 统计当前存活的怪物数量
        int aliveCount = countAliveMobs(data);
        int maxCount = data.spawnConfig().maxCount();
        
        // 需要刷新的数量
        int toSpawn = maxCount - aliveCount;
        if (toSpawn <= 0) return;
        
        // 刷新怪物
        for (int i = 0; i < toSpawn; i++) {
            spawnMob(level, data);
        }
    }
    
    /**
     * 刷新单个怪物
     */
    private void spawnMob(ServerLevel level, SpawnPointData data) {
        // 随机选择一个目标区域
        if (data.locations().isEmpty()) return;
        TargetLocation loc = data.locations().get(level.random.nextInt(data.locations().size()));
        
        // 检查维度
        if (loc.dimension() != null && !level.dimension().location().equals(loc.dimension())) {
            return;
        }
        
        // 计算刷新位置
        BlockPos spawnPos = findSpawnPosition(level, loc, data.spawnConfig().spawnRadius());
        if (spawnPos == null) return;
        
        // 获取实体类型
        Optional<EntityType<?>> entityTypeOpt = RegistryHelper.getEntityType(data.entityId());
        if (entityTypeOpt.isEmpty()) {
            RoadWeaverRPG.LOGGER.warn("Unknown entity type: {}", data.entityId());
            return;
        }
        
        try {
            Entity entity = entityTypeOpt.get().create(level);
            if (entity == null) return;
            
            entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.random.nextFloat() * 360f, 0f);
            
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null, null);
                
                if (data.spawnConfig().persistentMobs()) {
                    mob.setPersistenceRequired();
                }
                
                // 标记为委托怪物
                markAsQuestMob(mob, data);
            }
            
            level.addFreshEntity(entity);
            
            // 记录已刷新的怪物
            spawnedMobs.put(entity.getUUID(), new SpawnedMobData(
                    data.questId(),
                    data.objectiveId(),
                    data.playerId(),
                    entity.getUUID(),
                    System.currentTimeMillis()
            ));
            
            RoadWeaverRPG.LOGGER.debug("Spawned quest mob {} at {}", data.entityId(), spawnPos);
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to spawn quest mob: {}", e.getMessage());
        }
    }
    
    /**
     * 寻找合适的刷新位置
     */
    private BlockPos findSpawnPosition(ServerLevel level, TargetLocation loc, int spawnRadius) {
        BlockPos center = loc.center();
        
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = level.random.nextInt(spawnRadius * 2) - spawnRadius;
            int dz = level.random.nextInt(spawnRadius * 2) - spawnRadius;
            
            BlockPos testPos = center.offset(dx, 0, dz);
            
            // 寻找地面
            BlockPos groundPos = findGround(level, testPos);
            if (groundPos != null && isValidSpawnPosition(level, groundPos)) {
                return groundPos;
            }
        }
        
        return null;
    }
    
    /**
     * 寻找地面位置
     */
    private BlockPos findGround(ServerLevel level, BlockPos pos) {
        // 从上往下搜索
        for (int y = Math.min(pos.getY() + 32, level.getMaxBuildHeight() - 1); 
             y > level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(checkPos).isAir() && 
                !level.getBlockState(checkPos.below()).isAir()) {
                return checkPos;
            }
        }
        return null;
    }
    
    /**
     * 检查是否是有效的刷新位置
     */
    private boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        // 检查空间是否足够
        return level.getBlockState(pos).isAir() && 
               level.getBlockState(pos.above()).isAir() &&
               !level.getBlockState(pos.below()).isAir();
    }
    
    /**
     * 标记实体为委托怪物
     */
    private void markAsQuestMob(Mob mob, SpawnPointData data) {
        CompoundTag tag = new CompoundTag();
        mob.saveAsPassenger(tag);
        tag.putBoolean("QuestMob", true);
        tag.putString("QuestId", data.questId().toString());
        tag.putString("ObjectiveId", data.objectiveId());
        tag.putUUID("QuestPlayerId", data.playerId());
        mob.load(tag);
    }
    
    /**
     * 检查实体是否是委托怪物
     */
    public boolean isQuestMob(Entity entity) {
        if (!(entity instanceof Mob mob)) return false;
        CompoundTag tag = new CompoundTag();
        mob.saveAsPassenger(tag);
        return tag.getBoolean("QuestMob");
    }
    
    /**
     * 获取委托怪物的委托ID
     */
    public Optional<ResourceLocation> getQuestId(Entity entity) {
        if (!(entity instanceof Mob mob)) return Optional.empty();
        CompoundTag tag = new CompoundTag();
        mob.saveAsPassenger(tag);
        if (!tag.contains("QuestId")) return Optional.empty();
        return Optional.of(new ResourceLocation(tag.getString("QuestId")));
    }
    
    /**
     * 处理怪物死亡
     */
    public void onMobDeath(Entity entity) {
        SpawnedMobData mobData = spawnedMobs.remove(entity.getUUID());
        if (mobData == null) return;
        
        // 加入重生队列
        String key = makeKey(mobData.questId(), mobData.objectiveId());
        SpawnPointData spawnData = spawnPoints.get(key);
        
        if (spawnData != null && spawnData.spawnConfig().enabled()) {
            long respawnTime = System.currentTimeMillis() + 
                    (spawnData.spawnConfig().respawnDelayTicks() * 50L);
            respawnQueue.put(key + "_" + UUID.randomUUID(), respawnTime);
        }
    }
    
    /**
     * 处理重生队列
     */
    private void processRespawnQueue(ServerLevel level, long currentTime) {
        long currentMs = System.currentTimeMillis();
        
        Iterator<Map.Entry<String, Long>> it = respawnQueue.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (currentMs >= entry.getValue()) {
                it.remove();
                // 重生逻辑在processSpawnPoint中处理
            }
        }
    }
    
    /**
     * 统计存活的怪物数量
     */
    private int countAliveMobs(SpawnPointData data) {
        int count = 0;
        for (SpawnedMobData mob : spawnedMobs.values()) {
            if (mob.questId().equals(data.questId()) && 
                mob.objectiveId().equals(data.objectiveId())) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 定期清理过期数据
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL) return;
        
        lastCleanupTime = now;
        
        // 清理超时的已刷新怪物记录（超过10分钟）
        long timeout = now - 600_000L;
        spawnedMobs.entrySet().removeIf(e -> e.getValue().spawnTime() < timeout);
        
        RoadWeaverRPG.LOGGER.debug("Cleaned up quest mob spawn data");
    }
    
    /**
     * 清理指定玩家的所有刷新点
     */
    public void clearPlayerData(UUID playerId) {
        spawnPoints.entrySet().removeIf(e -> e.getValue().playerId().equals(playerId));
        spawnedMobs.entrySet().removeIf(e -> e.getValue().playerId().equals(playerId));
    }
    
    /**
     * 获取指定区域内的委托怪物
     */
    public List<Entity> getQuestMobsInArea(Level level, AABB area, ResourceLocation questId) {
        List<Entity> result = new ArrayList<>();
        
        for (Entity entity : level.getEntities(null, area)) {
            if (isQuestMob(entity)) {
                Optional<ResourceLocation> mobQuestId = getQuestId(entity);
                if (mobQuestId.isPresent() && mobQuestId.get().equals(questId)) {
                    result.add(entity);
                }
            }
        }
        
        return result;
    }
    
    private String makeKey(ResourceLocation questId, String objectiveId) {
        return questId.toString() + ":" + objectiveId;
    }
    
    // ==================== 持久化 ====================
    
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        
        ListTag spawnedList = new ListTag();
        for (SpawnedMobData mob : spawnedMobs.values()) {
            CompoundTag mobTag = new CompoundTag();
            mobTag.putString("questId", mob.questId().toString());
            mobTag.putString("objectiveId", mob.objectiveId());
            mobTag.putUUID("playerId", mob.playerId());
            mobTag.putUUID("entityId", mob.entityId());
            mobTag.putLong("spawnTime", mob.spawnTime());
            spawnedList.add(mobTag);
        }
        tag.put("spawnedMobs", spawnedList);
        
        return tag;
    }
    
    public void fromNbt(CompoundTag tag) {
        spawnedMobs.clear();
        
        if (tag.contains("spawnedMobs")) {
            ListTag list = tag.getList("spawnedMobs", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag mobTag = list.getCompound(i);
                SpawnedMobData data = new SpawnedMobData(
                        new ResourceLocation(mobTag.getString("questId")),
                        mobTag.getString("objectiveId"),
                        mobTag.getUUID("playerId"),
                        mobTag.getUUID("entityId"),
                        mobTag.getLong("spawnTime")
                );
                spawnedMobs.put(data.entityId(), data);
            }
        }
    }
    
    // ==================== 数据类 ====================
    
    private record SpawnPointData(
            ResourceLocation questId,
            UUID instanceId,
            String objectiveId,
            ResourceLocation entityId,
            List<TargetLocation> locations,
            SpawnConfig spawnConfig,
            UUID playerId
    ) {}
    
    private record SpawnedMobData(
            ResourceLocation questId,
            String objectiveId,
            UUID playerId,
            UUID entityId,
            long spawnTime
    ) {}
}
