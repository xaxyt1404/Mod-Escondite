package com.yessenia.escondite.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final EntityType<PortalArenaEntity> PORTAL_ARENA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("escondite", "portal_arena"),
            EntityType.Builder.<PortalArenaEntity>of(PortalArenaEntity::new, MobCategory.MISC)
                    .sized(1.1f, 1.35f)
                    .clientTrackingRange(10)
                    .updateInterval(5)
                    .fireImmune()
                    .build("portal_arena")
    );

    public static void register() {
        // Fuerza la carga estática de la clase al iniciar el mod
    }
}