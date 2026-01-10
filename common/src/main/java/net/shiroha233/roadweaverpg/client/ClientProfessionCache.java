package net.shiroha233.roadweaverpg.client;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.profession.ProfessionDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端职业缓存
 * 存储从服务器同步过来的职业定义
 * 
 * 设计原理：
 * - 线程安全：使用ConcurrentHashMap
 * - 客户端专用：仅在客户端使用，存储同步数据
 */
public class ClientProfessionCache {
    
    private static final Map<ResourceLocation, ProfessionDefinition> professions = new ConcurrentHashMap<>();
    private static volatile ResourceLocation playerProfession = null;
    
    private ClientProfessionCache() {}
    
    /**
     * 更新职业定义缓存（从服务器同步）
     */
    public static void updateProfessions(Collection<ProfessionDefinition> definitions) {
        professions.clear();
        for (ProfessionDefinition def : definitions) {
            professions.put(def.getId(), def);
        }
    }
    
    /**
     * 获取职业定义
     */
    public static ProfessionDefinition getProfession(ResourceLocation id) {
        return professions.get(id);
    }
    
    /**
     * 获取所有职业
     */
    public static Collection<ProfessionDefinition> getAllProfessions() {
        return Collections.unmodifiableCollection(professions.values());
    }
    
    /**
     * 设置玩家当前职业
     */
    public static void setPlayerProfession(ResourceLocation profId) {
        playerProfession = profId;
    }
    
    /**
     * 获取玩家当前职业ID
     */
    public static ResourceLocation getPlayerProfession() {
        return playerProfession;
    }
    
    /**
     * 获取玩家职业定义
     */
    public static ProfessionDefinition getPlayerProfessionDef() {
        if (playerProfession == null) return null;
        return professions.get(playerProfession);
    }
    
    /**
     * 清空缓存（断开连接时调用）
     */
    public static void clear() {
        professions.clear();
        playerProfession = null;
    }
}
