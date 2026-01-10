package net.shiroha233.roadweaverpg.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.util.EnumMap;
import java.util.Map;

/**
 * 属性加点配置管理器
 * 从数据包加载每点技能点对应的属性增益
 * 路径: data/<namespace>/stat_allocation/config.json
 */
public class StatAllocationConfig extends SimpleJsonResourceReloadListener {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "stat_allocation";
    
    private static volatile StatAllocationConfig instance;
    private static final Object LOCK = new Object();
    
    // 每点技能点对应的属性增益
    private final Map<StatType, Double> bonusPerPoint = new EnumMap<>(StatType.class);
    
    // 默认值
    private static final Map<StatType, Double> DEFAULT_BONUS = new EnumMap<>(StatType.class);
    static {
        // 基础属性
        DEFAULT_BONUS.put(StatType.MAX_HEALTH, 5.0);      // 每点+5生命值
        DEFAULT_BONUS.put(StatType.MAX_MANA, 10.0);       // 每点+10魔力值
        DEFAULT_BONUS.put(StatType.ATTACK, 1.0);          // 每点+1攻击力
        DEFAULT_BONUS.put(StatType.DEFENSE, 1.0);         // 每点+1防御力
        DEFAULT_BONUS.put(StatType.MAGIC_ATTACK, 1.0);    // 每点+1魔法攻击
        DEFAULT_BONUS.put(StatType.MAGIC_DEFENSE, 1.0);   // 每点+1魔法防御
        // 战斗属性
        DEFAULT_BONUS.put(StatType.CRIT_RATE, 0.5);       // 每点+0.5%暴击率
        DEFAULT_BONUS.put(StatType.CRIT_DAMAGE, 2.0);     // 每点+2%暴击伤害
        DEFAULT_BONUS.put(StatType.ATTACK_COOLDOWN, 1.0); // 每点-1%攻击冷却
        DEFAULT_BONUS.put(StatType.MOVE_SPEED, 0.5);      // 每点+0.5%移动速度
        DEFAULT_BONUS.put(StatType.HEALTH_REGEN, 0.1);    // 每点+0.1生命回复
        DEFAULT_BONUS.put(StatType.MANA_REGEN, 0.2);      // 每点+0.2魔力回复
    }
    
    public StatAllocationConfig() {
        super(GSON, DIRECTORY);
        // 初始化默认值
        bonusPerPoint.putAll(DEFAULT_BONUS);
        instance = this;
    }
    
    public static StatAllocationConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException("StatAllocationConfig not initialized");
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
        // 重置为默认值
        bonusPerPoint.clear();
        bonusPerPoint.putAll(DEFAULT_BONUS);
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                
                if (json.has("bonus_per_point")) {
                    JsonObject bonusObj = json.getAsJsonObject("bonus_per_point");
                    for (String key : bonusObj.keySet()) {
                        StatType type = StatType.fromId(key);
                        if (type != null && type.isAllocatable()) {
                            bonusPerPoint.put(type, bonusObj.get(key).getAsDouble());
                        }
                    }
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to load stat allocation config: {}", entry.getKey(), e);
            }
        }
        
        RoadWeaverRPG.LOGGER.info("Loaded stat allocation config with {} stat types", bonusPerPoint.size());
    }
    
    /**
     * 获取指定属性每点技能点的增益
     */
    public double getBonusPerPoint(StatType type) {
        return bonusPerPoint.getOrDefault(type, 0.0);
    }
    
    /**
     * 计算指定点数的总增益
     */
    public double calculateBonus(StatType type, int points) {
        return getBonusPerPoint(type) * points;
    }
}
