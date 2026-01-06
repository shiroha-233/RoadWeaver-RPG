package net.shiroha233.roadweaverpg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.shiroha233.roadweaverpg.client.ClientNetworkHandlerFabric;
import net.shiroha233.roadweaverpg.client.ModEntityRenderersFabric;
import net.shiroha233.roadweaverpg.client.ModItemPropertiesFabric;
import net.shiroha233.roadweaverpg.client.QuestScrollClientHandler;
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
        
        LOGGER.info("RoadWeaver RPG client initialized!");
    }
}
