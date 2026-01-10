package net.shiroha233.roadweaverpg.profession;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.data.PlayerQuestData;
import net.shiroha233.roadweaverpg.data.QuestDataAccessor;
import net.shiroha233.roadweaverpg.stats.RpgStatsService;
import net.shiroha233.roadweaverpg.stats.StatAllocationService;
import net.shiroha233.roadweaverpg.stats.StatEffectService;
import net.shiroha233.roadweaverpg.stats.StatType;

import java.util.function.BiConsumer;

/**
 * 职业数据服务 - 处理职业选择、转职和属性应用
 * 
 * 设计原理：
 * - 单一职责：专注于职业相关的业务逻辑
 * - 依赖倒置：通过回调与网络层解耦
 * - 线程安全：关键操作使用synchronized
 */
public class ProfessionDataService {
    
    private static volatile ProfessionDataService instance;
    private static final Object LOCK = new Object();
    
    private final QuestDataAccessor dataAccessor;
    
    // 同步回调
    private BiConsumer<ServerPlayer, ResourceLocation> onSyncProfession;
    
    private ProfessionDataService() {
        this.dataAccessor = QuestDataAccessor.getInstance();
    }
    
    public static ProfessionDataService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ProfessionDataService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置同步回调
     */
    public void setOnSyncProfession(BiConsumer<ServerPlayer, ResourceLocation> callback) {
        this.onSyncProfession = callback;
    }
    
    /**
     * 选择职业（首次选择）
     * 
     * @return 是否成功
     */
    public synchronized boolean selectProfession(ServerPlayer player, ResourceLocation professionId) {
        if (!ProfessionManager.isInitialized()) {
            RoadWeaverRPG.LOGGER.warn("ProfessionManager not initialized");
            return false;
        }
        
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        
        // 检查是否已有职业
        if (data.getProfessionId() != null) {
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.profession_already_selected"));
            return false;
        }
        
        ProfessionDefinition profession = ProfessionManager.getInstance().getProfession(professionId);
        if (profession == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.profession_not_found"));
            return false;
        }
        
        // 检查解锁条件
        if (!checkRequirements(player, profession)) {
            return false;
        }
        
        // 应用职业
        applyProfession(player, data, profession, true);
        
        player.sendSystemMessage(Component.translatable(
                "message.roadweaver_rpg.profession_selected", profession.getName()));
        
        RoadWeaverRPG.LOGGER.info("Player {} selected profession: {}", 
                player.getName().getString(), professionId);
        
        return true;
    }

    /**
     * 转职（更换职业）
     * 
     * @param resetStats 是否重置技能点
     * @return 是否成功
     */
    public synchronized boolean changeProfession(ServerPlayer player, ResourceLocation newProfessionId, 
                                                  boolean resetStats) {
        if (!ProfessionManager.isInitialized()) {
            return false;
        }
        
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        ResourceLocation currentProfId = data.getProfessionId();
        
        if (currentProfId != null && currentProfId.equals(newProfessionId)) {
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.profession_same"));
            return false;
        }
        
        ProfessionDefinition newProfession = ProfessionManager.getInstance().getProfession(newProfessionId);
        if (newProfession == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.roadweaver_rpg.profession_not_found"));
            return false;
        }
        
        // 检查解锁条件
        if (!checkRequirements(player, newProfession)) {
            return false;
        }
        
        // 移除旧职业效果
        StatEffectService.removeAllModifiers(player);
        
        // 重置技能点（如果需要）
        if (resetStats) {
            data.getStatAllocationData().resetAllocation();
        }
        
        // 应用新职业（转职不发放初始物品）
        applyProfession(player, data, newProfession, false);
        
        player.sendSystemMessage(Component.translatable(
                "message.roadweaver_rpg.profession_changed", newProfession.getName()));
        
        RoadWeaverRPG.LOGGER.info("Player {} changed profession to: {}", 
                player.getName().getString(), newProfessionId);
        
        return true;
    }
    
