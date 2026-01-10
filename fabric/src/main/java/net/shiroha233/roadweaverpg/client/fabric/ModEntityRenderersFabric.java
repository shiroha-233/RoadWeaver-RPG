package net.shiroha233.roadweaverpg.client.fabric;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.shiroha233.roadweaverpg.entity.fabric.ModEntitiesFabric;

/**
 * Fabric 端实体渲染器注册
 */
@Environment(EnvType.CLIENT)
public class ModEntityRenderersFabric {
    
    /**
     * 注册实体渲染器
     * 使用 TouhouLittleMaid 的女仆渲染器
     */
    public static void register() {
        // 公会女仆使用原版女仆渲染器
        EntityRendererRegistry.register(ModEntitiesFabric.GUILD_MAID, context -> 
            new EntityMaidRenderer(context));
        
        // 商店女仆使用相同的渲染器
        EntityRendererRegistry.register(ModEntitiesFabric.SHOP_MAID, context -> 
            new EntityMaidRenderer(context));
    }
}
