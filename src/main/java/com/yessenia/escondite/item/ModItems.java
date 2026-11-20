package com.yessenia.escondite.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class ModItems {

    public static final Item ORBE_PREPARACION = registrar("orbe_preparacion",
            new Item(new Item.Properties().stacksTo(1)));

    public static final Item BACULO_ANFITRION = registrar("baculo_anfitrion",
            new Item(new Item.Properties().stacksTo(1)));

    public static final Item MARTILLO_JUEZ = registrar("martillo_juez",
            new Item(new Item.Properties().stacksTo(1)));

    public static final Item PUERTA_HOGAR = registrar("puerta_hogar",
            new Item(new Item.Properties().stacksTo(1)));

    // 1.0 base + 6.0 modificador = 7.0 de daño por golpe (3 golpes exactos)
    public static final Item CASTIGO = registrar("castigo",
            new Item(new Item.Properties().stacksTo(1).attributes(
                    ItemAttributeModifiers.builder()
                            .add(Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .add(Attributes.ATTACK_SPEED,
                                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                                    EquipmentSlotGroup.MAINHAND)
                            .build()
            )));

    public static final Item PALITROQUE = registrar("palitroque",
            new Item(new Item.Properties().stacksTo(1)));

    public static final Item GESTOR_ARENAS = registrar("gestor_arenas",
            new Item(new Item.Properties().stacksTo(1)));

    private static Item registrar(String nombre, Item item) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("escondite", nombre);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void register() {
        // Fuerza la carga estática de los ítems
    }
}