package net.shiroha233.roadweaverpg.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.client.gui.debug.EnvironmentDebugOverlay;
import org.lwjgl.glfw.GLFW;

/**
 * Forge端调试覆盖层处理器
 * 按键绑定：Z+V 切换环境调试信息显示
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugOverlayHandler {
    
    private static KeyMapping toggleKey;
    
    @Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            toggleKey = new KeyMapping(
                    "key.roadweaver_rpg.toggle_env_debug",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.roadweaver_rpg"
            );
            event.register(toggleKey);
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        // Z+V 组合键切换
        long window = mc.getWindow().getWindow();
        boolean zDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Z);
        if (zDown && toggleKey.consumeClick()) {
            EnvironmentDebugOverlay.toggle();
        }
        
        // 更新环境数据
        if (EnvironmentDebugOverlay.isEnabled()) {
            EnvironmentDebugOverlay.updateFromPlayer(mc.player);
        }
    }
    
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (EnvironmentDebugOverlay.isEnabled()) {
            EnvironmentDebugOverlay.render(event.getGuiGraphics(), event.getPartialTick());
        }
    }
}
