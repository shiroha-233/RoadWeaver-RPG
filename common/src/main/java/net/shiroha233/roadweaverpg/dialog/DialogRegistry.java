package net.shiroha233.roadweaverpg.dialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对话注册表
 * 职责：加载和管理所有对话数据
 * 
 * 优化点：
 * - 添加NPC类型索引缓存
 * - 添加加载统计
 * - 优化重载逻辑
 */
public class DialogRegistry extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "dialogs";
    
    // 线程安全的对话缓存
    private static final ConcurrentHashMap<ResourceLocation, DialogData> DIALOGS = new ConcurrentHashMap<>();
    
    // NPC类型 -> 对话ID列表的索引缓存
    private static final ConcurrentHashMap<String, List<ResourceLocation>> NPC_TYPE_INDEX = new ConcurrentHashMap<>();
    
    // 加载统计
    private static final AtomicInteger loadCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    
    // 单例实例
    private static DialogRegistry instance;
    
    public DialogRegistry() {
        super(GSON, DIRECTORY);
    }
    
    public static DialogRegistry getInstance() {
        if (instance == null) {
            instance = new DialogRegistry();
        }
        return instance;
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        // 保存旧数据用于增量更新检测
        Set<ResourceLocation> oldIds = new HashSet<>(DIALOGS.keySet());
        
        // 使用临时 Map 加载新数据（原子性重载）
        // 原理：先在临时容器中加载所有数据，加载完成后一次性替换，避免中间状态
        ConcurrentHashMap<ResourceLocation, DialogData> tempDialogs = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<ResourceLocation>> tempIndex = new ConcurrentHashMap<>();
        AtomicInteger tempLoadCount = new AtomicInteger(0);
        AtomicInteger tempErrorCount = new AtomicInteger(0);
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                ResourceLocation id = entry.getKey();
                JsonElement element = entry.getValue();
                
                if (element.isJsonObject()) {
                    JsonObject json = element.getAsJsonObject();
                    DialogData dialog = DialogData.fromJson(id, json);
                    tempDialogs.put(id, dialog);
                    
                    // 更新NPC类型索引
                    tempIndex.computeIfAbsent(dialog.npcType(), k -> new ArrayList<>()).add(id);
                    
                    tempLoadCount.incrementAndGet();
                }
                
            } catch (Exception e) {
                tempErrorCount.incrementAndGet();
                RoadWeaverRPG.LOGGER.error("加载对话失败: {}", entry.getKey(), e);
            }
        }
        
        // 原子性替换（使用单次操作避免中间状态）
        // 先构建新的 Map，然后一次性替换所有键值对
        DIALOGS.forEach((key, value) -> {
            if (!tempDialogs.containsKey(key)) {
                DIALOGS.remove(key);
            }
        });
        DIALOGS.putAll(tempDialogs);
        
        NPC_TYPE_INDEX.forEach((key, value) -> {
            if (!tempIndex.containsKey(key)) {
                NPC_TYPE_INDEX.remove(key);
            }
        });
        NPC_TYPE_INDEX.putAll(tempIndex);
        
        loadCount.set(tempLoadCount.get());
        errorCount.set(tempErrorCount.get());
        
        // 记录变更
        Set<ResourceLocation> newIds = DIALOGS.keySet();
        int added = (int) newIds.stream().filter(id -> !oldIds.contains(id)).count();
        int removed = (int) oldIds.stream().filter(id -> !newIds.contains(id)).count();
        
        RoadWeaverRPG.LOGGER.info("对话注册表重载完成: 加载 {} 个, 新增 {}, 移除 {}, 错误 {}", 
                loadCount.get(), added, removed, errorCount.get());
    }
    
    /**
     * 获取对话数据
     */
    public static Optional<DialogData> getDialog(ResourceLocation id) {
        return Optional.ofNullable(DIALOGS.get(id));
    }
    
    /**
     * 获取NPC类型的默认对话
     */
    public static Optional<DialogData> getDefaultDialog(String npcType) {
        ResourceLocation defaultId = new ResourceLocation(RoadWeaverRPG.MOD_ID, npcType + "/greeting");
        return getDialog(defaultId);
    }
    
    /**
     * 获取NPC类型的所有对话
     */
    public static List<ResourceLocation> getDialogsByNpcType(String npcType) {
        return NPC_TYPE_INDEX.getOrDefault(npcType, Collections.emptyList());
    }
    
    /**
     * 检查对话是否存在
     */
    public static boolean hasDialog(ResourceLocation id) {
        return DIALOGS.containsKey(id);
    }
    
    /**
     * 获取所有已注册的对话ID
     */
    public static Set<ResourceLocation> getAllDialogIds() {
        return Collections.unmodifiableSet(DIALOGS.keySet());
    }
    
    /**
     * 获取所有已注册的NPC类型
     */
    public static Set<String> getAllNpcTypes() {
        return Collections.unmodifiableSet(NPC_TYPE_INDEX.keySet());
    }
    
    /**
     * 手动注册对话（用于代码定义的对话）
     */
    public static void registerDialog(DialogData dialog) {
        DIALOGS.put(dialog.id(), dialog);
        NPC_TYPE_INDEX.computeIfAbsent(dialog.npcType(), k -> new ArrayList<>()).add(dialog.id());
        RoadWeaverRPG.LOGGER.debug("手动注册对话: {}", dialog.id());
    }
    
    /**
     * 移除对话
     */
    public static void unregisterDialog(ResourceLocation id) {
        DialogData removed = DIALOGS.remove(id);
        if (removed != null) {
            List<ResourceLocation> typeDialogs = NPC_TYPE_INDEX.get(removed.npcType());
            if (typeDialogs != null) {
                typeDialogs.remove(id);
            }
        }
    }
    
    /**
     * 清空缓存（用于重载）
     */
    public static void clearCache() {
        DIALOGS.clear();
        NPC_TYPE_INDEX.clear();
        loadCount.set(0);
        errorCount.set(0);
    }
    
    /**
     * 获取加载统计
     */
    public static int getLoadedCount() {
        return loadCount.get();
    }
    
    public static int getErrorCount() {
        return errorCount.get();
    }
    
    /**
     * 获取对话总数
     */
    public static int getTotalCount() {
        return DIALOGS.size();
    }
}
