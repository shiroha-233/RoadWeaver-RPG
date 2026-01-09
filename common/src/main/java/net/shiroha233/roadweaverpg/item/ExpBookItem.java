package net.shiroha233.roadweaverpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 经验书物品 - 使用后获得玩家等级经验
 * 
 * 设计原理：
 * - 玩家等级只能通过完成委托或使用经验书获得
 * - 不同等级的经验书提供不同数量的经验
 * - 使用回调模式处理服务端逻辑，保持common模块的独立性
 */
public class ExpBookItem extends Item {
    
    /** 经验书等级 */
    public enum Tier {
        SMALL(100, "small"),      // 小型经验书
        MEDIUM(500, "medium"),    // 中型经验书
        LARGE(2000, "large"),     // 大型经验书
        GRAND(10000, "grand");    // 特大经验书
        
        private final int expAmount;
        private final String name;
        
        Tier(int expAmount, String name) {
            this.expAmount = expAmount;
            this.name = name;
        }
        
        public int getExpAmount() { return expAmount; }
        public String getName() { return name; }
    }
    
    private final Tier tier;
    
    // 使用经验书回调（由平台特定代码设置）
    private static BiConsumer<ServerPlayer, Integer> useExpBookHandler;
    
    public ExpBookItem(Tier tier, Properties properties) {
        super(properties.stacksTo(64));
        this.tier = tier;
    }
    
    public Tier getTier() {
        return tier;
    }
    
    public int getExpAmount() {
        return tier.getExpAmount();
    }
    
    /**
     * 设置使用经验书处理器（由Fabric/Forge端调用）
     */
    public static void setUseExpBookHandler(BiConsumer<ServerPlayer, Integer> handler) {
        useExpBookHandler = handler;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (useExpBookHandler != null) {
                useExpBookHandler.accept(serverPlayer, tier.getExpAmount());
                stack.shrink(1);
                
                // 播放使用音效
                player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.5f, 1.0f);
                
                return InteractionResultHolder.consume(stack);
            }
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 经验值说明
        tooltip.add(Component.translatable("item.roadweaver_rpg.exp_book.tooltip", tier.getExpAmount())
                .withStyle(ChatFormatting.GREEN));
        
        // 使用提示
        tooltip.add(Component.translatable("item.roadweaver_rpg.exp_book.use_hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
