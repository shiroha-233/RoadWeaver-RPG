package net.shiroha233.roadweaverpg.forge.compat.magic;

import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.compat.magic.MagicModCompatRegistry;

/**
 * Forge 平台魔法模组兼容初始化
 */
public final class MagicCompatInitForge {
    
    private MagicCompatInitForge() {}
    
    /**
     * 初始化所有魔法模组兼容
     * 应在模组初始化阶段调用
     */
    public static void init() {
        RoadWeaverRPG.LOGGER.info("Initializing magic mod compatibility (Forge)...");
        
        // 注册 Iron's Spells 兼容
        MagicModCompatRegistry.register(new IronsSpellsCompatForge());
        
        // 注册 Goety-2 兼容
        MagicModCompatRegistry.register(new GoetyCompatForge());
        
        // 注册 Ars Nouveau 兼容
        MagicModCompatRegistry.register(new ArsNouveauCompatForge());
        
        RoadWeaverRPG.LOGGER.info("Magic mod compatibility initialized");
    }
}
