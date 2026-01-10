package net.shiroha233.roadweaverpg.client.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.shiroha233.roadweaverpg.client.gui.hud.CoinNotificationRenderer;
import net.shiroha233.roadweaverpg.client.gui.hud.DamageIndicatorRenderer;

/**
 * Fabric 客户端入口
 */
@Environment(EnvType.CLIENT)
public class RoadWeaverRPGFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        ModEntityRenderersFabric.register();
        
        // 注册客户端网络接收器
        ClientNetworkHandlerFabric.registerClientReceivers();
        
        // 注册HUD渲染（金币获取提示）
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && !mc.options.hideGui) {
                CoinNotificationRenderer.render(graphics, mc.getWindow().getGuiScaledWidth(), 
                        mc.getWindow().getGuiScaledHeight());
            }
        });
        
        // 注册世界渲染事件（伤害飘字）
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                DamageIndicatorRenderer.render(
                        context.matrixStack(),
                        context.consumers(),
                        context.camera(),
                        context.tickDelta()
                );
            }
        });
        
        // 注册客户端Tick事件（更新伤害飘字）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !client.isPaused()) {
                DamageIndicatorRenderer.tick();
            }
        });
    }
}
