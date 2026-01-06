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
    private static final ResourceLocation SHOP_CONFIG = new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop");
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
        
        // 只读取 shop.json 文件
        JsonElement shopConfig = resources.get(SHOP_CONFIG);
        if (shopConfig == null) {
            RoadWeaverRPG.LOGGER.warn("Shop config not found: {}", SHOP_CONFIG);
            return;
        }
        
        try {
            JsonObject root = shopConfig.getAsJsonObject();
            JsonArray items = root.getAsJsonArray("items");
            
            for (JsonElement element : items) {
                JsonObject itemJson = element.getAsJsonObject();
                String id = itemJson.get("id").getAsString();
                
                ShopItem item = ShopItem.fromJson(id, itemJson);
                itemsByCategory.get(item.category()).add(item);
                itemsById.put(item.id(), item);
            }
            
            RoadWeaverRPG.LOGGER.info("Loaded {} shop items from config", itemsById.size());
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to load shop config", e);
        }
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
