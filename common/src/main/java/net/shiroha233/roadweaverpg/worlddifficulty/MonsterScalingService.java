package net.shiroha233.roadweaverpg.worlddifficulty;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.adventure.AdventureDataService;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 怪物属性缩放服务
 * 
 * 设计原理：
 * - 单一职责：负责计算和应用怪物属性缩放
 * - 与冒险等级系统集成
 * - 线程安全设计
 */
public final class MonsterScalingService {
    
    private static volatile MonsterScalingService instance;
    private static final Object LOCK = new Object();
    
    // 属性修改器UUID（固定，便于移除）
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID DAMAGE_MODIFIER_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID ARMOR_TOUGH_MODIFIER_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
    
    private static final String MODIFIER_NAME = "roadweaver_rpg_scaling";
    
    // 缓存已处理的实体（防止重复处理）
    private final ConcurrentHashMap<UUID, Long> processedEntities = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_MS = 60000; // 1分钟过期
    
    private final Random random = new Random();
    
    private MonsterScalingService() {
        // 定期清理过期缓存
        startCacheCleanup();
    }
    
    public static MonsterScalingService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new MonsterScalingService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 处理怪物生成事件
     * 根据附近玩家的冒险等级计算怪物属性
     */
    public void onMobSpawn(Mob mob, ServerLevel level) {
        if (mob == null || level == null) return;
        
        // 检查是否已处理
        MonsterData existingData = MonsterData.fromEntity(mob);
        if (existingData != null && existingData.isScaled()) {
            return;
        }
        
        // 防止短时间内重复处理
        UUID mobUuid = mob.getUUID();
        Long lastProcess = processedEntities.get(mobUuid);
        if (lastProcess != null && System.currentTimeMillis() - lastProcess < 1000) {
            return;
        }
        processedEntities.put(mobUuid, System.currentTimeMillis());
        
        try {
            // 获取附近最高冒险等级的玩家
            int adventureLevel = getHighestNearbyAdventureLevel(mob, level);
            
            // 获取怪物配置
            ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            DifficultyScaling config = DifficultyConfigManager.getInstance().getConfigForMob(mobId);
            
            // 决定稀有度
            MonsterRarity rarity = determineRarity(config, adventureLevel);
            
            // 计算属性
            MonsterStats stats = calculateStats(mob, config, adventureLevel, rarity);
            
            // 应用属性
            applyStats(mob, stats, rarity);
            
            // 保存数据
            MonsterData data = MonsterData.create(adventureLevel, rarity, stats);
            data.saveToEntity(mob);
            
            RoadWeaverRPG.LOGGER.debug("怪物属性缩放: {} Lv.{} [{}]", 
                    mobId, adventureLevel, rarity.getId());
            
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("怪物属性缩放失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取附近最高冒险等级
     */
    private int getHighestNearbyAdventureLevel(Mob mob, ServerLevel level) {
        int maxLevel = 1;
        double searchRadius = 64.0;
        
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                double distance = player.distanceTo(mob);
                if (distance <= searchRadius) {
                    int playerLevel = AdventureDataService.getInstance().getAdventureLevel(serverPlayer);
                    maxLevel = Math.max(maxLevel, playerLevel);
                }
            }
        }
        
        return maxLevel;
    }
    
    /**
     * 决定怪物稀有度
     */
    private MonsterRarity determineRarity(DifficultyScaling config, int level) {
        // 等级越高，稀有怪物概率越高
        float levelBonus = level * 0.005f;
        
        float roll = random.nextFloat();
        
        if (roll < config.legendaryChance() + levelBonus * 0.1f) {
            return MonsterRarity.LEGENDARY;
        } else if (roll < config.bossChance() + levelBonus * 0.5f) {
            return MonsterRarity.BOSS;
        } else if (roll < config.eliteChance() + levelBonus) {
            return MonsterRarity.ELITE;
        }
        
        return MonsterRarity.NORMAL;
    }
    
    /**
     * 计算怪物属性
     */
    private MonsterStats calculateStats(LivingEntity mob, DifficultyScaling config, 
                                         int level, MonsterRarity rarity) {
        float rarityMult = rarity.getStatMultiplier();
        int effectiveLevel = level - 1; // 1级为基准
        
        // 基础属性（每级增量 * 等级 * 稀有度倍率）
        float armor = config.armorPerLevel() * effectiveLevel * rarityMult;
        float health = config.healthPerLevel() * effectiveLevel * rarityMult;
        float resistance = Math.min(0.8f, config.resistancePerLevel() * effectiveLevel * rarityMult);
        float healthRegen = config.healthRegenPerLevel() * effectiveLevel * rarityMult;
        float physResist = Math.min(0.6f, config.physicalResistPerLevel() * effectiveLevel * rarityMult);
        float magicResist = Math.min(0.6f, config.magicResistPerLevel() * effectiveLevel * rarityMult);
        float damage = config.damagePerLevel() * effectiveLevel * rarityMult;
        
        // 计算环境加成
        MonsterStats.Builder envBuilder = EnvironmentEvaluator.calculateEnvironmentBonuses(
                mob, config.environmentBonuses());
        
        return MonsterStats.builder()
                .armor(armor)
                .health(health)
                .resistance(resistance)
                .healthRegen(healthRegen)
                .physicalResist(physResist)
                .magicResist(magicResist)
                .damage(damage)
                .envArmor(envBuilder.build().envArmorBonus())
                .envHealth(envBuilder.build().envHealthBonus())
                .envDamage(envBuilder.build().envDamageBonus())
                .envResistance(envBuilder.build().envResistanceBonus())
                .build();
    }
    
    /**
     * 应用属性到怪物
     */
    private void applyStats(LivingEntity mob, MonsterStats stats, MonsterRarity rarity) {
        // 生命值
        applyAttributeModifier(mob, Attributes.MAX_HEALTH, HEALTH_MODIFIER_UUID,
                stats.getTotalHealth(), AttributeModifier.Operation.ADDITION);
        
        // 护甲
        applyAttributeModifier(mob, Attributes.ARMOR, ARMOR_MODIFIER_UUID,
                stats.getTotalArmor(), AttributeModifier.Operation.ADDITION);
        
        // 攻击力
        applyAttributeModifier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_UUID,
                stats.getTotalDamage(), AttributeModifier.Operation.ADDITION);
        
        // 护甲韧性（基于抗性）
        applyAttributeModifier(mob, Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGH_MODIFIER_UUID,
                stats.getTotalResistance() * 10, AttributeModifier.Operation.ADDITION);
        
        // 恢复满血
        mob.setHealth(mob.getMaxHealth());
    }
    
    private void applyAttributeModifier(LivingEntity entity, 
                                         net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                         UUID uuid, double amount, 
                                         AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        
        // 移除旧的修改器
        instance.removeModifier(uuid);
        
        if (amount > 0) {
            instance.addPermanentModifier(new AttributeModifier(
                    uuid, MODIFIER_NAME, amount, operation));
        }
    }
    
    /**
     * 启动缓存清理任务
     */
    private void startCacheCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CACHE_EXPIRE_MS);
                    long now = System.currentTimeMillis();
                    processedEntities.entrySet().removeIf(
                            entry -> now - entry.getValue() > CACHE_EXPIRE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "RoadWeaverRPG-MonsterCache-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * 获取怪物数据（用于Jade等模组显示）
     */
    public MonsterData getMonsterData(LivingEntity entity) {
        return MonsterData.fromEntity(entity);
    }
}
