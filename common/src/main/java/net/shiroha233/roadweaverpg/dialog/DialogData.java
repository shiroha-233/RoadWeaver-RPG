package net.shiroha233.roadweaverpg.dialog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 对话数据模型
 * 职责：存储单个对话的完整数据结构
 * 原理：不可变数据对象，支持JSON序列化和网络传输
 * 
 * 文本处理：直接使用数据包中的文本内容，不使用语言文件翻译，方便玩家自定义对话
 */
public record DialogData(
        ResourceLocation id,
        String npcType,
        List<DialogLine> lines,
        List<DialogChoice> choices,
        Optional<ResourceLocation> nextDialog
) {
    
    /**
     * 从JSON解析对话数据
     */
    public static DialogData fromJson(ResourceLocation id, JsonObject json) {
        String npcType = json.has("npc_type") ? json.get("npc_type").getAsString() : "default";
        
        // 解析对话行
        List<DialogLine> lines = new ArrayList<>();
        if (json.has("lines")) {
            JsonArray linesArray = json.getAsJsonArray("lines");
            for (JsonElement element : linesArray) {
                lines.add(DialogLine.fromJson(element.getAsJsonObject()));
            }
        }
        
        // 解析选项
        List<DialogChoice> choices = new ArrayList<>();
        if (json.has("choices")) {
            JsonArray choicesArray = json.getAsJsonArray("choices");
            for (JsonElement element : choicesArray) {
                choices.add(DialogChoice.fromJson(element.getAsJsonObject()));
            }
        }
        
        // 解析下一个对话
        Optional<ResourceLocation> nextDialog = Optional.empty();
        if (json.has("next")) {
            nextDialog = Optional.of(new ResourceLocation(json.get("next").getAsString()));
        }
        
        return new DialogData(id, npcType, 
                Collections.unmodifiableList(lines), 
                Collections.unmodifiableList(choices), 
                nextDialog);
    }
    
    /**
     * 网络序列化
     */
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeUtf(npcType);
        
        buf.writeVarInt(lines.size());
        for (DialogLine line : lines) {
            line.toNetwork(buf);
        }
        
        buf.writeVarInt(choices.size());
        for (DialogChoice choice : choices) {
            choice.toNetwork(buf);
        }
        
        buf.writeBoolean(nextDialog.isPresent());
        nextDialog.ifPresent(buf::writeResourceLocation);
    }
    
    /**
     * 网络反序列化
     */
    public static DialogData fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        String npcType = buf.readUtf();
        
        int lineCount = buf.readVarInt();
        List<DialogLine> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(DialogLine.fromNetwork(buf));
        }
        
        int choiceCount = buf.readVarInt();
        List<DialogChoice> choices = new ArrayList<>(choiceCount);
        for (int i = 0; i < choiceCount; i++) {
            choices.add(DialogChoice.fromNetwork(buf));
        }
        
        Optional<ResourceLocation> nextDialog = buf.readBoolean() 
                ? Optional.of(buf.readResourceLocation()) 
                : Optional.empty();
        
        return new DialogData(id, npcType, 
                Collections.unmodifiableList(lines), 
                Collections.unmodifiableList(choices), 
                nextDialog);
    }
    
    /**
     * 对话行数据
     * 文本直接存储内容，不使用翻译键，方便玩家自定义
     * behavior: 可选的行为ID，用于触发NPC动作和语音
     */
    public record DialogLine(
            String speaker,           // "npc" 或 "player"
            String text,              // 直接文本内容（不是翻译键）
            Optional<String> condition, // 可选条件
            Optional<ResourceLocation> behavior // 可选行为ID（触发动作和语音）
    ) {
        public static DialogLine fromJson(JsonObject json) {
            String speaker = json.has("speaker") ? json.get("speaker").getAsString() : "npc";
            String text = json.get("text").getAsString();
            Optional<String> condition = json.has("condition") 
                    ? Optional.of(json.get("condition").getAsString()) 
                    : Optional.empty();
            Optional<ResourceLocation> behavior = json.has("behavior")
                    ? Optional.of(new ResourceLocation(json.get("behavior").getAsString()))
                    : Optional.empty();
            return new DialogLine(speaker, text, condition, behavior);
        }
        
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeUtf(speaker);
            buf.writeUtf(text);
            buf.writeBoolean(condition.isPresent());
            condition.ifPresent(buf::writeUtf);
            buf.writeBoolean(behavior.isPresent());
            behavior.ifPresent(buf::writeResourceLocation);
        }
        
        public static DialogLine fromNetwork(FriendlyByteBuf buf) {
            String speaker = buf.readUtf();
            String text = buf.readUtf();
            Optional<String> condition = buf.readBoolean() 
                    ? Optional.of(buf.readUtf()) 
                    : Optional.empty();
            Optional<ResourceLocation> behavior = buf.readBoolean()
                    ? Optional.of(buf.readResourceLocation())
                    : Optional.empty();
            return new DialogLine(speaker, text, condition, behavior);
        }
        
        /**
         * 获取文本组件（直接使用文本内容）
         */
        public Component getTextComponent() {
            return Component.literal(text);
        }
        
        public boolean isNpc() {
            return "npc".equals(speaker);
        }
    }
    
    /**
     * 对话选项数据
     * 文本直接存储内容，不使用翻译键，方便玩家自定义
     * 
     * responseText: 玩家选择后显示的对话内容（可选，如果为空则不显示玩家回话）
     */
    public record DialogChoice(
            String id,                          // 选项ID
            String text,                        // 选项按钮文本（直接内容）
            String responseText,                // 玩家选择后的回话内容（直接内容，可为空）
            String action,                      // 动作类型
            Optional<ResourceLocation> nextDialog, // 跳转对话
            Optional<String> condition          // 显示条件
    ) {
        public static DialogChoice fromJson(JsonObject json) {
            String id = json.get("id").getAsString();
            String text = json.get("text").getAsString();
            // 玩家回话内容，如果没有则使用选项文本
            String responseText = json.has("response") 
                    ? json.get("response").getAsString() 
                    : "";
            String action = json.has("action") ? json.get("action").getAsString() : "none";
            
            Optional<ResourceLocation> nextDialog = json.has("next") 
                    ? Optional.of(new ResourceLocation(json.get("next").getAsString())) 
                    : Optional.empty();
            
            Optional<String> condition = json.has("condition") 
                    ? Optional.of(json.get("condition").getAsString()) 
                    : Optional.empty();
            
            return new DialogChoice(id, text, responseText, action, nextDialog, condition);
        }
        
        public void toNetwork(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(text);
            buf.writeUtf(responseText);
            buf.writeUtf(action);
            buf.writeBoolean(nextDialog.isPresent());
            nextDialog.ifPresent(buf::writeResourceLocation);
            buf.writeBoolean(condition.isPresent());
            condition.ifPresent(buf::writeUtf);
        }
        
        public static DialogChoice fromNetwork(FriendlyByteBuf buf) {
            String id = buf.readUtf();
            String text = buf.readUtf();
            String responseText = buf.readUtf();
            String action = buf.readUtf();
            Optional<ResourceLocation> nextDialog = buf.readBoolean() 
                    ? Optional.of(buf.readResourceLocation()) 
                    : Optional.empty();
            Optional<String> condition = buf.readBoolean() 
                    ? Optional.of(buf.readUtf()) 
                    : Optional.empty();
            return new DialogChoice(id, text, responseText, action, nextDialog, condition);
        }
        
        /**
         * 获取选项按钮文本组件
         */
        public Component getTextComponent() {
            return Component.literal(text);
        }
        
        /**
         * 获取玩家回话文本组件
         */
        public Component getResponseComponent() {
            return Component.literal(responseText);
        }
        
        /**
         * 是否有玩家回话内容
         */
        public boolean hasResponse() {
            return responseText != null && !responseText.isEmpty();
        }
    }
}
