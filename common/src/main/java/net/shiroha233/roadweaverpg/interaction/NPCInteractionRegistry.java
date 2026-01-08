package net.shiroha233.roadweaverpg.interaction;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.INPCEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NPC交互注册表
 * 职责：管理所有NPC类型的交互入口
 * 原理：注册表模式，支持动态注册和数据驱动
 */
public final class NPCInteractionRegistry {
    
    private NPCInteractionRegistry() {}
    
    // NPC类型 -> 交互入口列表
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<NPCInteractionEntry>> 
            ENTRIES = new ConcurrentHashMap<>();
    
    // 全局交互入口（所有NPC共享）
    private static final CopyOnWriteArrayList<NPCInteractionEntry> GLOBAL_ENTRIES = new CopyOnWriteArrayList<>();
    
    /**
     * 初始化默认交互入口
     */
    public static void init() {
        // 公会女仆的交互入口
        registerEntry(INPCEntity.NPCType.GUILD_MAID.getId(), NPCInteractionEntry.dialog(
                "chat",
                "gui.roadweaver_rpg.interaction.chat",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "guild_maid/greeting"),
                0
        ));
        
        registerEntry(INPCEntity.NPCType.GUILD_MAID.getId(), NPCInteractionEntry.function(
                "show_quests",
                "gui.roadweaver_rpg.dialog.show_quests",
                NPCInteractionEntry.ICON_QUEST,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "show_quests"),
                10
        ));
        
        registerEntry(INPCEntity.NPCType.GUILD_MAID.getId(), NPCInteractionEntry.function(
                "complete_quest",
                "gui.roadweaver_rpg.dialog.complete_quest",
                NPCInteractionEntry.ICON_QUEST,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "complete_quest"),
                20
        ));
        
        registerEntry(INPCEntity.NPCType.GUILD_MAID.getId(), NPCInteractionEntry.function(
                "retrieve_scroll",
                "gui.roadweaver_rpg.dialog.retrieve_scroll",
                NPCInteractionEntry.ICON_QUEST,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "retrieve_scroll"),
                30
        ));
        
        registerEntry(INPCEntity.NPCType.GUILD_MAID.getId(), NPCInteractionEntry.function(
                "view_reputation",
                "gui.roadweaver_rpg.dialog.view_reputation",
                NPCInteractionEntry.ICON_REPUTATION,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "view_reputation"),
                40
        ));
        
        // 查看冒险等级入口
        registerEntry(INPCEntity.NPCType.GUILD_MAID.getId(), NPCInteractionEntry.function(
                "view_adventure_level",
                "gui.roadweaver_rpg.dialog.view_adventure_level",
                NPCInteractionEntry.ICON_REPUTATION,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "view_adventure_level"),
                45
        ));
        
        // 商店女仆的交互入口
        registerEntry(INPCEntity.NPCType.SHOP_MAID.getId(), NPCInteractionEntry.dialog(
                "chat",
                "gui.roadweaver_rpg.interaction.chat",
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "shop_maid/greeting"),
                0
        ));
        
        registerEntry(INPCEntity.NPCType.SHOP_MAID.getId(), NPCInteractionEntry.function(
                "open_shop",
                "gui.roadweaver_rpg.shop_dialog.open_shop",
                NPCInteractionEntry.ICON_SHOP,
                new ResourceLocation(RoadWeaverRPG.MOD_ID, "open_shop"),
                10
        ));
        
        RoadWeaverRPG.LOGGER.info("已注册 NPC 交互入口");
    }
    
    /**
     * 注册交互入口
     */
    public static void registerEntry(String npcType, NPCInteractionEntry entry) {
        ENTRIES.computeIfAbsent(npcType, k -> new CopyOnWriteArrayList<>()).add(entry);
    }
    
    /**
     * 注册全局交互入口
     */
    public static void registerGlobalEntry(NPCInteractionEntry entry) {
        GLOBAL_ENTRIES.add(entry);
    }
    
    /**
     * 获取NPC的所有交互入口（已排序）
     */
    public static List<NPCInteractionEntry> getEntries(String npcType) {
        List<NPCInteractionEntry> result = new ArrayList<>();
        
        // 添加NPC特定入口
        CopyOnWriteArrayList<NPCInteractionEntry> npcEntries = ENTRIES.get(npcType);
        if (npcEntries != null) {
            result.addAll(npcEntries);
        }
        
        // 添加全局入口
        result.addAll(GLOBAL_ENTRIES);
        
        // 按优先级排序
        result.sort(Comparator.comparingInt(NPCInteractionEntry::priority));
        
        return result;
    }
    
    /**
     * 获取NPC的所有交互入口（通过NPC实体）
     */
    public static List<NPCInteractionEntry> getEntries(INPCEntity npc) {
        return getEntries(npc.getNPCType().getId());
    }
    
    /**
     * 移除交互入口
     */
    public static void removeEntry(String npcType, String entryId) {
        CopyOnWriteArrayList<NPCInteractionEntry> entries = ENTRIES.get(npcType);
        if (entries != null) {
            entries.removeIf(e -> e.id().equals(entryId));
        }
    }
    
    /**
     * 移除全局交互入口
     */
    public static void removeGlobalEntry(String entryId) {
        GLOBAL_ENTRIES.removeIf(e -> e.id().equals(entryId));
    }
    
    /**
     * 清理未使用的NPC类型（防止内存泄漏）
     */
    public static void cleanupUnusedTypes(Set<String> activeNpcTypes) {
        ENTRIES.keySet().removeIf(type -> !activeNpcTypes.contains(type));
    }
    
    /**
     * 清空所有入口
     */
    public static void clear() {
        ENTRIES.clear();
        GLOBAL_ENTRIES.clear();
    }
    
    /**
     * 获取已注册的NPC类型数量
     */
    public static int getRegisteredTypeCount() {
        return ENTRIES.size();
    }
    
    /**
     * 获取全局入口数量
     */
    public static int getGlobalEntryCount() {
        return GLOBAL_ENTRIES.size();
    }
}
