package net.shiroha233.roadweaverpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.shiroha233.roadweaverpg.client.gui.debug.EnvironmentDebugOverlay;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric端调试覆盖层处理器
 * 按键绑定：Z+V 切换环境调试信息显示
 */
@Environment(EnvType.CLIENT)
public class DebugOverlayHandlerFabric {
    
    private static KeyMapping toggleKey;
    
    public static void register() {
        // 注册按键
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.roadweaver_rpg.toggle_env_debug",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.roadweaver_rpg"
        ));
        
        // 客户端Tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.screen != null) return;
            
            // Z+V 组合键切换
            long window = client.getWindow().getWindow();
            boolean zDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Z);
            if (zDown && toggleKey.consumeClick()) {
                EnvironmentDebugOverlay.toggle();
            }
            
            // 更新环境数据
            if (EnvironmentDebugOverlay.isEnabled()) {
                EnvironmentDebugOverlay.updateFromPlayer(client.player);
            }
        });
        
        // HUD渲染事件
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (EnvironmentDebugOverlay.isEnabled()) {
                EnvironmentDebugOverlay.render(graphics, tickDelta);
            }
        });
    }
}
