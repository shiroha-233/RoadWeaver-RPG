package net.shiroha233.roadweaverpg.quest.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective;
import net.shiroha233.roadweaverpg.quest.objective.LocationKillObjective.TargetLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 委托掉落物收集器
 * 
 * 功能：
 * 1. 自动收集委托区域内的掉落物
 * 2. 将掉落物发送给玩家
 * 3. 支持延迟收集（避免立即消失）
 * 4. 支持收集范围配置
 * 
 * 设计原则：
 * - 性能优化：批量处理，避免频繁遍历
 * - 线程安全：使用ConcurrentHashMap
 * - 可配置：支持自定义收集行为
 */
public class QuestLootCollector {
    
    private static volatile QuestLootCollector instance;
    private static final Object LOCK = new Object();
    
    // 活跃的收集区域：key = questId:objectiveId
    private final Map<String, CollectionZone> activeZones = new ConcurrentHashMap<>();
    
    // 待收集的掉落物：key = itemEntityUUID
    private final Map<UUID, PendingLoot> pendingLoots = new ConcurrentHashMap<>();
    
    // 收集延迟（tick）
    private static final int COLLECT_DELAY_TICKS = 40; // 2秒
    
    // 收集范围扩展
    private static final int COLLECT_RANGE_EXTENSION = 8;
    
    private QuestLootCollector() {}
    
