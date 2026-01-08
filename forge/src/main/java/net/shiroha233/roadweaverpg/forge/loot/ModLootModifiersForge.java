package net.shiroha233.roadweaverpg.forge.loot;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

/**
 * Forge全局战利品修改器注册
 * 
 * 设计原理：
 * - 使用DeferredRegister注册全局战利品修改器序列化器
 * - 在模组初始化时注册到事件总线
 * - 通过JSON配置文件控制修改器的应用条件
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = RoadWeaverRPG.MOD_ID)
public final class ModLootModifiersForge {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZER = 
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RoadWeaverRPG.MOD_ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> COIN_MODIFIER = 
            GLOBAL_LOOT_MODIFIER_SERIALIZER.register("coin", CoinGlobalLootModifier.CODEC);

    /**
     * 注册全局战利品修改器到事件总线
     */
    public static void register(IEventBus modEventBus) {
        GLOBAL_LOOT_MODIFIER_SERIALIZER.register(modEventBus);
    }
}
