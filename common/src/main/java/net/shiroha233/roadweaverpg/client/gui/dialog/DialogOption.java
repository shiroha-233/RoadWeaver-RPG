package net.shiroha233.roadweaverpg.client.gui.dialog;

import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaverpg.network.packet.ui.DialogResponsePacket;

/**
 * 对话选项数据类
 * 职责：封装单个对话选项的数据
 */
public class DialogOption {
    
    private final Component text;
    private final DialogResponsePacket.DialogOption action;
    private final boolean isSecondary;
    
    public DialogOption(Component text, DialogResponsePacket.DialogOption action) {
        this(text, action, false);
    }
    
    public DialogOption(Component text, DialogResponsePacket.DialogOption action, boolean isSecondary) {
        this.text = text;
        this.action = action;
        this.isSecondary = isSecondary;
    }
    
    public Component getText() {
        return text;
    }
    
    public DialogResponsePacket.DialogOption getAction() {
        return action;
    }
    
    public boolean isSecondary() {
        return isSecondary;
    }
    
    public static final String KEY_SHOW_QUESTS = "gui.roadweaver_rpg.dialog.show_quests";
    public static final String KEY_COMPLETE_QUEST = "gui.roadweaver_rpg.dialog.complete_quest";
    public static final String KEY_VIEW_REPUTATION = "gui.roadweaver_rpg.dialog.view_reputation";
    public static final String KEY_RETRIEVE_SCROLL = "gui.roadweaver_rpg.dialog.retrieve_scroll";
    public static final String KEY_CANCEL = "gui.roadweaver_rpg.dialog.cancel";
    
    public static DialogOption showQuests() {
        return new DialogOption(
                Component.translatable(KEY_SHOW_QUESTS),
                DialogResponsePacket.DialogOption.SHOW_QUESTS
        );
    }
    
    public static DialogOption completeQuest() {
        return new DialogOption(
                Component.translatable(KEY_COMPLETE_QUEST),
                DialogResponsePacket.DialogOption.COMPLETE_QUEST
        );
    }

    public static DialogOption viewReputation() {
        return new DialogOption(
                Component.translatable(KEY_VIEW_REPUTATION),
                DialogResponsePacket.DialogOption.VIEW_REPUTATION
        );
    }

    public static DialogOption retrieveScroll() {
        return new DialogOption(
                Component.translatable(KEY_RETRIEVE_SCROLL),
                DialogResponsePacket.DialogOption.RETRIEVE_SCROLL
        );
    }
    
    public static DialogOption cancel() {
        return new DialogOption(
                Component.translatable(KEY_CANCEL),
                DialogResponsePacket.DialogOption.SHOW_QUESTS,
                true
        );
    }
}
