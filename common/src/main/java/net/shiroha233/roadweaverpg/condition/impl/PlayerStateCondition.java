package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家状态条件判定 - 使用单例缓存优化
 */
public final class PlayerStateCondition implements PlayerCondition<ConditionContext> {
    
    public enum StateType {
        SNEAKING, SPRINTING, SWIMMING, FLYING, ON_GROUND, GAME_MODE
    }
    
    // 单例缓存
    private static final PlayerStateCondition SNEAKING_INSTANCE = new PlayerStateCondition(StateType.SNEAKING, null);
    private static final PlayerStateCondition SPRINTING_INSTANCE = new PlayerStateCondition(StateType.SPRINTING, null);
    private static final PlayerStateCondition SWIMMING_INSTANCE = new PlayerStateCondition(StateType.SWIMMING, null);
    private static final PlayerStateCondition FLYING_INSTANCE = new PlayerStateCondition(StateType.FLYING, null);
    private static final PlayerStateCondition ON_GROUND_INSTANCE = new PlayerStateCondition(StateType.ON_GROUND, null);
    private static final ConcurrentHashMap<GameType, PlayerStateCondition> GAME_MODE_CACHE = new ConcurrentHashMap<>();
    
    private final StateType type;
    private final GameType gameMode;
    
    private PlayerStateCondition(StateType type, GameType gameMode) {
        this.type = type;
        this.gameMode = gameMode;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        return switch (type) {
            case SNEAKING -> player.isShiftKeyDown();
            case SPRINTING -> player.isSprinting();
            case SWIMMING -> player.isSwimming();
            case FLYING -> player.getAbilities().flying;
            case ON_GROUND -> player.onGround();
            case GAME_MODE -> player.gameMode.getGameModeForPlayer() == gameMode;
        };
    }
    
    public static PlayerStateCondition sneaking() { return SNEAKING_INSTANCE; }
    public static PlayerStateCondition sprinting() { return SPRINTING_INSTANCE; }
    public static PlayerStateCondition swimming() { return SWIMMING_INSTANCE; }
    public static PlayerStateCondition flying() { return FLYING_INSTANCE; }
    public static PlayerStateCondition onGround() { return ON_GROUND_INSTANCE; }
    
    public static PlayerStateCondition gameMode(GameType mode) {
        return GAME_MODE_CACHE.computeIfAbsent(mode, m -> new PlayerStateCondition(StateType.GAME_MODE, m));
    }
    
    public StateType getType() { return type; }
}