    /**
     * 检查职业解锁条件
     */
    private boolean checkRequirements(ServerPlayer player, ProfessionDefinition profession) {
        // 移除冒险等级限制，允许所有玩家注册冒险家
        // int adventureLevel = AdventureDataService.getInstance().getAdventureLevel(player);
        // if (adventureLevel < profession.getMinAdventureLevel()) {
        //     player.sendSystemMessage(Component.translatable(
        //             "message.roadweaver_rpg.profession_level_required", 
        //             profession.getMinAdventureLevel()));
        //     return false;
        // }
        
        // 检查前置职业
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        for (ResourceLocation reqProfId : profession.getRequiredProfessions()) {
            if (!data.hasCompletedProfession(reqProfId)) {
                ProfessionDefinition reqProf = ProfessionManager.getInstance().getProfession(reqProfId);
                Component reqName = reqProf != null ? reqProf.getName() : Component.literal(reqProfId.toString());
                player.sendSystemMessage(Component.translatable(
                        "message.roadweaver_rpg.profession_prerequisite_required", reqName));
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 应用职业
     */
    private void applyProfession(ServerPlayer player, PlayerQuestData data, 
                                  ProfessionDefinition profession, boolean grantItems) {
        // 保存职业ID
        data.setProfessionId(profession.getId());
        
        // 更新可加点属性
        data.getStatAllocationData().setUnlockedStats(profession.getUnlockedStats());
        
        // 应用基础属性
        applyBaseStats(player, profession);
        
        // 应用成长属性（基于当前等级）
        int playerLevel = data.getPlayerLevel();
        applyGrowthStats(player, profession, playerLevel);
        
        // 重新应用技能点分配的属性
        StatAllocationService.getInstance().refreshAllStats(player);
        
        // 发放初始物品
        if (grantItems) {
            grantStartingItems(player, profession);
        }
        
        // 标记数据已修改
        dataAccessor.markDirty(player);
        
        // 同步到客户端
        syncToClient(player);
        
        // 同步RPG属性
        RpgStatsService.getInstance().syncRpgStats(player);
    }

    /**
     * 应用职业基础属性
     */
    private void applyBaseStats(ServerPlayer player, ProfessionDefinition profession) {
        RoadWeaverRPG.LOGGER.info("Applying base stats for profession: {} to player: {}", 
                profession.getId(), player.getName().getString());
        
        for (StatType type : StatType.values()) {
            double baseValue = profession.getBaseStat(type);
            
            switch (type) {
                case MAX_HEALTH -> {
                    double bonus = baseValue - 20.0;
                    RoadWeaverRPG.LOGGER.debug("Applying MAX_HEALTH: base={}, bonus={}", baseValue, bonus);
                    StatEffectService.applyProfessionMaxHealth(player, bonus);
                }
                case ATTACK -> {
                    double bonus = baseValue - 1.0;
                    RoadWeaverRPG.LOGGER.debug("Applying ATTACK: base={}, bonus={}", baseValue, bonus);
                    StatEffectService.applyProfessionAttack(player, bonus);
                }
                case DEFENSE -> StatEffectService.applyProfessionDefense(player, baseValue);
                case MAGIC_DEFENSE -> StatEffectService.applyProfessionMagicDefense(player, baseValue);
                case MOVE_SPEED -> {
                    if (baseValue != 100.0) {
                        StatEffectService.applyProfessionMoveSpeed(player, baseValue - 100.0);
                    }
                }
                case MAX_MANA -> StatEffectService.applyProfessionMaxMana(player, baseValue - 100.0);
                case MAGIC_ATTACK -> StatEffectService.applyProfessionSpellPower(player, baseValue - 1.0);
                case MANA_REGEN -> StatEffectService.applyProfessionManaRegen(player, baseValue - 1.0);
                default -> {} // 其他属性通过RPG系统计算
            }
        }
        
        // 验证属性是否正确应用
        double finalAttack = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        RoadWeaverRPG.LOGGER.info("After applying profession stats - Attack: {}", finalAttack);
    }
    
    /**
     * 应用职业成长属性
     */
    private void applyGrowthStats(ServerPlayer player, ProfessionDefinition profession, int level) {
        if (level <= 1) return;
        
        int growthLevels = level - 1;
        
        for (StatType type : StatType.values()) {
            double growth = profession.getGrowth(type);
            if (growth == 0) continue;
            
            double totalGrowth = growth * growthLevels;
            
            switch (type) {
                case MAX_HEALTH -> StatEffectService.applyGrowthMaxHealth(player, totalGrowth);
                case ATTACK -> StatEffectService.applyGrowthAttack(player, totalGrowth);
                case DEFENSE -> StatEffectService.applyGrowthDefense(player, totalGrowth);
                case MAGIC_DEFENSE -> StatEffectService.applyGrowthMagicDefense(player, totalGrowth);
                case MAX_MANA -> StatEffectService.applyGrowthMaxMana(player, totalGrowth);
                case MAGIC_ATTACK -> StatEffectService.applyGrowthSpellPower(player, totalGrowth);
                default -> {}
            }
        }
    }
    
    /**
     * 发放初始物品
     */
    private void grantStartingItems(ServerPlayer player, ProfessionDefinition profession) {
        for (ProfessionDefinition.StartingItem startingItem : profession.getStartingItems()) {
            try {
                Item item = BuiltInRegistries.ITEM.get(startingItem.itemId());
                if (item != null) {
                    ItemStack stack = new ItemStack(item, startingItem.count());
                    // TODO: 支持NBT
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            } catch (Exception e) {
                RoadWeaverRPG.LOGGER.error("Failed to grant starting item: {}", startingItem.itemId(), e);
            }
        }
    }
    
    /**
     * 获取玩家当前职业
     */
    public ProfessionDefinition getPlayerProfession(ServerPlayer player) {
        if (!ProfessionManager.isInitialized()) return null;
        
        ResourceLocation profId = dataAccessor.getPlayerData(player).getProfessionId();
        if (profId == null) return null;
        
        return ProfessionManager.getInstance().getProfession(profId);
    }
    
    /**
     * 获取玩家职业ID
     */
    public ResourceLocation getPlayerProfessionId(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getProfessionId();
    }
    
    /**
     * 检查玩家是否已选择职业
     */
    public boolean hasProfession(ServerPlayer player) {
        return dataAccessor.getPlayerData(player).getProfessionId() != null;
    }
    
    /**
     * 刷新玩家职业效果（登录/重生时调用）
     */
    public void refreshProfessionEffects(ServerPlayer player) {
        ProfessionDefinition profession = getPlayerProfession(player);
        if (profession == null) return;
        
        PlayerQuestData data = dataAccessor.getPlayerData(player);
        
        // 重新应用基础属性
        applyBaseStats(player, profession);
        
        // 重新应用成长属性
        applyGrowthStats(player, profession, data.getPlayerLevel());
        
        // 同步RPG属性
        RpgStatsService.getInstance().syncRpgStats(player);
    }
    
    /**
     * 玩家升级时更新成长属性
     */
    public void onPlayerLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        ProfessionDefinition profession = getPlayerProfession(player);
        if (profession == null) return;
        
        // 计算新增的成长属性
        int newGrowthLevels = newLevel - oldLevel;
        
        for (StatType type : StatType.values()) {
            double growth = profession.getGrowth(type);
            if (growth == 0) continue;
            
            double additionalGrowth = growth * newGrowthLevels;
            
            switch (type) {
                case MAX_HEALTH -> StatEffectService.addGrowthMaxHealth(player, additionalGrowth);
                case ATTACK -> StatEffectService.addGrowthAttack(player, additionalGrowth);
                case DEFENSE -> StatEffectService.addGrowthDefense(player, additionalGrowth);
                case MAGIC_DEFENSE -> StatEffectService.addGrowthMagicDefense(player, additionalGrowth);
                case MAX_MANA -> StatEffectService.addGrowthMaxMana(player, additionalGrowth);
                case MAGIC_ATTACK -> StatEffectService.addGrowthSpellPower(player, additionalGrowth);
                default -> {}
            }
        }
    }
    
    /**
     * 获取职业的技能点加成倍率
     */
    public double getStatBonusMultiplier(ServerPlayer player, StatType type) {
        ProfessionDefinition profession = getPlayerProfession(player);
        if (profession == null) return 1.0;
        return profession.getBonusMultiplier(type);
    }
    
    /**
     * 同步职业数据到客户端
     */
    public void syncToClient(ServerPlayer player) {
        if (onSyncProfession != null) {
            ResourceLocation profId = dataAccessor.getPlayerData(player).getProfessionId();
            onSyncProfession.accept(player, profId);
        }
        
        // 同步属性分配数据（职业会影响可加点属性）
        StatAllocationService.getInstance().syncToClient(player);
    }
}
