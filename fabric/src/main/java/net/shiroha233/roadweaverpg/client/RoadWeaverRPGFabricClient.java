package net.shiroha233.roadweaverpg.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.shiroha233.roadweaverpg.client.gui.hud.CoinNotificationRenderer;

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
    }
}