    public static QuestLootCollector getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new QuestLootCollector();
                }
            }
        }
        return instance;
    }

    /**
     * 注册收集区域
     */
    public void registerCollectionZone(QuestInstance quest, LocationKillObjective objective) {
        if (!objective.isAutoCollectDrops()) return;
        
        String key = makeKey(quest.getQuestId(), objective.getId());
        CollectionZone zone = new CollectionZone(
                quest.getQuestId(),
                quest.getInstanceId(),
                objective.getId(),
                quest.getPlayerId(),
                objective.getLocations()
        );
        
        activeZones.put(key, zone);
        RoadWeaverRPG.LOGGER.debug("Registered loot collection zone: {}", key);
    }
    
    /**
     * 注销收集区域
     */
    public void unregisterCollectionZone(ResourceLocation questId, String objectiveId) {
        String key = makeKey(questId, objectiveId);
        activeZones.remove(key);
        
        // 清理相关的待收集物品
        pendingLoots.entrySet().removeIf(e -> 
                e.getValue().questId().equals(questId) && 
                e.getValue().objectiveId().equals(objectiveId));
        
        RoadWeaverRPG.LOGGER.debug("Unregistered loot collection zone: {}", key);
    }
    
    /**
     * 处理实体死亡产生的掉落物
     */
    public void onEntityDeath(ServerLevel level, BlockPos deathPos, 
                               ResourceLocation questId, String objectiveId, UUID playerId) {
        String key = makeKey(questId, objectiveId);
        CollectionZone zone = activeZones.get(key);
        if (zone == null) return;
        
        // 延迟收集，让掉落物先生成
        level.getServer().execute(() -> {
            // 等待几tick让掉落物生成
            scheduleCollection(level, deathPos, zone, level.getGameTime() + 5);
        });
    }
    
    /**
     * 每tick处理
     */
    public void tick(ServerLevel level) {
        long currentTime = level.getGameTime();
        
        // 处理待收集的掉落物
        processPendingLoots(level, currentTime);
        
        // 扫描活跃区域内的掉落物
        scanActiveZones(level, currentTime);
    }
    
    /**
     * 扫描活跃区域
     */
    private void scanActiveZones(ServerLevel level, long currentTime) {
        // 每20tick扫描一次（1秒）
        if (currentTime % 20 != 0) return;
        
        for (CollectionZone zone : activeZones.values()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(zone.playerId());
            if (player == null) continue;
            
            for (TargetLocation loc : zone.locations()) {
                // 检查维度
                if (loc.dimension() != null && 
                    !level.dimension().location().equals(loc.dimension())) {
                    continue;
                }
                
                scanAreaForLoot(level, zone, loc, currentTime);
            }
        }
    }
    
    /**
     * 扫描区域内的掉落物
     */
    private void scanAreaForLoot(ServerLevel level, CollectionZone zone, 
                                  TargetLocation loc, long currentTime) {
        int radius = loc.radius() + COLLECT_RANGE_EXTENSION;
        BlockPos center = loc.center();
        
        AABB area = new AABB(
                center.getX() - radius, center.getY() - radius * 2, center.getZ() - radius,
                center.getX() + radius, center.getY() + radius * 2, center.getZ() + radius
        );
        
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        
        for (ItemEntity item : items) {
            if (item.isRemoved()) continue;
            
            UUID itemId = item.getUUID();
            if (pendingLoots.containsKey(itemId)) continue;
            
            // 添加到待收集队列
            pendingLoots.put(itemId, new PendingLoot(
                    itemId,
                    zone.questId(),
                    zone.objectiveId(),
                    zone.playerId(),
                    currentTime + COLLECT_DELAY_TICKS
            ));
        }
    }

    /**
     * 处理待收集的掉落物
     */
    private void processPendingLoots(ServerLevel level, long currentTime) {
        Iterator<Map.Entry<UUID, PendingLoot>> it = pendingLoots.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<UUID, PendingLoot> entry = it.next();
            PendingLoot pending = entry.getValue();
            
            // 检查是否到达收集时间
            if (currentTime < pending.collectTime()) continue;
            
            it.remove();
            
            // 查找物品实体
            ItemEntity itemEntity = findItemEntity(level, pending.itemEntityId());
            if (itemEntity == null || itemEntity.isRemoved()) continue;
            
            // 查找玩家
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(pending.playerId());
            if (player == null) continue;
            
            // 收集物品
            collectItem(player, itemEntity);
        }
    }
    
    /**
     * 查找物品实体
     */
    private ItemEntity findItemEntity(ServerLevel level, UUID entityId) {
        for (var entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && entity.getUUID().equals(entityId)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 收集物品到玩家背包
     */
    private void collectItem(ServerPlayer player, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem().copy();
        
        if (player.getInventory().add(stack)) {
            // 成功添加到背包
            itemEntity.discard();
            
            // 发送提示
            player.displayClientMessage(
                    Component.translatable("message.roadweaver_rpg.loot_collected", 
                            stack.getCount(), stack.getHoverName()),
                    true
            );
            
            RoadWeaverRPG.LOGGER.debug("Collected loot for player {}: {} x{}", 
                    player.getName().getString(), stack.getItem(), stack.getCount());
        } else {
            // 背包满，掉落在玩家脚下
            player.drop(stack, false);
            itemEntity.discard();
            
            player.displayClientMessage(
                    Component.translatable("message.roadweaver_rpg.loot_dropped_nearby"),
                    true
            );
        }
    }
    
    /**
     * 延迟收集调度
     */
    private void scheduleCollection(ServerLevel level, BlockPos pos, 
                                     CollectionZone zone, long collectTime) {
        int radius = COLLECT_RANGE_EXTENSION;
        AABB area = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius
        );
        
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        
        for (ItemEntity item : items) {
            if (item.isRemoved()) continue;
            
            UUID itemId = item.getUUID();
            if (!pendingLoots.containsKey(itemId)) {
                pendingLoots.put(itemId, new PendingLoot(
                        itemId,
                        zone.questId(),
                        zone.objectiveId(),
                        zone.playerId(),
                        collectTime
                ));
            }
        }
    }
    
    /**
     * 清理玩家数据
     */
    public void clearPlayerData(UUID playerId) {
        activeZones.entrySet().removeIf(e -> e.getValue().playerId().equals(playerId));
        pendingLoots.entrySet().removeIf(e -> e.getValue().playerId().equals(playerId));
    }
    
    /**
     * 检查位置是否在收集区域内（使用condition系统）
     */
    public boolean isInCollectionZone(BlockPos pos, ResourceLocation questId) {
        for (CollectionZone zone : activeZones.values()) {
            if (!zone.questId().equals(questId)) continue;
            
            for (TargetLocation loc : zone.locations()) {
                // 使用toCondition()进行判定
                if (loc.toCondition().evaluate(null, 
                        net.shiroha233.roadweaverpg.condition.ConditionContext.ofPosition(pos))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private String makeKey(ResourceLocation questId, String objectiveId) {
        return questId.toString() + ":" + objectiveId;
    }
    
    // ==================== 数据类 ====================
    
    private record CollectionZone(
            ResourceLocation questId,
            UUID instanceId,
            String objectiveId,
            UUID playerId,
            List<TargetLocation> locations
    ) {}
    
    private record PendingLoot(
            UUID itemEntityId,
            ResourceLocation questId,
            String objectiveId,
            UUID playerId,
            long collectTime
    ) {}
}
