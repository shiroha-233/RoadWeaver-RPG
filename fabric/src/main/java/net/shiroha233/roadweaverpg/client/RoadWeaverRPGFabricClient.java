package net.shiroha233.roadweaverpg.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Fabric 客户端入口
 */
@Environment(EnvType.CLIENT)
public class RoadWeaverRPGFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        ModEntityRenderersFabric.register();
    }
}
