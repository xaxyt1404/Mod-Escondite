package com.yessenia.escondite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.config.PosData;
import com.yessenia.escondite.game.ArenaBuilder;
import com.yessenia.escondite.game.GameManager;
import com.yessenia.escondite.item.ItemFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class EsconditeCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("escondite")
                .then(Commands.literal("fijarlobby")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ConfigManager.get().lobbyGeneral = PosData.fromPlayer(player);
                            ConfigManager.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Lobby General fijado en tu posición."), true);
                            return 1;
                        }))
                .then(Commands.literal("creararena")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("nombre", StringArgumentType.string())
                                        .then(Commands.argument("bloque", StringArgumentType.string())
                                                .then(Commands.argument("skin", StringArgumentType.word())
                                                        .then(Commands.argument("ancho", IntegerArgumentType.integer(20, 200))
                                                                .then(Commands.argument("alto", IntegerArgumentType.integer(10, 100))
                                                                        .then(Commands.argument("largo", IntegerArgumentType.integer(20, 200))
                                                                                .executes(ctx -> {
                                                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                                                                    String nombre = StringArgumentType.getString(ctx, "nombre").trim();
                                                                                    String bloque = StringArgumentType.getString(ctx, "bloque").trim();
                                                                                    String skin = StringArgumentType.getString(ctx, "skin").trim();
                                                                                    int ancho = IntegerArgumentType.getInteger(ctx, "ancho");
                                                                                    int alto = IntegerArgumentType.getInteger(ctx, "alto");
                                                                                    int largo = IntegerArgumentType.getInteger(ctx, "largo");

                                                                                    // 1. Validar que el ID no exista ya
                                                                                    if (ConfigManager.get().arenas.containsKey(id)) {
                                                                                        ctx.getSource().sendFailure(Component.literal("§c[Escondite] Ya existe una arena con el ID '" + id + "'. Usa otro ID o elimínala primero con /escondite eliminararena " + id));
                                                                                        return 0;
                                                                                    }

                                                                                    String bloqueNuevoId = normalizarBloqueId(bloque);

                                                                                    // 2 y 3. Validar que ni el nombre visible ni el bloque base estén repetidos
                                                                                    for (ArenaData existente : ConfigManager.get().arenas.values()) {
                                                                                        if (existente.nombreDisplay != null && existente.nombreDisplay.equalsIgnoreCase(nombre)) {
                                                                                            ctx.getSource().sendFailure(Component.literal("§c[Escondite] Ya existe una arena llamada '" + existente.nombreDisplay + "' (ID: " + existente.id + ")."));
                                                                                            return 0;
                                                                                        }
                                                                                        if (existente.bloqueBase != null && normalizarBloqueId(existente.bloqueBase).equals(bloqueNuevoId)) {
                                                                                            ctx.getSource().sendFailure(Component.literal("§c[Escondite] Ya existe una arena con el bloque '" + bloque + "' (Arena: " + existente.nombreDisplay + ", ID: " + existente.id + ")."));
                                                                                            return 0;
                                                                                        }
                                                                                    }

                                                                                    ArenaData arena = ArenaBuilder.crearArena(player.serverLevel(), id, nombre, bloque, skin, ancho, alto, largo);
                                                                                    arena.spawnArena.teleport(player, player.getServer());
                                                                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Arena '" + nombre + "' generada. Usa /escondite habilitar " + id + " true cuando termines de decorarla."), true);
                                                                                    return 1;
                                                                                })))))))))
                .then(Commands.literal("fijarspawn")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("tipo", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                            String tipo = StringArgumentType.getString(ctx, "tipo").toLowerCase();
                                            ArenaData arena = ConfigManager.get().arenas.get(id);
                                            if (arena == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cArena no encontrada."));
                                                return 0;
                                            }
                                            PosData pos = PosData.fromPlayer(player);
                                            switch (tipo) {
                                                case "arena" -> arena.spawnArena = pos;
                                                case "sublobby" -> arena.spawnSubLobby = pos;
                                                case "buscador" -> arena.spawnEsperaBuscador = pos;
                                                default -> {
                                                    ctx.getSource().sendFailure(Component.literal("§cTipo inválido. Usa: arena, sublobby o buscador."));
                                                    return 0;
                                                }
                                            }
                                            ConfigManager.save();
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Spawn '" + tipo + "' actualizado para " + arena.nombreDisplay), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("habilitar")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("estado", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                            boolean estado = BoolArgumentType.getBool(ctx, "estado");
                                            ArenaData arena = ConfigManager.get().arenas.get(id);
                                            if (arena == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cArena no encontrada."));
                                                return 0;
                                            }
                                            arena.habilitada = estado;
                                            ConfigManager.save();

                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Arena '" + arena.nombreDisplay + "' habilitada: " + estado), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("entrar")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                    GameManager.entrarArena(player, id, player.getServer());
                                    return 1;
                                })))
                .then(Commands.literal("eliminararena")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                    ArenaData arena = ConfigManager.get().arenas.get(id);
                                    if (arena == null) {
                                        ctx.getSource().sendFailure(Component.literal("§cArena no encontrada."));
                                        return 0;
                                    }

                                    // 1. Sacar jugadores y cerrar sesión activa si existe
                                    GameManager.eliminarSesion(id, ctx.getSource().getServer());

                                    // 2. Limpiar bloques de la estructura en el mundo
                                    ArenaBuilder.eliminarEstructuraArena(ctx.getSource().getLevel(), arena);

                                    // 3. Borrar del JSON y guardar
                                    ConfigManager.get().arenas.remove(id);
                                    ConfigManager.save();

                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Arena '" + arena.nombreDisplay + "' eliminada por completo."), true);
                                    return 1;
                                })))
        );

        dispatcher.register(Commands.literal("lobby")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GameManager.enviarALobbyGeneral(player, player.getServer());
                    return 1;
                }));
    }

    private static String normalizarBloqueId(String bloqueRaw) {
        ResourceLocation rl = ResourceLocation.tryParse(bloqueRaw.toLowerCase());
        Block bloque = (rl != null) ? BuiltInRegistries.BLOCK.get(rl) : Blocks.AIR;
        if (bloque == Blocks.AIR) {
            bloque = Blocks.STONE;
        }
        return BuiltInRegistries.BLOCK.getKey(bloque).toString();
    }
}