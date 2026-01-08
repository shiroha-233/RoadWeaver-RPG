package net.shiroha233.roadweaverpg.shop;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.item.ModItems;

import java.util.*;

/**
 * 商店管理器 - 从单个JSON文件加载所有商品
 * 文件路径: data/roadweaver_rpg/shop_config/shop.json
 */
public class ShopManager extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ShopManager instance;
    
    private final Map<ShopCategory, List<ShopItem>> itemsByCategory = new EnumMap<>(ShopCategory.class);
    private final Map<ResourceLocation, ShopItem> itemsById = new HashMap<>();
    
    public ShopManager() {
        super(GSON, "shop_config");
        instance = this;
    }
    
    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        itemsByCategory.clear();
        itemsById.clear();
        
        for (ShopCategory cat : ShopCategory.values()) {
            itemsByCategory.put(cat, new ArrayList<>());
        }
        
        if (resources.isEmpty()) {
            RoadWeaverRPG.LOGGER.warn("No shop configs found under data/{}/shop_config", RoadWeaverRPG.MOD_ID);
            return;
        }

        int totalItems = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation configId = entry.getKey();
            JsonElement element = entry.getValue();
            try {
                if (!element.isJsonObject()) {
                    RoadWeaverRPG.LOGGER.warn("Skip shop config {}: root is not a JsonObject", configId);
                    continue;
                }
                JsonObject root = element.getAsJsonObject();
                JsonArray items = root.getAsJsonArray("items");
                if (items == null) {
                    RoadWeaverRPG.LOGGER.warn("Skip shop config {}: missing 'items' array", configId);
                    continue;
                }
                
                for (JsonElement itemElement : items) {
                    if (!itemElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject itemJson = itemElement.getAsJsonObject();
                    String id = itemJson.get("id").getAsString();
                    
                    ShopItem item = ShopItem.fromJson(id, itemJson);
                    itemsByCategory.get(item.category()).add(item);
                    itemsById.put(item.id(), item);
                    totalItems++;
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load shop config file {}", configId, e);
            }
        }

        RoadWeaverRPG.LOGGER.info("Loaded {} shop items from {} shop config files", totalItems, resources.size());
    }
    
    public List<ShopItem> getItemsByCategory(ShopCategory category) {
        return Collections.unmodifiableList(itemsByCategory.getOrDefault(category, Collections.emptyList()));
    }
    
    public List<ShopItem> getAllItems() {
        List<ShopItem> all = new ArrayList<>();
        itemsByCategory.values().forEach(all::addAll);
        return all;
    }
    
    public Optional<ShopItem> getItem(ResourceLocation id) {
        return Optional.ofNullable(itemsById.get(id));
    }
    
    /** 处理购买请求 */
    public PurchaseResult purchase(ServerPlayer player, ResourceLocation itemId, int quantity) {
        Optional<ShopItem> itemOpt = getItem(itemId);
        if (itemOpt.isEmpty()) {
            return PurchaseResult.ITEM_NOT_FOUND;
        }
        
        ShopItem shopItem = itemOpt.get();
        
        // 检查声望等级要求
        if (shopItem.requiredLevel() > 0) {
            int currentLevel = net.shiroha233.roadweaverpg.data.QuestDataAccessor.getInstance()
                    .getPlayerData(player)
                    .getReputationLevel(new ResourceLocation("roadweaver_rpg", "guild"));
            if (currentLevel < shopItem.requiredLevel()) {
                return PurchaseResult.INSUFFICIENT_LEVEL;
            }
        }
        
        int totalPrice = shopItem.price() * quantity;
        
        int playerCoins = countPlayerCoins(player);
        if (playerCoins < totalPrice) {
            return PurchaseResult.INSUFFICIENT_COINS;
        }
        
        if (!removeCoins(player, totalPrice)) {
            return PurchaseResult.INSUFFICIENT_COINS;
        }
        
        ItemStack stack = shopItem.createItemStack();
        stack.setCount(shopItem.count() * quantity);
        
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        
        return PurchaseResult.SUCCESS;
    }
    
    public int countPlayerCoins(ServerPlayer player) {
        if (ModItems.COIN == null) return 0;
        
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.COIN.get()) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private boolean removeCoins(ServerPlayer player, int amount) {
        if (ModItems.COIN == null) return false;
        
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ModItems.COIN.get()) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0;
    }
    
    public enum PurchaseResult {
        SUCCESS, ITEM_NOT_FOUND, INSUFFICIENT_COINS, INSUFFICIENT_LEVEL, OUT_OF_STOCK
    }
}
