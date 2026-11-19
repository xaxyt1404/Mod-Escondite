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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
                                                                                    String id = StringArgumentType.getString(ctx, "id");
                                                                                    String nombre = StringArgumentType.getString(ctx, "nombre");
                                                                                    String bloque = StringArgumentType.getString(ctx, "bloque");
                                                                                    String skin = StringArgumentType.getString(ctx, "skin");
                                                                                    int ancho = IntegerArgumentType.getInteger(ctx, "ancho");
                                                                                    int alto = IntegerArgumentType.getInteger(ctx, "alto");
                                                                                    int largo = IntegerArgumentType.getInteger(ctx, "largo");

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

                                            // Actualizar en vivo el Libro de Arenas en el inventario de los jugadores
                                            for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                                if (GameManager.getSesionDeJugador(p.getUUID()) == null) {
                                                    ItemStack nuevoLibro = ItemFactory.crearLibroArenas();
                                                    boolean reemplazado = false;
                                                    for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                                                        if ("libro_arenas".equals(ItemFactory.getTipoItem(p.getInventory().getItem(i)))) {
                                                            p.getInventory().setItem(i, nuevoLibro);
                                                            reemplazado = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!reemplazado && !p.isCreative()) {
                                                        p.getInventory().setItem(0, nuevoLibro);
                                                    }
                                                    p.containerMenu.broadcastFullState();
                                                }
                                            }

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

                                    // 4. Actualizar Libro de Arenas en vivo a quienes estén en el Lobby
                                    for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                        if (GameManager.getSesionDeJugador(p.getUUID()) == null) {
                                            ItemStack nuevoLibro = ItemFactory.crearLibroArenas();
                                            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                                                if ("libro_arenas".equals(ItemFactory.getTipoItem(p.getInventory().getItem(i)))) {
                                                    p.getInventory().setItem(i, nuevoLibro);
                                                    break;
                                                }
                                            }
                                            p.containerMenu.broadcastFullState();
                                        }
                                    }

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
}