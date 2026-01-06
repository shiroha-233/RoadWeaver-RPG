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
 * 公会女仆 NPC 实体
 * 基于 TouhouLittleMaid 的女仆实体
 * 特性：使用酒狐模型、禁用语音和聊天气泡、可转身但不可移动
 */
public class GuildMaidEntity extends EntityMaid {
    
    private static final String TAG_IS_GUILD_NPC = "IsGuildNPC";
    private static final String TAG_HIT_COUNT = "HitCount";
    private static final String TAG_LAST_HIT_TIME = "LastHitTime";
    private static final String TAG_WEAPON_DRAWN = "WeaponDrawn";
    private static final double MAX_HEALTH = 10000.0;
    
    // 酒狐模型 ID（geckolib 版本）
    private static final String WINE_FOX_MODEL_ID = "geckolib:winefox";
    
    // 反击机制相关变量
    private int hitCount = 0; // 玩家击打次数
    private long lastHitTime = 0; // 最后一次被击打时间
    private boolean weaponDrawn = false; // 是否拔出武器
    private UUID lastAttacker = null; // 最后攻击者的UUID
    
    @SuppressWarnings("unchecked")
    public GuildMaidEntity(EntityType<? extends GuildMaidEntity> type, Level level) {
        // 由于继承自 EntityMaid，需要进行类型转换，这是不可避免的
        super((EntityType<EntityMaid>) (EntityType<?>) type, level);
        // 不设置 NoGravity，让实体正常受重力影响站在地面上
        initNPCSettings();
    }
    
    /**
     * 初始化 NPC 设置：模型、语音、聊天气泡
     */
    private void initNPCSettings() {
        // 设置酒狐模型
        this.setModelId(WINE_FOX_MODEL_ID);
        // 禁用语音（频率设为0）
        this.getConfigManager().setSoundFreq(0.0f);
        // 启用聊天气泡
        this.getConfigManager().setChatBubbleShow(true);
    }
    
    public static AttributeSupplier.Builder createNPCAttributes() {
        return EntityMaid.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }
    
    // ==================== 禁止位移但保留转身 ====================
    
    @Override
    public void move(MoverType type, Vec3 movement) {
        // 只允许重力下落（Y轴负方向），禁止水平移动
        if (type == MoverType.SELF || type == MoverType.PLAYER) {
            // 只保留垂直方向的移动
            super.move(type, new Vec3(0, movement.y, 0));
        }
    }
    
    @Override
    public void push(double x, double y, double z) {
        // 禁止被推动
    }
    
    @Override
    public void knockback(double strength, double x, double z) {
        // 禁止击退
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    protected void pushEntities() {
        // 不推动其他实体
    }
    
    // ==================== 禁止驯服 ====================
    
    @Override
    public boolean isTame() {
        return false;
    }
    
    @Override
    public void tame(Player player) {
        // 禁止驯服
    }
    
    @Override
    public void setTame(boolean tamed) {
        // 禁止设置驯服状态
    }
    
    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
    
    // ==================== 交互处理 ====================
    
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // 服务端发送打开对话界面的数据包
            openDialogForPlayer(serverPlayer);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    /**
     * 为玩家打开对话界面（由平台实现）
     */
    protected void openDialogForPlayer(net.minecraft.server.level.ServerPlayer player) {
        // 由子类或平台特定代码实现
    }
    
    // ==================== 伤害处理 ====================
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player attacker) {
            // 记录击打信息
            hitCount++;
            lastHitTime = this.level().getGameTime();
            lastAttacker = attacker.getUUID();
            
            // 第一次击打时拔出钻石剑
            if (hitCount == 1) {
                drawWeapon();
            }
            
            // 执行反击
            if (hitCount >= 2) {
                counterAttack(attacker);
            }
            
            // 受到伤害
            return super.hurt(source, amount);
        }
        return false;
    }
    
    @Override
    public boolean isInvulnerable() {
        return false; // 移除无敌状态
    }
    
    // ==================== AI 相关 ====================
    
    @Override
    public void aiStep() {
        // 检查是否需要收起武器（20秒没有被击打）
        if (weaponDrawn && (this.level().getGameTime() - lastHitTime) > 20 * 20) { // 20秒 = 400 ticks
            sheatheWeapon();
        }
        
        // 保留 AI 更新（允许转身看向玩家）
        // 只清除水平速度，保留垂直速度（重力）
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(0, motion.y, 0);
        super.aiStep();
    }
    
    @Override
    public void travel(Vec3 travelVector) {
        // 调用父类处理重力，但传入零向量禁止主动移动
        super.travel(Vec3.ZERO);
    }
    
    // ==================== 数据持久化 ====================
    
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, 
            MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        // 不调用父类方法，避免随机模型覆盖
        // 直接应用 NPC 设置
        initNPCSettings();
        return spawnDataIn;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_IS_GUILD_NPC, true);
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
        // 读取保存的数据
        hitCount = tag.getInt(TAG_HIT_COUNT);
        lastHitTime = tag.getLong(TAG_LAST_HIT_TIME);
        weaponDrawn = tag.getBoolean(TAG_WEAPON_DRAWN);
        if (tag.hasUUID("LastAttacker")) {
            lastAttacker = tag.getUUID("LastAttacker");
        }
        // 读取后重新应用 NPC 设置，确保数据一致性
        initNPCSettings();
        
        // 如果保存时武器已拔出，重新拔出
        if (weaponDrawn) {
            drawWeapon();
        }
    }
    
    // ==================== 音效相关 ====================
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
    
    // ==================== 显示名称 ====================
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.roadweaver_rpg.guild_maid");
    }
    
    @Override
    public boolean hasCustomName() {
        return true;
    }
    
    // ==================== 武器和反击机制 ====================
    
    /**
     * 拔出钻石剑武器
     */
    private void drawWeapon() {
        weaponDrawn = true;
        // 设置主手为钻石剑
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
    }
    
    /**
     * 收起武器
     */
    private void sheatheWeapon() {
        weaponDrawn = false;
        // 清空主手
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // 重置击打计数
        hitCount = 0;
        lastAttacker = null;
    }
    
    /**
     * 反击攻击者
     * @param attacker 攻击者
     */
    private void counterAttack(Player attacker) {
        if (attacker == null || !attacker.isAlive()) {
            return;
        }
        
        // 计算反击伤害：根据击打次数造成百分比伤害
        float damagePercentage = 0.0f;
        if (hitCount >= 10) {
            damagePercentage = 1.0f; // 100%伤害
        } else if (hitCount >= 5) {
            damagePercentage = 0.5f; // 50%伤害
        } else if (hitCount >= 2) {
            damagePercentage = 0.1f; // 10%伤害
        }
        
        if (damagePercentage > 0.0f) {
            // 计算实际伤害值（基于玩家最大血量）
            float maxHealth = attacker.getMaxHealth();
            float damage = maxHealth * damagePercentage;
            
            // 造成伤害
            attacker.hurt(this.damageSources().mobAttack(this), damage);
            
            // 播放攻击音效
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
        }
    }
}
