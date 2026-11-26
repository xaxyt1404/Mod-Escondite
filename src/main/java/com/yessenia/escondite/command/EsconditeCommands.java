package com.yessenia.escondite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.config.PosData;
import com.yessenia.escondite.entity.PortalManager;
import com.yessenia.escondite.game.ArenaBuilder;
import com.yessenia.escondite.game.GameManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class EsconditeCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hideandseek")
                .then(Commands.literal("setlobby")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ConfigManager.get().lobbyGeneral = PosData.fromPlayer(player);
                            ConfigManager.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Main lobby set at your current position."), true);
                            return 1;
                        }))
                .then(Commands.literal("createarena")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("block", StringArgumentType.string())
                                                .then(Commands.argument("skin", StringArgumentType.word())
                                                        .then(Commands.argument("width", IntegerArgumentType.integer(20, 200))
                                                                .then(Commands.argument("height", IntegerArgumentType.integer(10, 100))
                                                                        .then(Commands.argument("length", IntegerArgumentType.integer(20, 200))
                                                                                .executes(ctx -> {
                                                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                                                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                                                                    String block = StringArgumentType.getString(ctx, "block").trim();
                                                                                    String skin = StringArgumentType.getString(ctx, "skin").trim();
                                                                                    int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                                    int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                                    int length = IntegerArgumentType.getInteger(ctx, "length");

                                                                                    if (ConfigManager.get().arenas.containsKey(id)) {
                                                                                        ctx.getSource().sendFailure(Component.literal("§c[Escondite] An arena with ID '" + id + "' already exists. Use another ID or delete it first with /hideandseek deletearena " + id));
                                                                                        return 0;
                                                                                    }

                                                                                    String newBlockId = normalizeBlockId(block);

                                                                                    for (ArenaData existing : ConfigManager.get().arenas.values()) {
                                                                                        if (existing.nombreDisplay != null && existing.nombreDisplay.equalsIgnoreCase(name)) {
                                                                                            ctx.getSource().sendFailure(Component.literal("§c[Escondite] An arena named '" + existing.nombreDisplay + "' (ID: " + existing.id + ") already exists."));
                                                                                            return 0;
                                                                                        }
                                                                                        if (existing.bloqueBase != null && normalizeBlockId(existing.bloqueBase).equals(newBlockId)) {
                                                                                            ctx.getSource().sendFailure(Component.literal("§c[Escondite] An arena using block '" + block + "' already exists (Arena: " + existing.nombreDisplay + ", ID: " + existing.id + ")."));
                                                                                            return 0;
                                                                                        }
                                                                                    }

                                                                                    ArenaData arena = ArenaBuilder.crearArena(player.serverLevel(), id, name, block, skin, width, height, length);
                                                                                    arena.spawnArena.teleport(player, player.getServer());
                                                                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Arena '" + name + "' generated. Use /hideandseek enable " + id + " true once you finish decorating it."), true);
                                                                                    return 1;
                                                                                })))))))))
                .then(Commands.literal("setspawn")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                            String type = StringArgumentType.getString(ctx, "type").toLowerCase();
                                            ArenaData arena = ConfigManager.get().arenas.get(id);
                                            if (arena == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cArena not found."));
                                                return 0;
                                            }
                                            PosData pos = PosData.fromPlayer(player);
                                            switch (type) {
                                                case "arena" -> arena.spawnArena = pos;
                                                case "sublobby" -> arena.spawnSubLobby = pos;
                                                case "seeker" -> arena.spawnEsperaBuscador = pos;
                                                default -> {
                                                    ctx.getSource().sendFailure(Component.literal("§cInvalid type. Use: arena, sublobby, or seeker."));
                                                    return 0;
                                                }
                                            }
                                            ConfigManager.save();
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Spawn '" + type + "' updated for " + arena.nombreDisplay), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("setportal")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                    ArenaData arena = ConfigManager.get().arenas.get(id);
                                    if (arena == null) {
                                        ctx.getSource().sendFailure(Component.literal("§cArena '" + id + "' does not exist."));
                                        return 0;
                                    }
                                    PosData pos = PosData.fromPlayer(player);
                                    pos.y += 0.25;
                                    arena.posPortalLobby = pos;
                                    ConfigManager.save();
                                    PortalManager.sincronizarPortales(player.getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Floating portal for §e" + arena.nombreDisplay + " §aset at your position."), true);
                                    return 1;
                                })))
                .then(Commands.literal("removeportal")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                    ArenaData arena = ConfigManager.get().arenas.get(id);
                                    if (arena == null) {
                                        ctx.getSource().sendFailure(Component.literal("§cArena '" + id + "' does not exist."));
                                        return 0;
                                    }
                                    arena.posPortalLobby = null;
                                    ConfigManager.save();
                                    PortalManager.eliminarPortal(id, ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§e[Escondite] Floating portal for §f" + arena.nombreDisplay + " §eremoved."), true);
                                    return 1;
                                })))
                .then(Commands.literal("enable")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            ArenaData arena = ConfigManager.get().arenas.get(id);
                                            if (arena == null) {
                                                ctx.getSource().sendFailure(Component.literal("§cArena not found."));
                                                return 0;
                                            }
                                            arena.habilitada = enabled;
                                            ConfigManager.save();
                                            PortalManager.sincronizarPortales(ctx.getSource().getServer());

                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Arena '" + arena.nombreDisplay + "' enabled: " + enabled), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("join")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                    GameManager.entrarArena(player, id, player.getServer());
                                    return 1;
                                })))
                .then(Commands.literal("deletearena")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id").toLowerCase();
                                    ArenaData arena = ConfigManager.get().arenas.get(id);
                                    if (arena == null) {
                                        ctx.getSource().sendFailure(Component.literal("§cArena not found."));
                                        return 0;
                                    }

                                    // 1. Remove players and close active session if present
                                    GameManager.eliminarSesion(id, ctx.getSource().getServer());

                                    // 2. Remove floating portal entity if present
                                    PortalManager.eliminarPortal(id, ctx.getSource().getServer());

                                    // 3. Clear structure blocks in the world
                                    ArenaBuilder.eliminarEstructuraArena(ctx.getSource().getLevel(), arena);

                                    // 4. Remove from JSON and save
                                    ConfigManager.get().arenas.remove(id);
                                    ConfigManager.save();

                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Escondite] Arena '" + arena.nombreDisplay + "' completely deleted."), true);
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

    private static String normalizeBlockId(String blockRaw) {
        ResourceLocation rl = ResourceLocation.tryParse(blockRaw.toLowerCase());
        Block block = (rl != null) ? BuiltInRegistries.BLOCK.get(rl) : Blocks.AIR;
        if (block == Blocks.AIR) {
            block = Blocks.STONE;
        }
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}