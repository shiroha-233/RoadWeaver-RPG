package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;

import java.util.Optional;

/**
 * 物品条件判定
 */
public final class ItemCondition implements PlayerCondition<ConditionContext> {
    
    public enum ItemCheckType {
        HAS_ITEM,
        HAS_ITEM_TAG,
        MAIN_HAND,
        OFF_HAND,
        HOLDING,
        WEARING
    }
    
    private final ItemCheckType type;
    private final ResourceLocation itemId;
    private final ResourceLocation tagId;
    private final int count;
    private final EquipmentSlot slot;
    
    private ItemCondition(ItemCheckType type, ResourceLocation itemId, ResourceLocation tagId, 
                          int count, EquipmentSlot slot) {
        this.type = type;
        this.itemId = itemId;
        this.tagId = tagId;
        this.count = count;
        this.slot = slot;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        return switch (type) {
            case HAS_ITEM -> checkHasItem(player);
            case HAS_ITEM_TAG -> checkHasItemTag(player);
            case MAIN_HAND -> checkMainHand(player);
            case OFF_HAND -> checkOffHand(player);
            case HOLDING -> checkHolding(player);
            case WEARING -> checkWearing(player);
        };
    }
    
    private boolean checkHasItem(ServerPlayer player) {
        Optional<Item> item = RegistryHelper.getItem(itemId);
        if (item.isEmpty()) return false;
        
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item.get()) {
                total += stack.getCount();
            }
        }
        return total >= count;
    }
    
    private boolean checkHasItemTag(ServerPlayer player) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(tag)) {
                total += stack.getCount();
            }
        }
        return total >= count;
    }
    
    private boolean checkMainHand(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        Optional<Item> item = RegistryHelper.getItem(itemId);
        return item.isPresent() && stack.getItem() == item.get();
    }
    
    private boolean checkOffHand(ServerPlayer player) {
        ItemStack stack = player.getOffhandItem();
        Optional<Item> item = RegistryHelper.getItem(itemId);
        return item.isPresent() && stack.getItem() == item.get();
    }
    
    private boolean checkHolding(ServerPlayer player) {
        return checkMainHand(player) || checkOffHand(player);
    }
    
    private boolean checkWearing(ServerPlayer player) {
        if (slot == null) return false;
        ItemStack stack = player.getItemBySlot(slot);
        Optional<Item> item = RegistryHelper.getItem(itemId);
        return item.isPresent() && stack.getItem() == item.get();
    }
    
    // 工厂方法
    public static ItemCondition hasItem(String itemId, int count) {
        return new ItemCondition(ItemCheckType.HAS_ITEM, new ResourceLocation(itemId), null, count, null);
    }
    
    public static ItemCondition hasItemTag(String tagId, int count) {
        return new ItemCondition(ItemCheckType.HAS_ITEM_TAG, null, new ResourceLocation(tagId), count, null);
    }
    
    public static ItemCondition mainHand(String itemId) {
        return new ItemCondition(ItemCheckType.MAIN_HAND, new ResourceLocation(itemId), null, 1, null);
    }
    
    public static ItemCondition offHand(String itemId) {
        return new ItemCondition(ItemCheckType.OFF_HAND, new ResourceLocation(itemId), null, 1, null);
    }
    
    public static ItemCondition holding(String itemId) {
        return new ItemCondition(ItemCheckType.HOLDING, new ResourceLocation(itemId), null, 1, null);
    }
    
    public static ItemCondition wearing(String itemId, EquipmentSlot slot) {
        return new ItemCondition(ItemCheckType.WEARING, new ResourceLocation(itemId), null, 1, slot);
    }
    
    public ItemCheckType getType() {
        return type;
    }
}
