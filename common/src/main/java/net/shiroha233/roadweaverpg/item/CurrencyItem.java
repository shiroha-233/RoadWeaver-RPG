package net.shiroha233.roadweaverpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.currency.CurrencyType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 货币物品 - 支持多种货币类型
 * 
 * 设计原则：
 * - 每种货币都是独立的物品
 * - Shift+右键存入钱包
 * - 钱包内部统一以铜币为单位存储
 */
public class CurrencyItem extends Item {
    
    private final CurrencyType currencyType;
    
    // 存入钱包回调（由平台特定代码设置）
    private static BiConsumer<Player, ItemStack> depositHandler;
    
    public CurrencyItem(CurrencyType currencyType, Properties properties) {
        super(properties.stacksTo(64));
        this.currencyType = currencyType;
    }
    
    public CurrencyType getCurrencyType() {
        return currencyType;
    }
    
    /**
     * 设置存入钱包处理器（由Fabric/Forge端调用）
     */
    public static void setDepositHandler(BiConsumer<Player, ItemStack> handler) {
        depositHandler = handler;
    }
    
    /**
     * 获取存入钱包处理器
     */
    public static BiConsumer<Player, ItemStack> getDepositHandler() {
        return depositHandler;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Shift+右键存入钱包（手持货币时）
        if (player.isShiftKeyDown() && !level.isClientSide) {
            if (depositHandler != null) {
                depositHandler.accept(player, stack);
                return InteractionResultHolder.success(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 货币说明
        tooltip.add(Component.translatable("item.roadweaver_rpg.currency.tooltip")
                .withStyle(ChatFormatting.GRAY));
        
        // 价值说明（如果不是铜币）
        if (currencyType != CurrencyType.COPPER) {
            long copperValue = currencyType.getValueInCopper() * stack.getCount();
            tooltip.add(Component.literal("价值: " + copperValue + " 铜币")
                    .withStyle(ChatFormatting.YELLOW));
        }
        
        // 存入提示
        tooltip.add(Component.translatable("item.roadweaver_rpg.currency.deposit_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
