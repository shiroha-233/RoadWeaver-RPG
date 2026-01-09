package net.shiroha233.roadweaverpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.shiroha233.roadweaverpg.client.gui.character.CharacterScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric端角色界面按键处理
 * 按键绑定：J 打开角色界面
 */
@Environment(EnvType.CLIENT)
public class CharacterKeyHandlerFabric {
    
    private static KeyMapping characterKey;
    
    public static void register() {
        // 注册按键 J
        characterKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.roadweaver_rpg.open_character",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "key.categories.roadweaver_rpg"
        ));
        
        // 客户端Tick事件处理按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            // J键打开角色界面
            if (characterKey.consumeClick() && client.screen == null) {
                // 设置技能点分配回调
                CharacterScreen.setOnAllocatePoint(ClientNetworkHandlerFabric::sendAllocateStatPoint);
                CharacterScreen.setOnDeallocatePoint(ClientNetworkHandlerFabric::sendDeallocateStatPoint);
                CharacterScreen.setOnResetAllocation(ClientNetworkHandlerFabric::sendResetStatAllocation);
                Minecraft.getInstance().setScreen(new CharacterScreen());
            }
        });
    }
}
