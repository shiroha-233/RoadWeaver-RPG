package net.shiroha233.roadweaverpg.playerlevel.effect.impl;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffect;
import net.shiroha233.roadweaverpg.playerlevel.effect.LevelEffectType;

/**
 * 命令执行效果
 * 用于技能解锁、特殊奖励等场景
 * 注意：仅在升级时执行一次，不会重复执行
 */
public class CommandEffect implements LevelEffect {
    
    private final String command;
    private final String description;
    
    public CommandEffect(String command, String description) {
        this.command = command;
        this.description = description;
    }
    
    @Override
    public String getTypeId() {
        return LevelEffectType.COMMAND.getId();
    }
    
    @Override
    public Component getDescription() {
        return Component.literal(description);
    }
    
    @Override
    public void apply(ServerPlayer player, int level) {
        if (player.getServer() == null) return;
        
        // 替换占位符
        String finalCommand = command
                .replace("{player}", player.getName().getString())
                .replace("{level}", String.valueOf(level))
                .replace("{uuid}", player.getUUID().toString());
        
        try {
            player.getServer().getCommands().performPrefixedCommand(
                    player.getServer().createCommandSourceStack().withSuppressedOutput(),
                    finalCommand
            );
            RoadWeaverRPG.LOGGER.debug("Executed command for {}: {}", 
                    player.getName().getString(), finalCommand);
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to execute command: {}", finalCommand, e);
        }
    }
    
    @Override
    public void remove(ServerPlayer player) {
        // 命令效果不支持移除
    }
    
    @Override
    public void refresh(ServerPlayer player, int level) {
        // 命令效果不需要刷新，仅在升级时执行一次
    }
    
    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(command);
        buf.writeUtf(description);
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", getTypeId());
        json.addProperty("command", command);
        json.addProperty("description", description);
        return json;
    }
    
    public static CommandEffect fromJson(JsonObject json) {
        String command = json.has("command") ? json.get("command").getAsString() : "";
        String description = json.has("description") ? json.get("description").getAsString() : "执行命令";
        return new CommandEffect(command, description);
    }
    
    public static CommandEffect fromNetwork(FriendlyByteBuf buf) {
        String command = buf.readUtf();
        String description = buf.readUtf();
        return new CommandEffect(command, description);
    }
}
