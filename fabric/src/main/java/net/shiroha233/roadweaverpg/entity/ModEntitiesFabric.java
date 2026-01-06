package net.shiroha233.roadweaverpg.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;
import net.shiroha233.roadweaverpg.entity.npc.GuildMaidEntity;
import net.shiroha233.roadweaverpg.entity.npc.ShopMaidEntity;

/**
 * Fabric 端实体注册
 */
public class ModEntitiesFabric {
    
    // 公会女仆实体类型
    public static final EntityType<GuildMaidEntityFabric> GUILD_MAID = EntityType.Builder
            .<GuildMaidEntityFabric>of(GuildMaidEntityFabric::new, MobCategory.MISC)
            .sized(0.6f, 1.5f)
            .clientTrackingRange(10)
            .build(ModEntities.GUILD_MAID_ID);
    
    // 商店女仆实体类型
    public static final EntityType<ShopMaidEntityFabric> SHOP_MAID = EntityType.Builder
            .<ShopMaidEntityFabric>of(ShopMaidEntityFabric::new, MobCategory.MISC)
            .sized(0.6f, 1.5f)
            .clientTrackingRange(10)
            .build(ModEntities.SHOP_MAID_ID);
    
    public static void register() {
        RoadWeaverRPG.LOGGER.info("Registering entities for Fabric...");
        
        Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ModEntities.GUILD_MAID_LOCATION,
                GUILD_MAID
        );
        
        Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ModEntities.SHOP_MAID_LOCATION,
                SHOP_MAID
        );
        
        FabricDefaultAttributeRegistry.register(GUILD_MAID, GuildMaidEntity.createNPCAttributes());
        FabricDefaultAttributeRegistry.register(SHOP_MAID, ShopMaidEntity.createNPCAttributes());
        
        RoadWeaverRPG.LOGGER.info("Entity registration complete!");
    }
}
