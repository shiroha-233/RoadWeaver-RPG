package net.shiroha233.roadweaverpg.entity;

import net.minecraft.resources.ResourceLocation;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * 模组实体常量定义
 * 实际注册在各平台模块中完成
 */
public class ModEntities {
    
    // 实体 ID
    public static final String GUILD_MAID_ID = "guild_maid";
    public static final String SHOP_MAID_ID = "shop_maid";
    
    // 资源位置
    public static final ResourceLocation GUILD_MAID_LOCATION = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, GUILD_MAID_ID);
    public static final ResourceLocation SHOP_MAID_LOCATION = 
            new ResourceLocation(RoadWeaverRPG.MOD_ID, SHOP_MAID_ID);
    
    private ModEntities() {}
}
