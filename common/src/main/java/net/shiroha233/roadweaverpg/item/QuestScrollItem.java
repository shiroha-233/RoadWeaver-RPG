package net.shiroha233.roadweaverpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaverpg.quest.type.QuestRank;
import net.shiroha233.roadweaverpg.quest.type.QuestState;
import net.shiroha233.roadweaverpg.quest.definition.QuestDefinition;
import net.shiroha233.roadweaverpg.quest.instance.QuestInstance;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 委托书物品 - 存储委托实例数据
 * 包含委托ID、进度状态等信息
 */
public class QuestScrollItem extends Item {
    
    // NBT标签
    public static final String TAG_QUEST_ID = "QuestId";
    public static final String TAG_QUEST_RANK = "QuestRank";
    public static final String TAG_QUEST_TITLE = "QuestTitle";
    public static final String TAG_HAS_QUEST = "HasQuest";
    public static final String TAG_INSTANCE_ID = "InstanceId";
    public static final String TAG_STATE = "State";
    
    public QuestScrollItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide && hasQuest(stack)) {
            openQuestProgress(stack);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    
    /** 打开委托进度界面（客户端实现） */
    protected void openQuestProgress(ItemStack stack) {
        // 由平台特定代码实现
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasQuest(stack)) {
            QuestRank rank = getQuestRank(stack);
            String title = getQuestTitle(stack);
            QuestState state = getQuestState(stack);
            
            tooltip.add(Component.literal("[" + rank.getDisplayName() + "级委托]")
                    .withStyle(rank.getColor()));
            tooltip.add(Component.literal(title).withStyle(ChatFormatting.GRAY));
            
            // 显示状态
            Component stateText = switch (state) {
                case IN_PROGRESS -> Component.translatable("item.roadweaver_rpg.quest_scroll.in_progress")
                        .withStyle(ChatFormatting.YELLOW);
                case COMPLETED -> Component.translatable("item.roadweaver_rpg.quest_scroll.completed")
                        .withStyle(ChatFormatting.GREEN);
                default -> Component.translatable("item.roadweaver_rpg.quest_scroll.unknown")
                        .withStyle(ChatFormatting.GRAY);
            };
            tooltip.add(stateText);
            
            // 提示
            if (state == QuestState.COMPLETED) {
                tooltip.add(Component.translatable("item.roadweaver_rpg.quest_scroll.turn_in_hint")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            } else {
                tooltip.add(Component.translatable("item.roadweaver_rpg.quest_scroll.view_hint")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        } else {
            tooltip.add(Component.translatable("item.roadweaver_rpg.quest_scroll.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
    
    @Override
    public Component getName(ItemStack stack) {
        if (hasQuest(stack)) {
            QuestRank rank = getQuestRank(stack);
            QuestState state = getQuestState(stack);
            
            // 已完成的委托书显示特殊颜色
            if (state == QuestState.COMPLETED) {
                return Component.translatable("item.roadweaver_rpg.quest_scroll.completed_name")
                        .withStyle(ChatFormatting.GREEN);
            }
            return Component.translatable("item.roadweaver_rpg.quest_scroll.filled")
                    .withStyle(rank.getColor());
        }
        return super.getName(stack);
    }
    
    // region 静态工具方法
    public static boolean hasQuest(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_HAS_QUEST);
    }
    
    public static QuestRank getQuestRank(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_QUEST_RANK)) {
            return QuestRank.fromIndex(tag.getInt(TAG_QUEST_RANK));
        }
        return QuestRank.D;
    }
    
    public static String getQuestTitle(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_QUEST_TITLE) : "";
    }
    
    public static Optional<ResourceLocation> getQuestId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_QUEST_ID)) {
            return Optional.of(new ResourceLocation(tag.getString(TAG_QUEST_ID)));
        }
        return Optional.empty();
    }
    
    public static Optional<UUID> getInstanceId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(TAG_INSTANCE_ID)) {
            return Optional.of(tag.getUUID(TAG_INSTANCE_ID));
        }
        return Optional.empty();
    }
    
    public static QuestState getQuestState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STATE)) {
            return QuestState.fromString(tag.getString(TAG_STATE));
        }
        return QuestState.IN_PROGRESS;
    }
    
    /** 写入委托定义（接受委托时使用） */
    public static ItemStack writeQuest(ItemStack stack, QuestDefinition quest, QuestInstance instance) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_HAS_QUEST, true);
        tag.putString(TAG_QUEST_ID, quest.getId().toString());
        tag.putInt(TAG_QUEST_RANK, quest.getRank().getIndex());
        tag.putString(TAG_QUEST_TITLE, quest.getTitle().getString());
        tag.putUUID(TAG_INSTANCE_ID, instance.getInstanceId());
        tag.putString(TAG_STATE, instance.getState().getSerializedName());
        return stack;
    }
    
    /** 更新委托状态 */
    public static void updateState(ItemStack stack, QuestState state) {
        if (hasQuest(stack)) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(TAG_STATE, state.getSerializedName());
        }
    }
    
    /** 创建带委托的委托书 */
    public static ItemStack createWithQuest(Item item, QuestDefinition quest, QuestInstance instance) {
        ItemStack stack = new ItemStack(item);
        return writeQuest(stack, quest, instance);
    }
    // endregion
}
