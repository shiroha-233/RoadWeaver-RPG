package net.shiroha233.roadweaverpg.entity.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 公会女仆 NPC 实体
 * 职责：处理委托相关的交互
 */
public class GuildMaidEntity extends BaseNPCEntity implements NPCBehavior.Dialogable {
    
    private static final String MODEL_ID = "geckolib:winefox";
    
    public GuildMaidEntity(EntityType<? extends GuildMaidEntity> type, Level level) {
        super(type, level);
    }
    
    @Override
    public NPCType getNPCType() {
        return NPCType.GUILD_MAID;
    }
    
    @Override
    public String getModelId() {
        return MODEL_ID;
    }
    
    @Override
    public void handlePlayerInteraction(ServerPlayer player) {
        openDialog(player);
    }
    
    @Override
    public void openDialog(ServerPlayer player) {
        // 由平台特定代码实现（Fabric/Forge）
        openDialogForPlayer(player);
    }
    
    @Override
    public List<NPCBehavior.DialogOption> getDialogOptions(ServerPlayer player) {
        List<NPCBehavior.DialogOption> options = new ArrayList<>();
        options.add(new NPCBehavior.DialogOption("show_quests", 
                net.minecraft.network.chat.Component.translatable("gui.roadweaver_rpg.dialog.show_quests")));
        options.add(new NPCBehavior.DialogOption("complete_quest", 
                net.minecraft.network.chat.Component.translatable("gui.roadweaver_rpg.dialog.complete_quest")));
        options.add(new NPCBehavior.DialogOption("view_reputation", 
                net.minecraft.network.chat.Component.translatable("gui.roadweaver_rpg.dialog.view_reputation")));
        options.add(new NPCBehavior.DialogOption("retrieve_scroll", 
                net.minecraft.network.chat.Component.translatable("gui.roadweaver_rpg.dialog.retrieve_scroll")));
        return options;
    }
    
    /**
     * 为玩家打开对话界面（由平台实现）
     */
    protected void openDialogForPlayer(ServerPlayer player) {
        // 由子类或平台特定代码实现
    }
}
