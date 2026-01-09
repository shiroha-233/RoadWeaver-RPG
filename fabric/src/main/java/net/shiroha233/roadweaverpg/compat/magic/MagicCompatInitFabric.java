package net.shiroha233.roadweaverpg.compat.magic;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Fabric 平台魔法模组兼容初始化
 */
public final class MagicCompatInitFabric {
    
    private MagicCompatInitFabric() {}
    
    /**
     * 初始化所有魔法模组兼容
     */
    public static void init() {
        RoadWeaverRPG.LOGGER.info("Initializing magic mod compatibility (Fabric)...");
        
        // 注册魔法模组兼容（这些模组主要是 Forge 版本，Fabric 版本可能不存在）
        net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry.register(new IronsSpellsCompatFabric());
        net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry.register(new GoetyCompatFabric());
        net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry.register(new ArsNouveauCompatFabric());
        
        RoadWeaverRPG.LOGGER.info("Magic mod compatibility initialized");
    }
}
