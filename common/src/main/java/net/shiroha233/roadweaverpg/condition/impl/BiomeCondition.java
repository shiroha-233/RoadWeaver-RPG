package net.shiroha233.roadweaverpg.condition.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaverpg.condition.ConditionContext;
import net.shiroha233.roadweaverpg.condition.PlayerCondition;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 群系条件判定 - 使用缓存优化
 */
public final class BiomeCondition implements PlayerCondition<ConditionContext> {
    
    // 缓存
    private static final ConcurrentHashMap<String, BiomeCondition> BIOME_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BiomeCondition> TAG_CACHE = new ConcurrentHashMap<>();
    
    private final ResourceLocation biomeId;
    private final ResourceLocation biomeTag;
    private final boolean useTag;
    
    private BiomeCondition(ResourceLocation biomeId, ResourceLocation biomeTag, boolean useTag) {
        this.biomeId = biomeId;
        this.biomeTag = biomeTag;
        this.useTag = useTag;
    }
    
    @Override
    public boolean evaluate(ServerPlayer player, ConditionContext context) {
        BlockPos pos = context != null && context.getPosition().isPresent() 
                ? context.getPosition().get() 
                : player.blockPosition();
        
        Holder<Biome> biome = player.serverLevel().getBiome(pos);
        
        if (useTag) {
            return biome.is(TagKey.create(Registries.BIOME, biomeTag));
        } else {
            return biome.is(ResourceKey.create(Registries.BIOME, biomeId));
        }
    }
    
    public static BiomeCondition of(String biomeId) {
        return BIOME_CACHE.computeIfAbsent(biomeId, 
                id -> new BiomeCondition(new ResourceLocation(id), null, false));
    }
    
    public static BiomeCondition ofTag(String tagId) {
        return TAG_CACHE.computeIfAbsent(tagId,
                id -> new BiomeCondition(null, new ResourceLocation(id), true));
    }
    
    public ResourceLocation getBiomeId() { return biomeId; }
}
