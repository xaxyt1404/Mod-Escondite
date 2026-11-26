package com.yessenia.escondite.item;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class ItemFactory {

    public static ItemStack crearEtiquetaPreparacion(boolean listo) {
        ItemStack stack = new ItemStack(ModItems.ORBE_PREPARACION);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(listo ? 1 : 0));
        stack.set(DataComponents.ITEM_NAME, Component.literal(listo ? "§a§lPreparación: LISTO" : "§c§lPreparación: NO LISTO"));
        return stack;
    }

    public static ItemStack crearGalletaAnfitrion(boolean contando, boolean esEntreRondas) {
        ItemStack stack = new ItemStack(ModItems.BACULO_ANFITRION);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(contando ? 1 : 0));
        if (contando) {
            stack.set(DataComponents.ITEM_NAME, Component.literal("§c§lCancelar inicio"));
        } else {
            stack.set(DataComponents.ITEM_NAME, Component.literal(esEntreRondas ? "§e§lIniciar ronda" : "§e§lIniciar partida"));
        }
        return stack;
    }

    public static ItemStack crearPalaTerminar() {
        ItemStack stack = new ItemStack(ModItems.MARTILLO_JUEZ);
        stack.set(DataComponents.ITEM_NAME, Component.literal("§4§lTerminar partida"));
        return stack;
    }

    public static ItemStack crearPuertaSalir() {
        ItemStack stack = new ItemStack(ModItems.PUERTA_HOGAR);
        stack.set(DataComponents.ITEM_NAME, Component.literal("§c§lVolver a Inicio"));
        return stack;
    }

    public static ItemStack crearGestorArenas() {
        ItemStack stack = new ItemStack(ModItems.GESTOR_ARENAS);
        stack.set(DataComponents.ITEM_NAME, Component.literal("§b§lGestor de Arenas"));
        return stack;
    }

    public static void darKitBuscador(ServerPlayer player, RegistryAccess registries) {
        player.getInventory().clearContent();

        ItemStack castigo = new ItemStack(ModItems.CASTIGO);
        castigo.set(DataComponents.ITEM_NAME, Component.literal("§c§lCastigo"));
        Holder<Enchantment> fireAspect = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);
        castigo.enchant(fireAspect, 2);
        player.getInventory().setItem(0, castigo);

        ItemStack pocionLentitud = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SLOWNESS);
        pocionLentitud.setCount(3);
        pocionLentitud.set(DataComponents.ITEM_NAME, Component.literal("§9Poción de Lentitud"));
        player.getInventory().setItem(1, pocionLentitud);

        ItemStack pocionVeneno = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_POISON);
        pocionVeneno.setCount(3);
        pocionVeneno.set(DataComponents.ITEM_NAME, Component.literal("§2Poción de Veneno II"));
        player.getInventory().setItem(2, pocionVeneno);
    }

    public static void darKitEscondido(ServerPlayer player, RegistryAccess registries) {
        player.getInventory().clearContent();

        ItemStack palitroque = new ItemStack(ModItems.PALITROQUE);
        palitroque.set(DataComponents.ITEM_NAME, Component.literal("§a§lPalitroque"));
        Holder<Enchantment> knockback = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);
        palitroque.enchant(knockback, 3); // Empuje III
        player.getInventory().setItem(0, palitroque);

        ItemStack bolas = new ItemStack(Items.SNOWBALL, 16);
        bolas.set(DataComponents.ITEM_NAME, Component.literal("§fBolas de Nieve Infinitas"));
        player.getInventory().setItem(1, bolas);

        ItemStack cohetes = new ItemStack(Items.FIREWORK_ROCKET, 16);
        cohetes.set(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
        cohetes.set(DataComponents.ITEM_NAME, Component.literal("§dCohetes Infinitos"));
        player.getInventory().setItem(2, cohetes);
    }

    public static String getTipoItem(ItemStack stack) {
        if (stack.isEmpty()) return "";
        if (stack.is(ModItems.ORBE_PREPARACION)) return "preparacion";
        if (stack.is(ModItems.BACULO_ANFITRION)) return "anfitrion";
        if (stack.is(ModItems.MARTILLO_JUEZ)) return "terminar";
        if (stack.is(ModItems.PUERTA_HOGAR)) return "salir";
        if (stack.is(ModItems.GESTOR_ARENAS)) return "gestor";
        return "";
    }
}