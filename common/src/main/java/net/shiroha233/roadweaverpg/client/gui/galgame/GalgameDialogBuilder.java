package net.shiroha233.roadweaverpg.client.gui.galgame;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Galgame对话界面构建器
 * 职责：提供流畅的API来构建对话界面
 * 原理：使用Builder模式简化对话界面的创建过程
 */
public class GalgameDialogBuilder {
    
    private final int npcEntityId;
    private final List<GalgameDialogScreen.DialogEntry> entries = new ArrayList<>();
    private final List<GalgameDialogScreen.DialogOptionData> options = new ArrayList<>();
    private Consumer<Integer> optionCallback;
    
    private Component npcName = Component.literal("NPC");
    private Component playerName = Component.literal("玩家");
    
    private GalgameDialogBuilder(int npcEntityId) {
        this.npcEntityId = npcEntityId;
    }
    
    /**
     * 创建构建器
     */
    public static GalgameDialogBuilder create(int npcEntityId) {
        return new GalgameDialogBuilder(npcEntityId);
    }
    
    /**
     * 设置NPC名称
     */
    public GalgameDialogBuilder npcName(Component name) {
        this.npcName = name;
        return this;
    }
    
    /**
     * 设置NPC名称（字符串版本）
     */
    public GalgameDialogBuilder npcName(String name) {
        this.npcName = Component.literal(name);
        return this;
    }
    
    /**
     * 设置NPC名称（翻译键版本）
     */
    public GalgameDialogBuilder npcNameTranslatable(String key) {
        this.npcName = Component.translatable(key);
        return this;
    }
    
    /**
     * 设置玩家名称
     */
    public GalgameDialogBuilder playerName(Component name) {
        this.playerName = name;
        return this;
    }
    
    /**
     * 添加NPC对话
     */
    public GalgameDialogBuilder npcSays(Component text) {
        entries.add(new GalgameDialogScreen.DialogEntry(npcName, text, true));
        return this;
    }
    
    /**
     * 添加NPC对话（字符串版本）
     */
    public GalgameDialogBuilder npcSays(String text) {
        return npcSays(Component.literal(text));
    }
    
    /**
     * 添加NPC对话（翻译键版本）
     */
    public GalgameDialogBuilder npcSaysTranslatable(String key) {
        return npcSays(Component.translatable(key));
    }
    
    /**
     * 添加NPC对话（带参数的翻译键）
     */
    public GalgameDialogBuilder npcSaysTranslatable(String key, Object... args) {
        return npcSays(Component.translatable(key, args));
    }
    
    /**
     * 添加玩家对话
     */
    public GalgameDialogBuilder playerSays(Component text) {
        entries.add(new GalgameDialogScreen.DialogEntry(playerName, text, false));
        return this;
    }
    
    /**
     * 添加玩家对话（字符串版本）
     */
    public GalgameDialogBuilder playerSays(String text) {
        return playerSays(Component.literal(text));
    }
    
    /**
     * 添加玩家对话（翻译键版本）
     */
    public GalgameDialogBuilder playerSaysTranslatable(String key) {
        return playerSays(Component.translatable(key));
    }
    
    /**
     * 添加选项
     */
    public GalgameDialogBuilder addOption(Component text, String actionId) {
        options.add(new GalgameDialogScreen.DialogOptionData(text, actionId));
        return this;
    }
    
    /**
     * 添加选项（字符串版本）
     */
    public GalgameDialogBuilder addOption(String text, String actionId) {
        return addOption(Component.literal(text), actionId);
    }
    
    /**
     * 添加选项（翻译键版本）
     */
    public GalgameDialogBuilder addOptionTranslatable(String key, String actionId) {
        return addOption(Component.translatable(key), actionId);
    }
    
    /**
     * 设置选项回调
     */
    public GalgameDialogBuilder onOptionSelected(Consumer<Integer> callback) {
        this.optionCallback = callback;
        return this;
    }
    
    /**
     * 构建对话界面
     */
    public GalgameDialogScreen build() {
        GalgameDialogScreen screen = new GalgameDialogScreen(npcEntityId);
        
        // 添加所有对话条目
        for (GalgameDialogScreen.DialogEntry entry : entries) {
            screen.addDialog(entry.speaker(), entry.text(), entry.isNpc());
        }
        
        // 设置选项
        if (!options.isEmpty() && optionCallback != null) {
            screen.setOptions(options, optionCallback);
        }
        
        return screen;
    }
}
