package net.shiroha233.roadweaverpg.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 条件评估上下文
 * 
 * 设计原理：
 * - 携带条件评估所需的额外信息
 * - 不可变设计，线程安全
 * - Builder模式构建
 */
public final class ConditionContext {
    
    private final @Nullable BlockPos position;
    private final @Nullable Entity targetEntity;
    private final @Nullable ResourceLocation dimension;
    private final @Nullable ResourceLocation biome;
    private final int npcEntityId;
    private final Map<String, Object> extras;
    
    private ConditionContext(Builder builder) {
        this.position = builder.position;
        this.targetEntity = builder.targetEntity;
        this.dimension = builder.dimension;
        this.biome = builder.biome;
        this.npcEntityId = builder.npcEntityId;
        this.extras = Map.copyOf(builder.extras);
    }
    
    // ==================== Getters ====================
    
    public Optional<BlockPos> getPosition() { return Optional.ofNullable(position); }
    public Optional<Entity> getTargetEntity() { return Optional.ofNullable(targetEntity); }
    public Optional<ResourceLocation> getDimension() { return Optional.ofNullable(dimension); }
    public Optional<ResourceLocation> getBiome() { return Optional.ofNullable(biome); }
    public int getNpcEntityId() { return npcEntityId; }
    
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getExtra(String key, Class<T> type) {
        Object value = extras.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    // ==================== 静态工厂方法 ====================
    
    public static ConditionContext empty() {
        return new Builder().build();
    }
    
    public static ConditionContext ofPosition(BlockPos pos) {
        return new Builder().position(pos).build();
    }
    
    public static ConditionContext ofEntity(Entity entity) {
        return new Builder().targetEntity(entity).build();
    }
    
    public static ConditionContext ofNpc(int npcEntityId) {
        return new Builder().npcEntityId(npcEntityId).build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // ==================== Builder ====================
    
    public static final class Builder {
        private BlockPos position;
        private Entity targetEntity;
        private ResourceLocation dimension;
        private ResourceLocation biome;
        private int npcEntityId = -1;
        private final Map<String, Object> extras = new HashMap<>();
        
        public Builder position(BlockPos pos) {
            this.position = pos;
            return this;
        }
        
        public Builder targetEntity(Entity entity) {
            this.targetEntity = entity;
            return this;
        }
        
        public Builder dimension(ResourceLocation dim) {
            this.dimension = dim;
            return this;
        }
        
        public Builder biome(ResourceLocation biome) {
            this.biome = biome;
            return this;
        }
        
        public Builder npcEntityId(int id) {
            this.npcEntityId = id;
            return this;
        }
        
        public Builder extra(String key, Object value) {
            this.extras.put(key, value);
            return this;
        }
        
        public ConditionContext build() {
            return new ConditionContext(this);
        }
    }
}
