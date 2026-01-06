package net.shiroha233.roadweaverpg.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * 注册表辅助类
 * 提供平台无关的注册表访问方法
 */
public final class RegistryHelper {
    
    private RegistryHelper() {}
    
    /** 根据ID获取物品 */
    public static Optional<Item> getItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item != null ? Optional.of(item) : Optional.empty();
    }
    
    /** 根据ID获取实体类型 */
    public static Optional<EntityType<?>> getEntityType(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return type != null ? Optional.of(type) : Optional.empty();
    }
    
    /** 根据ID获取方块 */
    public static Optional<Block> getBlock(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block != null ? Optional.of(block) : Optional.empty();
    }
    
    /** 获取物品的注册ID */
    public static Optional<ResourceLocation> getItemId(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return Optional.ofNullable(id);
    }
    
    /** 获取实体类型的注册ID */
    public static Optional<ResourceLocation> getEntityTypeId(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return Optional.ofNullable(id);
    }
    
    /** 获取方块的注册ID */
    public static Optional<ResourceLocation> getBlockId(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return Optional.ofNullable(id);
    }
}
