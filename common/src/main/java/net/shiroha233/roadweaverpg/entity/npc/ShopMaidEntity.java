package net.shiroha233.roadweaverpg.entity.npc;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * 商店女仆 NPC 实体
 * 基于 TouhouLittleMaid 的女仆实体
 * 负责打开商店界面
 */
public class ShopMaidEntity extends EntityMaid {
    
    private static final String TAG_IS_SHOP_NPC = "IsShopNPC";
    private static final String TAG_HIT_COUNT = "HitCount";
    private static final String TAG_LAST_HIT_TIME = "LastHitTime";
    private static final String TAG_WEAPON_DRAWN = "WeaponDrawn";
    private static final double MAX_HEALTH = 10000.0;
    
    // 使用不同的模型ID区分商店女仆
    private static final String SHOP_MODEL_ID = "geckolib:winefox";
    
    private int hitCount = 0;
    private long lastHitTime = 0;
    private boolean weaponDrawn = false;
    private UUID lastAttacker = null;
    
    @SuppressWarnings("unchecked")
    public ShopMaidEntity(EntityType<? extends ShopMaidEntity> type, Level level) {
        super((EntityType<EntityMaid>) (EntityType<?>) type, level);
        initNPCSettings();
    }
    
    private void initNPCSettings() {
        this.setModelId(SHOP_MODEL_ID);
        this.getConfigManager().setSoundFreq(0.0f);
        this.getConfigManager().setChatBubbleShow(true);
    }
    
    public static AttributeSupplier.Builder createNPCAttributes() {
        return EntityMaid.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }
    
    @Override
    public void move(MoverType type, Vec3 movement) {
        if (type == MoverType.SELF || type == MoverType.PLAYER) {
            super.move(type, new Vec3(0, movement.y, 0));
        }
    }
    
    @Override
    public void push(double x, double y, double z) {}
    
    @Override
    public void knockback(double strength, double x, double z) {}
    
    @Override
    public boolean isPushable() { return false; }
    
    @Override
    protected void pushEntities() {}
    
    @Override
    public boolean isTame() { return false; }
    
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
        if (!this.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            openShopDialogForPlayer(serverPlayer);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    /**
     * 为玩家打开商店对话界面（由平台实现）
     */
    protected void openShopDialogForPlayer(net.minecraft.server.level.ServerPlayer player) {
        // 由子类或平台特定代码实现
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player attacker) {
            hitCount++;
            lastHitTime = this.level().getGameTime();
            lastAttacker = attacker.getUUID();
            
            if (hitCount == 1) {
                drawWeapon();
            }
            
            if (hitCount >= 2) {
                counterAttack(attacker);
            }
            
            return super.hurt(source, amount);
        }
        return false;
    }
    
    @Override
    public boolean isInvulnerable() { return false; }
    
    @Override
    public void aiStep() {
        if (weaponDrawn && (this.level().getGameTime() - lastHitTime) > 20 * 20) {
            sheatheWeapon();
        }
        
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(0, motion.y, 0);
        super.aiStep();
    }
    
    @Override
    public void travel(Vec3 travelVector) {
        super.travel(Vec3.ZERO);
    }
    
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, 
            MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        initNPCSettings();
        return spawnDataIn;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_IS_SHOP_NPC, true);
        tag.putInt(TAG_HIT_COUNT, hitCount);
        tag.putLong(TAG_LAST_HIT_TIME, lastHitTime);
        tag.putBoolean(TAG_WEAPON_DRAWN, weaponDrawn);
        if (lastAttacker != null) {
            tag.putUUID("LastAttacker", lastAttacker);
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hitCount = tag.getInt(TAG_HIT_COUNT);
        lastHitTime = tag.getLong(TAG_LAST_HIT_TIME);
        weaponDrawn = tag.getBoolean(TAG_WEAPON_DRAWN);
        if (tag.hasUUID("LastAttacker")) {
            lastAttacker = tag.getUUID("LastAttacker");
        }
        initNPCSettings();
        
        if (weaponDrawn) {
            drawWeapon();
        }
    }
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() { return null; }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.roadweaver_rpg.shop_maid");
    }
    
    @Override
    public boolean hasCustomName() { return true; }
    
    private void drawWeapon() {
        weaponDrawn = true;
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
    }
    
    private void sheatheWeapon() {
        weaponDrawn = false;
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        hitCount = 0;
        lastAttacker = null;
    }
    
    private void counterAttack(Player attacker) {
        if (attacker == null || !attacker.isAlive()) return;
        
        float damagePercentage = 0.0f;
        if (hitCount >= 10) {
            damagePercentage = 1.0f;
        } else if (hitCount >= 5) {
            damagePercentage = 0.5f;
        } else if (hitCount >= 2) {
            damagePercentage = 0.1f;
        }
        
        if (damagePercentage > 0.0f) {
            float maxHealth = attacker.getMaxHealth();
            float damage = maxHealth * damagePercentage;
            attacker.hurt(this.damageSources().mobAttack(this), damage);
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        }
    }
}
