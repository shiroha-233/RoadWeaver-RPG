package net.shiroha233.roadweaverpg.entity.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 商店女仆 NPC 实体
 * 职责：处理商店相关的交互
 */
public class ShopMaidEntity extends BaseNPCEntity implements NPCBehavior.Dialogable {
    
    private static final String MODEL_ID = "geckolib:winefox";
    
    public ShopMaidEntity(EntityType<? extends ShopMaidEntity> type, Level level) {
        super(type, level);
    }
    
    @Override
    public NPCType getNPCType() {
        return NPCType.SHOP_MAID;
    }
    
    @Override
    public String getModelId() {
        return MODEL_ID;
    }
    
    @Override
    public void handlePlayerInteraction(ServerPlayer player) {
        // 播放打招呼行为
        playAction(new net.minecraft.resources.ResourceLocation(
                net.shiroha233.roadweaverpg.RoadWeaverRPG.MOD_ID, "greeting"));
        
        // 打开交互菜单（由平台特定代码实现）
        openInteractionMenu(player);
    }
    
    @Override
    public void openDialog(ServerPlayer player) {
        // 由交互菜单中的"对话"按钮触发
        openShopDialogForPlayer(player);
    }
    
    @Override
    public List<NPCBehavior.DialogOption> getDialogOptions(ServerPlayer player) {
        List<NPCBehavior.DialogOption> options = new ArrayList<>();
        options.add(new NPCBehavior.DialogOption("open_shop", 
                net.minecraft.network.chat.Component.translatable("gui.roadweaver_rpg.shop_dialog.open_shop")));
        return options;
    }
    
    /**
     * 打开商店时播放欢迎行为
     */
    public void onShopOpened() {
        playAction(new net.minecraft.resources.ResourceLocation(
                net.shiroha233.roadweaverpg.RoadWeaverRPG.MOD_ID, "shop_buy"));
    }
    
    /**
     * 购买成功时播放行为
     */
    public void onPurchaseSuccess() {
        playAction(new net.minecraft.resources.ResourceLocation(
                net.shiroha233.roadweaverpg.RoadWeaverRPG.MOD_ID, "happy"));
    }
    
    /**
     * 金币不足时播放行为
     */
    public void onInsufficientFunds() {
        playAction(new net.minecraft.resources.ResourceLocation(
                net.shiroha233.roadweaverpg.RoadWeaverRPG.MOD_ID, "sad"));
    }
    
    /**
     * 打开交互菜单（由平台实现）
     */
    protected void openInteractionMenu(ServerPlayer player) {
        // 由子类或平台特定代码实现
    }
    
    /**
     * 为玩家打开商店对话界面（回退方案，由平台实现）
     */
    protected void openShopDialogForPlayer(ServerPlayer player) {
        // 由子类或平台特定代码实现
    }
}
