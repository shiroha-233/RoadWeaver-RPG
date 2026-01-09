package net.shiroha233.roadweaverpg.forge.loot;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Forge战利品修改器注册
 * 
 * 注意：全局战利品修改器已迁移到数据驱动系统
 * 使用CoinLootConfigManager从JSON加载掉落配置
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = RoadWeaverRPG.MOD_ID)
public final class ModLootModifiersForge {
    
    /**
     * 保留此方法以保持向后兼容性
     * 实际的战利品修改逻辑已移至CoinLootConfigManager
     */
    public static void register(IEventBus modEventBus) {
        // 不再需要注册全局战利品修改器
        // 战利品注入通过CoinLootConfigManager的数据驱动系统实现
    }
}
