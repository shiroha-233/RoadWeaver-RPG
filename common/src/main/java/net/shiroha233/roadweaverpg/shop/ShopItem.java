package net.shiroha233.roadweaverpg.shop;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.platform.RegistryHelper;

import java.util.Optional;

/**
 * 商店商品定义
 */
public record ShopItem(
        ResourceLocation id,
        ResourceLocation itemId,
        int count,
        int price,
        int stock,
        int requiredLevel,
        ShopCategory category,
        Optional<CompoundTag> nbt
) {
    
    public Component getDisplayName() {
        ItemStack stack = createItemStack();
        if (stack.isEmpty()) {
            return Component.literal(itemId.toString());
        }
        return stack.getHoverName();
    }
    
    public ItemStack createItemStack() {
        Optional<Item> item = RegistryHelper.getItem(itemId);
        if (item.isEmpty()) return ItemStack.EMPTY;
        
        ItemStack stack = new ItemStack(item.get(), count);
        nbt.ifPresent(stack::setTag);
        return stack;
    }
    
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeResourceLocation(itemId);
        buf.writeVarInt(count);
        buf.writeVarInt(price);
        buf.writeVarInt(stock);
        buf.writeVarInt(requiredLevel);
        buf.writeUtf(category.getId());
        buf.writeOptional(nbt, (b, tag) -> b.writeNbt(tag));
    }
    
    public static ShopItem fromNetwork(FriendlyByteBuf buf) {
        return new ShopItem(
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                ShopCategory.fromString(buf.readUtf()),
                buf.readOptional(FriendlyByteBuf::readNbt)
        );
    }
    
    public static ShopItem fromJson(String id, JsonObject json) {
        ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        int price = json.get("price").getAsInt();
        int stock = json.has("stock") ? json.get("stock").getAsInt() : -1;
        int requiredLevel = json.has("required_level") ? json.get("required_level").getAsInt() : 0;
        ShopCategory category = ShopCategory.fromString(
                json.has("category") ? json.get("category").getAsString() : "materials");
        
        Optional<CompoundTag> nbt = Optional.empty();
        if (json.has("nbt")) {
            try {
                nbt = Optional.of(TagParser.parseTag(json.get("nbt").getAsString()));
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to parse NBT for shop item: " + id, e);
            }
        }
        
        return new ShopItem(
                new ResourceLocation("roadweaver_rpg", id),
                itemId, count, price, stock, requiredLevel, category, nbt
        );
    }
}
