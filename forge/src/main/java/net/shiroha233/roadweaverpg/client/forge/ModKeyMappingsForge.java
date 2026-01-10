package net.shiroha233.roadweaverpg.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import org.lwjgl.glfw.GLFW;

/**
 * Forge端统一按键管理器
 * 所有按键在此统一注册，避免重复注册问题
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMappingsForge {
    
    // 角色界面按键
    public static KeyMapping CHARACTER_KEY;
    
    // 环境调试按键
    public static KeyMapping DEBUG_TOGGLE_KEY;
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册角色界面按键
        CHARACTER_KEY = new KeyMapping(
                "key.roadweaver_rpg.open_character",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.categories.roadweaver_rpg"
        );
        event.register(CHARACTER_KEY);
        
        // 注册环境调试按键
        DEBUG_TOGGLE_KEY = new KeyMapping(
                "key.roadweaver_rpg.toggle_env_debug",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.roadweaver_rpg"
        );
        event.register(DEBUG_TOGGLE_KEY);
    }
}
