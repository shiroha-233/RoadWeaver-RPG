package net.shiroha233.roadweaverpg.entity.npc;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.shiroha233.roadweaverpg.entity.npc.action.INPCAnimatable;
import net.shiroha233.roadweaverpg.entity.npc.action.NPCActionManager;
import net.shiroha233.roadweaverpg.entity.npc.action.NPCActionType;
import net.shiroha233.roadweaverpg.entity.npc.behavior.NPCBehaviorManager;
import net.shiroha233.roadweaverpg.entity.npc.voice.INPCVoiceable;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceManager;
import net.shiroha233.roadweaverpg.entity.npc.voice.NPCVoiceType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NPC实体基类
 * 职责：提供所有NPC的通用功能实现
 * 原理：模板方法模式 - 定义通用流程，子类实现具体细节
 */
public abstract class BaseNPCEntity extends EntityMaid implements INPCEntity, 
        NPCBehavior.Counterable, NPCBehavior.Immovable, INPCAnimatable, INPCVoiceable {
    
    private static final double MAX_HEALTH = 10000.0;
    
    // 反击机制
    private int hitCount = 0;
    private long lastHitTime = 0;
    private boolean weaponDrawn = false;
    private UUID lastAttacker = null;
    
    // 语音设置
    private String soundPackId = "";
    private boolean voiceEnabled = true;
    
    @SuppressWarnings("unchecked")
    public BaseNPCEntity(EntityType<? extends BaseNPCEntity> type, Level level) {
        super((EntityType<EntityMaid>) (EntityType<?>) type, level);
        initNPCSettings();
    }
    
    /**
     * 初始化NPC设置（子类可重写）
     */
    protected void initNPCSettings() {
        this.setModelId(getModelId());
        this.getConfigManager().setSoundFreq(0.0f);
        this.getConfigManager().setChatBubbleShow(true);
    }
    
    public static AttributeSupplier.Builder createNPCAttributes() {
        return EntityMaid.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }
    
    // ==================== INPCEntity 实现 ====================
    
    @Override
    public Component getNPCDisplayName() {
        return Component.translatable("entity.roadweaver_rpg." + getNPCType().getId());
    }
    
    @Override
    public EntityMaid asEntity() {
        return this;
    }
    
    // ==================== Immovable 实现 ====================
    
    @Override
    public boolean isCompletelyImmovable() {
        return true;
    }
    
    @Override
    public boolean allowGravity() {
        return true;
    }
    
    @Override
    public void move(MoverType type, Vec3 movement) {
        if (isCompletelyImmovable() && (type == MoverType.SELF || type == MoverType.PLAYER)) {
            // 只允许重力
            super.move(type, new Vec3(0, allowGravity() ? movement.y : 0, 0));
        } else {
            super.move(type, movement);
        }
    }
    
    @Override
    public void push(double x, double y, double z) {
        if (!isCompletelyImmovable()) {
            super.push(x, y, z);
        }
    }
    
    @Override
    public void knockback(double strength, double x, double z) {
        if (!isCompletelyImmovable()) {
            super.knockback(strength, x, z);
        }
    }
    
    @Override
    public boolean isPushable() {
        return !isCompletelyImmovable();
    }
    
    @Override
    protected void pushEntities() {
        if (!isCompletelyImmovable()) {
            super.pushEntities();
        }
    }
    
    // ==================== Counterable 实现 ====================
    
    @Override
    public boolean isCounterAttackEnabled() {
        return true;
    }
    
    @Override
    public void handleCounterAttack(ServerPlayer attacker, int hitCount) {
        if (attacker == null || !attacker.isAlive()) return;
        
        float damagePercentage = calculateCounterDamage(hitCount);
        if (damagePercentage > 0.0f) {
            float maxHealth = attacker.getMaxHealth();
            float damage = maxHealth * damagePercentage;
            attacker.hurt(this.damageSources().mobAttack(this), damage);
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        }
    }
    
    /**
     * 计算反击伤害百分比（子类可重写）
     */
    protected float calculateCounterDamage(int hitCount) {
        if (hitCount >= 10) return 1.0f;
        if (hitCount >= 5) return 0.5f;
        if (hitCount >= 2) return 0.1f;
        return 0.0f;
    }
    
    // ==================== 实体行为 ====================
    
    @Override
    public boolean isTame() {
        return false;
    }
    
    @Override
    public void tame(Player player) {}
    
    @Override
    public void setTame(boolean tamed) {}
    
    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 注册到服务层
            NPCService.getInstance().registerNPC(this);
            // 处理交互
            handlePlayerInteraction(serverPlayer);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCounterAttackEnabled() && source.getEntity() instanceof Player attacker) {
            hitCount++;
            lastHitTime = this.level().getGameTime();
            lastAttacker = attacker.getUUID();
            
            if (hitCount == 1) {
                drawWeapon();
            }
            
            if (hitCount >= 2 && attacker instanceof ServerPlayer serverPlayer) {
                handleCounterAttack(serverPlayer, hitCount);
            }
            
            return super.hurt(source, amount);
        }
        return false;
    }
    
    @Override
    public boolean isInvulnerable() {
        return false;
    }
    
    @Override
    public void aiStep() {
        // 武器收回逻辑
        if (weaponDrawn && (this.level().getGameTime() - lastHitTime) > 20 * 20) {
            sheatheWeapon();
        }
        
        // 禁止水平移动
        if (isCompletelyImmovable()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(0, motion.y, 0);
        }
        
        super.aiStep();
    }
    
    @Override
    public void travel(Vec3 travelVector) {
        if (isCompletelyImmovable()) {
            super.travel(Vec3.ZERO);
        } else {
            super.travel(travelVector);
        }
    }
    
    // ==================== 数据持久化 ====================
    
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn,
                                         MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, 
                                         @Nullable CompoundTag dataTag) {
        initNPCSettings();
        return spawnDataIn;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NPCType", getNPCType().getId());
        tag.putInt("HitCount", hitCount);
        tag.putLong("LastHitTime", lastHitTime);
        tag.putBoolean("WeaponDrawn", weaponDrawn);
        if (lastAttacker != null) {
            tag.putUUID("LastAttacker", lastAttacker);
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hitCount = tag.getInt("HitCount");
        lastHitTime = tag.getLong("LastHitTime");
        weaponDrawn = tag.getBoolean("WeaponDrawn");
        if (tag.hasUUID("LastAttacker")) {
            lastAttacker = tag.getUUID("LastAttacker");
        }
        initNPCSettings();
        
        if (weaponDrawn) {
            drawWeapon();
        }
    }
    
    // ==================== 名称显示 ====================
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
    
    @Override
    public Component getDisplayName() {
        // 在Galgame对话渲染时不显示名称
        if (this.level().isClientSide && 
            net.shiroha233.roadweaverpg.client.gui.galgame.GalgameEntityRenderer.isRenderingEntity(this.getId())) {
            return Component.empty();
        }
        return getNPCDisplayName();
    }
    
    @Override
    public boolean hasCustomName() {
        // 在Galgame对话渲染时返回false
        if (this.level().isClientSide && 
            net.shiroha233.roadweaverpg.client.gui.galgame.GalgameEntityRenderer.isRenderingEntity(this.getId())) {
            return false;
        }
        return true;
    }
    
    // ==================== 武器系统 ====================
    
    protected void drawWeapon() {
        weaponDrawn = true;
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        // 触发攻击任务以播放动画
        if (this.level() instanceof ServerLevel) {
            TaskManager.findTask(new ResourceLocation("touhoulittlemaid", "attack"))
                    .ifPresent(this::setTask);
        }
    }
    
    protected void sheatheWeapon() {
        weaponDrawn = false;
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        hitCount = 0;
        lastAttacker = null;
        // 恢复空闲任务
        this.setTask(TaskManager.getIdleTask());
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    public void remove(RemovalReason reason) {
        // 从服务层注销
        NPCService.getInstance().unregisterNPC(this.getId());
        super.remove(reason);
    }
}
