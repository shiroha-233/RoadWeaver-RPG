package net.shiroha233.roadweaverpg.worlddifficulty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.shiroha233.roadweaverpg.platform.PlatformHelper;

/**
 * 怪物难度数据
 * 
 * 设计原理：
 * - 存储单个怪物的等级和稀有度信息
 * - 不可变设计，线程安全
 * - 支持Jade等模组读取显示
 * - 使用平台抽象层处理NBT操作
 */
public record MonsterData(
        int level,                    // 怪物等级
        MonsterRarity rarity,         // 稀有度
        MonsterStats stats,           // 计算后的属性
        boolean isScaled              // 是否已应用缩放
) {
    
    public static final String NBT_KEY = "RoadWeaverRPG_MonsterData";
    public static final String NBT_LEVEL = "Level";
    public static final String NBT_RARITY = "Rarity";
    public static final String NBT_SCALED = "Scaled";
    
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarInt(rarity.ordinal());
        stats.toNetwork(buf);
        buf.writeBoolean(isScaled);
    }
    
    public static MonsterData fromNetwork(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        MonsterRarity rarity = MonsterRarity.fromOrdinal(buf.readVarInt());
        MonsterStats stats = MonsterStats.fromNetwork(buf);
        boolean scaled = buf.readBoolean();
        return new MonsterData(level, rarity, stats, scaled);
    }
    
    public static MonsterData createDefault() {
        return new MonsterData(1, MonsterRarity.NORMAL, MonsterStats.empty(), false);
    }
    
    public static MonsterData create(int level, MonsterRarity rarity, MonsterStats stats) {
        return new MonsterData(level, rarity, stats, true);
    }
    
    /**
     * 从实体NBT读取数据
     */
    public static MonsterData fromEntity(LivingEntity entity) {
        CompoundTag tag = PlatformHelper.getEntityPersistentData(entity);
        if (tag != null && tag.contains(NBT_KEY)) {
            CompoundTag dataTag = tag.getCompound(NBT_KEY);
            int level = dataTag.getInt(NBT_LEVEL);
            MonsterRarity rarity = MonsterRarity.fromOrdinal(dataTag.getInt(NBT_RARITY));
            boolean scaled = dataTag.getBoolean(NBT_SCALED);
            return new MonsterData(level, rarity, MonsterStats.empty(), scaled);
        }
        return null;
    }
    
    /**
     * 保存数据到实体NBT
     */
    public void saveToEntity(LivingEntity entity) {
        CompoundTag tag = PlatformHelper.getEntityPersistentData(entity);
        if (tag == null) return;
        
        CompoundTag dataTag = tag.contains(NBT_KEY) ? tag.getCompound(NBT_KEY) : new CompoundTag();
        dataTag.putInt(NBT_LEVEL, level);
        dataTag.putInt(NBT_RARITY, rarity.ordinal());
        dataTag.putBoolean(NBT_SCALED, isScaled);
        tag.put(NBT_KEY, dataTag);
    }
}
