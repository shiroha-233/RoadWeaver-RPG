package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

/**
 * 结构条件判定
 */
public final class StructureCondition implements PlayerCondition<ConditionContext> {
    
    private final ResourceLocation structureId;
    
    public StructureCondition(ResourceLocation structureId) {
        this.structureId = structureId;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        try {
            ServerLevel level = player.serverLevel();
            BlockPos pos = context != null && context.getPosition().isPresent()
                    ? context.getPosition().get()
                    : player.blockPosition();
            
            var structureKey = ResourceKey.create(Registries.STRUCTURE, structureId);
            return level.structureManager()
                    .getStructureWithPieceAt(pos, structureKey)
                    .isValid();
        } catch (Exception e) {
            return false;
        }
    }
    
    public static StructureCondition of(String structureId) {
        return new StructureCondition(new ResourceLocation(structureId));
    }
    
    public ResourceLocation getStructureId() {
        return structureId;
    }
}
