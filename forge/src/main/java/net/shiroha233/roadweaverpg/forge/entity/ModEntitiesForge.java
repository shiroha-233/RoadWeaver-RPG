package net.shiroha233.roadweaverpg.forge.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.ModEntities;
import net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity;
import net.shiroha233.roadweaverpg.entity.npc.ShopMaidEntity;

/**
 * Forge 端实体注册
 */
@Mod.EventBusSubscriber(modid = RoadWeaverRPG.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntitiesForge {
    
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RoadWeaverRPG.MOD_ID);
    
    // 公会女仆实体类型
    public static final RegistryObject<EntityType<GuildMaidEntityForge>> GUILD_MAID = ENTITY_TYPES.register(
            ModEntities.GUILD_MAID_ID,
            () -> EntityType.Builder.<GuildMaidEntityForge>of(GuildMaidEntityForge::new, MobCategory.MISC)
                    .sized(0.6f, 1.5f)
                    .clientTrackingRange(10)
                    .build(ModEntities.GUILD_MAID_ID)
    );
    
    // 商店女仆实体类型
    public static final RegistryObject<EntityType<ShopMaidEntityForge>> SHOP_MAID = ENTITY_TYPES.register(
            ModEntities.SHOP_MAID_ID,
            () -> EntityType.Builder.<ShopMaidEntityForge>of(ShopMaidEntityForge::new, MobCategory.MISC)
                    .sized(0.6f, 1.5f)
                    .clientTrackingRange(10)
                    .build(ModEntities.SHOP_MAID_ID)
    );
    
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GUILD_MAID.get(), GuildMaidEntity.createNPCAttributes().build());
        event.put(SHOP_MAID.get(), ShopMaidEntity.createNPCAttributes().build());
    }
}
