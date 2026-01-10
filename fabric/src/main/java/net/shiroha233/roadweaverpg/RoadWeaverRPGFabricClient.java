package net.shiroha233.roadweaverpg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.shiroha233.roadweaverpg.client.CharacterKeyHandlerFabric;
import net.shiroha233.roadweaverpg.client.ClientNetworkHandlerFabric;
import net.shiroha233.roadweaverpg.client.DebugOverlayHandlerFabric;
import net.shiroha233.roadweaverpg.client.ModEntityRenderersFabric;
import net.shiroha233.roadweaverpg.client.ModItemPropertiesFabric;
import net.shiroha233.roadweaverpg.client.QuestScrollClientHandler;
import net.shiroha233.roadweaverpg.client.gui.hud.CoinNotificationRenderer;
import net.shiroha233.roadweaverpg.client.gui.hud.DamageIndicatorRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric 客户端入口
 */
@Environment(EnvType.CLIENT)
public class RoadWeaverRPGFabricClient implements ClientModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver_rpg");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing RoadWeaver RPG client for Fabric...");
        
        // 注册实体渲染器
        ModEntityRenderersFabric.register();
        
        // 注册物品属性（模型谓词）
        ModItemPropertiesFabric.register();
        
        // 注册客户端网络接收器
        ClientNetworkHandlerFabric.registerClientReceivers();
        
        // 注册委托书客户端交互
        QuestScrollClientHandler.register();
        
        // 注册调试覆盖层（Z+V切换）
        DebugOverlayHandlerFabric.register();
        
        // 注册角色界面按键（J键）
        CharacterKeyHandlerFabric.register();
        
        // 注册金币获取通知HUD
        registerCoinNotificationHud();
        
        // 注册伤害飘字渲染
        registerDamageIndicatorRenderer();
        
        LOGGER.info("RoadWeaver RPG client initialized!");
    }
    
    /**
     * 注册金币获取通知HUD渲染
     */
    private void registerCoinNotificationHud() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.options.hideGui) {
                CoinNotificationRenderer.render(graphics, 
                        mc.getWindow().getGuiScaledWidth(), 
                        mc.getWindow().getGuiScaledHeight());
            }
        });
    }
    
    /**
     * 注册伤害飘字渲染
     */
    private void registerDamageIndicatorRenderer() {
        // 世界渲染事件
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            
            DamageIndicatorRenderer.render(
                    context.matrixStack(),
                    mc.renderBuffers().bufferSource(),
                    context.camera(),
                    context.tickDelta()
            );
        });
        
        // 客户端tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !client.isPaused()) {
                DamageIndicatorRenderer.tick();
            }
        });
    }
}
