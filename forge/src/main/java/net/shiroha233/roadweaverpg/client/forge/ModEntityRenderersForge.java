package net.shiroha233.roadweaverpg.client.forge;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.forge.ModEntitiesForge;

/**
 * Forge 端实体渲染器注册
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderersForge {
    
    /**
     * 注册实体渲染器
     */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 公会女仆使用原版女仆渲染器
        event.registerEntityRenderer(ModEntitiesForge.GUILD_MAID.get(), context -> 
            new EntityMaidRenderer(context));
        
        // 商店女仆使用相同的渲染器
        event.registerEntityRenderer(ModEntitiesForge.SHOP_MAID.get(), context -> 
            new EntityMaidRenderer(context));
    }
}
