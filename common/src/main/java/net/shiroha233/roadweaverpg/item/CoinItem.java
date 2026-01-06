package net.shiroha233.roadweaverpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 金币物品 - 游戏货币
 */
public class CoinItem extends Item {
    
    public CoinItem(Properties properties) {
        super(properties.stacksTo(64));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.roadweaver_rpg.coin.tooltip")
                .withStyle(style -> style.withColor(0xFFD700)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
