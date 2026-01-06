package net.shiroha233.roadweaverpg.worldgen;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.shiroha233.roadweaverpg.RoadWeaverRPG;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class VillagePoolInjector {
    
    private static final String GUILDHALL_TEMPLATE = "roadweaver_rpg:guildhall";
    private static final int GUILDHALL_WEIGHT = 50;
    
    private static final List<ResourceLocation> VANILLA_CENTER_POOLS = List.of(
        new ResourceLocation("minecraft", "village/plains/town_centers"),
        new ResourceLocation("minecraft", "village/desert/town_centers"),
        new ResourceLocation("minecraft", "village/savanna/town_centers"),
        new ResourceLocation("minecraft", "village/snowy/town_centers"),
        new ResourceLocation("minecraft", "village/taiga/town_centers")
    );
    
    private static final List<ResourceLocation> CTOV_CENTER_POOLS = List.of(
        new ResourceLocation("ctov", "village/plains/town_centers"),
        new ResourceLocation("ctov", "village/plains_fortified/town_centers"),
        new ResourceLocation("ctov", "village/desert/town_centers"),
        new ResourceLocation("ctov", "village/desert_oasis/town_centers"),
        new ResourceLocation("ctov", "village/savanna/town_centers"),
        new ResourceLocation("ctov", "village/savanna_na/town_centers"),
        new ResourceLocation("ctov", "village/snowy_igloo/town_centers"),
        new ResourceLocation("ctov", "village/taiga/town_centers"),
        new ResourceLocation("ctov", "village/taiga_fortified/town_centers"),
        new ResourceLocation("ctov", "village/beach/town_centers"),
        new ResourceLocation("ctov", "village/jungle/town_centers"),
        new ResourceLocation("ctov", "village/jungle_tree/town_centers"),
        new ResourceLocation("ctov", "village/mesa/town_centers"),
        new ResourceLocation("ctov", "village/mesa_fortified/town_centers"),
        new ResourceLocation("ctov", "village/mountain/town_centers"),
        new ResourceLocation("ctov", "village/mountain_alpine/town_centers"),
        new ResourceLocation("ctov", "village/swamp/town_centers"),
        new ResourceLocation("ctov", "village/swamp_fortified/town_centers"),
        new ResourceLocation("ctov", "village/mushroom/town_centers")
    );
    
    // 反射字段缓存
    private static Field rawTemplatesField;
    private static Field templatesField;
    
    static {
        try {
            rawTemplatesField = StructureTemplatePool.class.getDeclaredField("rawTemplates");
            rawTemplatesField.setAccessible(true);
            
            templatesField = StructureTemplatePool.class.getDeclaredField("templates");
            templatesField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            RoadWeaverRPG.LOGGER.error("Failed to get StructureTemplatePool fields via reflection", e);
        }
    }
    
    public static void injectGuildhallToVillages(MinecraftServer server) {
        Registry<StructureTemplatePool> poolRegistry = server.registryAccess()
            .registryOrThrow(Registries.TEMPLATE_POOL);
        
        for (ResourceLocation poolLocation : VANILLA_CENTER_POOLS) {
            injectToPool(poolRegistry, poolLocation);
        }
        
        for (ResourceLocation poolLocation : CTOV_CENTER_POOLS) {
            injectToPool(poolRegistry, poolLocation);
        }
    }
    
    private static boolean injectToPool(Registry<StructureTemplatePool> poolRegistry, ResourceLocation poolLocation) {
        ResourceKey<StructureTemplatePool> poolKey = ResourceKey.create(
            Registries.TEMPLATE_POOL, poolLocation
        );
        
        var holderOpt = poolRegistry.getHolder(poolKey);
        if (holderOpt.isPresent()) {
            StructureTemplatePool pool = holderOpt.get().value();
            return injectGuildhallToPool(pool);
        }
        return false;
    }
    
    private static boolean injectGuildhallToPool(StructureTemplatePool pool) {
        try {
            StructurePoolElement guildhallElement = StructurePoolElement.legacy(GUILDHALL_TEMPLATE)
                .apply(StructureTemplatePool.Projection.RIGID);
            
            // 使用反射修改 rawTemplates
            @SuppressWarnings("unchecked")
            List<Pair<StructurePoolElement, Integer>> oldRawTemplates = 
                (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);
            List<Pair<StructurePoolElement, Integer>> newRawTemplates = new ArrayList<>(oldRawTemplates);
            newRawTemplates.add(Pair.of(guildhallElement, GUILDHALL_WEIGHT));
            rawTemplatesField.set(pool, newRawTemplates);
            
            // 使用反射修改 templates
            @SuppressWarnings("unchecked")
            ObjectArrayList<StructurePoolElement> oldTemplates = 
                (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);
            ObjectArrayList<StructurePoolElement> newTemplates = new ObjectArrayList<>(oldTemplates);
            for (int i = 0; i < GUILDHALL_WEIGHT; i++) {
                newTemplates.add(guildhallElement);
            }
            templatesField.set(pool, newTemplates);
            
            return true;
        } catch (Exception e) {
            RoadWeaverRPG.LOGGER.error("Failed to inject guildhall to pool", e);
            return false;
        }
    }
}
