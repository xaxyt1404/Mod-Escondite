package com.yessenia.escondite.item;

import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    public static final String TAG_TIPO_ITEM = "escondite_item";

    private static ItemStack marcarItem(ItemStack stack, String tipo, Component nombre) {
        stack.set(DataComponents.CUSTOM_NAME, nombre);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TIPO_ITEM, tipo);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static String getTipoItem(ItemStack stack) {
        if (stack.isEmpty()) return "";
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";
        return customData.copyTag().getString(TAG_TIPO_ITEM);
    }

    public static ItemStack crearLibroArenas() {
        ItemStack libro = new ItemStack(Items.WRITTEN_BOOK);

        MutableComponent pagina = Component.literal("=== ESCONDITE ===\n")
                .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD)
                .append(Component.literal("Selecciona una arena:\n\n").withStyle(ChatFormatting.BLACK));

        boolean hayArenas = false;
        for (ArenaData arena : ConfigManager.get().arenas.values()) {
            if (!arena.habilitada) continue;
            hayArenas = true;

            MutableComponent linea = Component.literal(" ➤ " + arena.nombreDisplay + "\n")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.DARK_GREEN)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/escondite entrar " + arena.id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Clic para entrar a " + arena.nombreDisplay)))
                    );
            pagina.append(linea);
        }

        if (!hayArenas) {
            pagina.append(Component.literal("No hay arenas habilitadas aún.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        List<Filterable<Component>> paginas = new ArrayList<>();
        paginas.add(Filterable.passThrough(pagina));

        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("Libro de Arenas"),
                "Escondite",
                0,
                paginas,
                true
        );

        libro.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return marcarItem(libro, "libro_arenas", Component.literal("Libro de Arenas").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
    }

    public static ItemStack crearEtiquetaPreparacion(boolean listo) {
        Component nombre = listo
                ? Component.literal("Preparación: LISTO").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.literal("Preparación: NO LISTO").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        return marcarItem(new ItemStack(Items.NAME_TAG), "preparacion", nombre);
    }

    public static ItemStack crearGalletaAnfitrion(boolean contando, boolean entreRondas) {
        Component nombre;
        if (contando) {
            nombre = Component.literal("Cancelar Inicio").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else if (entreRondas) {
            nombre = Component.literal("Iniciar Ronda").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else {
            nombre = Component.literal("Iniciar Partida").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        }
        return marcarItem(new ItemStack(Items.COOKIE), "galleta_anfitrion", nombre);
    }

    public static ItemStack crearPuertaSalir() {
        return marcarItem(
                new ItemStack(Items.OAK_DOOR),
                "puerta_salir",
                Component.literal("Volver al lobby").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
        );
    }

    public static ItemStack crearPalaTerminar() {
        return marcarItem(
                new ItemStack(Items.IRON_SHOVEL),
                "pala_terminar",
                Component.literal("Terminar Partida").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );
    }

    // ==================== KITS DE JUEGO ====================

    public static void darKitBuscador(ServerPlayer player, RegistryAccess registries) {
        player.getInventory().clearContent();

        // 1. Palo "Castigo": 7.0 de daño (3 golpes exactos = 21 >= 20 HP) + Aspecto Ígneo II
        ItemStack castigo = new ItemStack(Items.STICK);
        marcarItem(castigo, "palo_castigo", Component.literal("Castigo").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        var enchantRegistry = registries.registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fireAspect = enchantRegistry.getHolderOrThrow(Enchantments.FIRE_ASPECT);
        castigo.enchant(fireAspect, 2);

        // Atributo base de daño de ataque = 6.0 (+ 1.0 base del jugador = 7.0 por golpe exacto)
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath("escondite", "castigo_damage"), 6.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
        castigo.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);

        // 2. 3 Pociones arrojadizas de Lentitud (8 segundos = 160 ticks)
        ItemStack pocionLentitud = new ItemStack(Items.SPLASH_POTION, 3);
        marcarItem(pocionLentitud, "pocion_lentitud", Component.literal("Poción de Lentitud (8s)").withStyle(ChatFormatting.AQUA));
        pocionLentitud.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                Optional.of(0x5A6C81),
                List.of(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1))
        ));

        // 3. 3 Pociones arrojadizas de Veneno II (5 segundos = 100 ticks, amplifier 1 = nivel II)
        ItemStack pocionVeneno = new ItemStack(Items.SPLASH_POTION, 3);
        marcarItem(pocionVeneno, "pocion_veneno", Component.literal("Poción de Veneno II (5s)").withStyle(ChatFormatting.DARK_GREEN));
        pocionVeneno.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                Optional.of(0x4E9331),
                List.of(new MobEffectInstance(MobEffects.POISON, 100, 1))
        ));

        player.getInventory().setItem(0, castigo);
        player.getInventory().setItem(1, pocionLentitud);
        player.getInventory().setItem(2, pocionVeneno);
    }

    public static void darKitEscondido(ServerPlayer player, RegistryAccess registries) {
        player.getInventory().clearContent();

        // 1. Palo "Palitroque": Empuje V
        ItemStack palitroque = new ItemStack(Items.STICK);
        marcarItem(palitroque, "palo_palitroque", Component.literal("Palitroque").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        var enchantRegistry = registries.registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> knockback = enchantRegistry.getHolderOrThrow(Enchantments.KNOCKBACK);
        palitroque.enchant(knockback, 3);

        // 2. Bolas de nieve infinitas (se reponen en cada lanzamiento)
        ItemStack nieve = new ItemStack(Items.SNOWBALL, 16);
        marcarItem(nieve, "nieve_infinita", Component.literal("Bolas de Nieve Infinitas").withStyle(ChatFormatting.WHITE));

        // 3. Cohetes de fuegos artificiales infinitos (se reponen tras cada uso)
        ItemStack cohetes = new ItemStack(Items.FIREWORK_ROCKET, 16);
        marcarItem(cohetes, "cohete_infinito", Component.literal("Cohetes Infinitos").withStyle(ChatFormatting.RED));

        player.getInventory().setItem(0, palitroque);
        player.getInventory().setItem(1, nieve);
        player.getInventory().setItem(2, cohetes);
    }
}