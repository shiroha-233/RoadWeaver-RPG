package net.shiroha233.roadweaverpg.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.client.gui.debug.EnvironmentDebugOverlay;
import org.lwjgl.glfw.GLFW;

/**
 * Forge端调试覆盖层处理器
 * 按键绑定：Z+V 切换环境调试信息显示
 * 
 * 按键注册在 ModKeyMappingsForge 中统一管理
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugOverlayHandler {
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        // Z+V 组合键切换
        long window = mc.getWindow().getWindow();
        boolean zDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Z);
        if (zDown && ModKeyMappingsForge.DEBUG_TOGGLE_KEY != null && ModKeyMappingsForge.DEBUG_TOGGLE_KEY.consumeClick()) {
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
