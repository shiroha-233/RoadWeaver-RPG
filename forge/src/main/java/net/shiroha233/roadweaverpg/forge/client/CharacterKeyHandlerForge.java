package net.shiroha233.roadweaverpg.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.client.gui.character.CharacterScreen;
import net.shiroha233.roadweaverpg.forge.network.NetworkHandlerForge;

/**
 * Forge端角色界面按键输入处理
 * 按键绑定：J 打开角色界面
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CharacterKeyHandlerForge {
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        // J键打开角色界面
        if (ModKeyMappingsForge.CHARACTER_KEY != null && ModKeyMappingsForge.CHARACTER_KEY.consumeClick()) {
            openCharacterScreen();
        }
    }
    
    /**
     * 打开角色界面并设置回调
     */
    private static void openCharacterScreen() {
        // 设置加点回调：发送网络包到服务端
        CharacterScreen.setOnAllocatePoint(statType -> {
            NetworkHandlerForge.sendAllocateStatPoint(statType);
        });
        
        // 设置减点回调：发送网络包到服务端
        CharacterScreen.setOnDeallocatePoint(statType -> {
            NetworkHandlerForge.sendDeallocateStatPoint(statType);
        });
        
        // 设置重置回调：发送网络包到服务端
        CharacterScreen.setOnResetAllocation(() -> {
            NetworkHandlerForge.sendResetStatAllocation();
        });
        
        Minecraft.getInstance().setScreen(new CharacterScreen());
    }
}
