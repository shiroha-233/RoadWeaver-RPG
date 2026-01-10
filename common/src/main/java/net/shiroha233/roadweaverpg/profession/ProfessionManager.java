package net.shiroha233.roadweaverpg.profession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 职业管理器 - 从数据包加载职业定义
 * 
 * 设计原理：
 * - 继承SimpleJsonResourceReloadListener实现数据包热重载
 * - 使用ConcurrentHashMap保证线程安全
 * - 提供职业查询、筛选等功能
 * 
 * 数据包路径: data/<namespace>/professions/
 */
public class ProfessionManager extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "professions";
    
    private static volatile ProfessionManager instance;
    private static final Object LOCK = new Object();
    
    // 职业定义缓存
    private final Map<ResourceLocation, ProfessionDefinition> professions = new ConcurrentHashMap<>();
    
    // 默认职业ID（新玩家可选）
    private final Set<ResourceLocation> defaultProfessions = ConcurrentHashMap.newKeySet();
    
    // 同步回调
    private static BiConsumer<ServerPlayer, Collection<ProfessionDefinition>> syncCallback;
    
    public ProfessionManager() {
        super(GSON, DIRECTORY);
        instance = this;
    }
    
    public static ProfessionManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException("ProfessionManager not initialized");
                }
            }
        }
        return instance;
    }
    
    public static boolean isInitialized() {
        return instance != null;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, 
                         ResourceManager manager, ProfilerFiller profiler) {
        professions.clear();
        defaultProfessions.clear();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                ResourceLocation fileId = entry.getKey();
                JsonObject json = entry.getValue().getAsJsonObject();
                
                // 构建职业ID
                ResourceLocation professionId = json.has("id") 
                        ? new ResourceLocation(json.get("id").getAsString())
                        : new ResourceLocation(fileId.getNamespace(), fileId.getPath());
                
                ProfessionDefinition definition = ProfessionDefinition.fromJson(professionId, json);
                professions.put(professionId, definition);
                
                if (definition.isDefaultProfession()) {
                    defaultProfessions.add(professionId);
                }
                
                RoadWeaverRPG.LOGGER.debug("Loaded profession: {}", professionId);
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load profession: {}", entry.getKey(), e);
            }
        }
        
        RoadWeaverRPG.LOGGER.info("Loaded {} professions ({} default)", 
                professions.size(), defaultProfessions.size());
    }

    // region 查询方法
    
    /**
     * 获取职业定义
     */
    public ProfessionDefinition getProfession(ResourceLocation id) {
        return professions.get(id);
    }
    
    /**
     * 获取所有职业
     */
    public Collection<ProfessionDefinition> getAllProfessions() {
        return Collections.unmodifiableCollection(professions.values());
    }
    
    /**
     * 获取所有职业ID
     */
    public Set<ResourceLocation> getAllProfessionIds() {
        return Collections.unmodifiableSet(professions.keySet());
    }
    
    /**
     * 获取默认职业（新玩家可选）
     */
    public Collection<ProfessionDefinition> getDefaultProfessions() {
        return defaultProfessions.stream()
                .map(professions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 按标签筛选职业
     */
    public Collection<ProfessionDefinition> getProfessionsByTag(String tag) {
        return professions.values().stream()
                .filter(p -> p.getTags().contains(tag))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取排序后的职业列表
     */
    public List<ProfessionDefinition> getSortedProfessions() {
        return professions.values().stream()
                .sorted(Comparator.comparingInt(ProfessionDefinition::getSortOrder)
                        .thenComparing(p -> p.getId().toString()))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查职业是否存在
     */
    public boolean hasProfession(ResourceLocation id) {
        return professions.containsKey(id);
    }
    
    /**
     * 获取职业数量
     */
    public int getProfessionCount() {
        return professions.size();
    }
    
    // endregion
    
    // region 同步方法
    
    /**
     * 设置同步回调
     */
    public static void setSyncCallback(BiConsumer<ServerPlayer, Collection<ProfessionDefinition>> callback) {
        syncCallback = callback;
    }
    
    /**
     * 同步所有职业定义到客户端
     */
    public void syncToClient(ServerPlayer player) {
        if (syncCallback != null) {
            syncCallback.accept(player, professions.values());
        }
    }
    
    /**
     * 同步到所有在线玩家
     */
    public void syncToAllPlayers(net.minecraft.server.MinecraftServer server) {
        if (syncCallback == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncCallback.accept(player, professions.values());
        }
    }
    
    // endregion
    
    // region 客户端缓存（用于客户端接收同步数据）
    
    /**
     * 客户端接收同步数据后更新缓存
     */
    public void updateFromSync(Collection<ProfessionDefinition> definitions) {
        professions.clear();
        defaultProfessions.clear();
        
        for (ProfessionDefinition def : definitions) {
            professions.put(def.getId(), def);
            if (def.isDefaultProfession()) {
                defaultProfessions.add(def.getId());
            }
        }
        
        RoadWeaverRPG.LOGGER.debug("Synced {} professions from server", professions.size());
    }
    
    // endregion
}
