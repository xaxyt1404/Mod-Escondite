package com.yessenia.escondite.network;

import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.config.PosData;
import com.yessenia.escondite.entity.PortalManager;
import com.yessenia.escondite.game.ArenaBuilder;
import com.yessenia.escondite.game.GameManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class AdminNetworkManager {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(OpenGestorPayload.TYPE, OpenGestorPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionPayload.TYPE, AdminActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HudSyncPayload.TYPE, HudSyncPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            server.execute(() -> {
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.literal("§c[Escondite] No tienes permisos de administrador."));
                    return;
                }
                procesarAccion(player, server, payload);
            });
        });
    }

    public static void abrirVentana(ServerPlayer player) {
        if (!player.hasPermissions(2)) return;
        List<AdminArenaInfo> lista = new ArrayList<>();
        for (ArenaData a : ConfigManager.get().arenas.values()) {
            lista.add(new AdminArenaInfo(
                    a.id,
                    a.nombreDisplay != null ? a.nombreDisplay : a.id,
                    a.bloqueBase != null ? a.bloqueBase : "minecraft:stone",
                    a.skinEscondido != null ? a.skinEscondido : "stone",
                    a.habilitada,
                    a.posPortalLobby != null
            ));
        }
        boolean tieneLobby = ConfigManager.get().lobbyGeneral != null;
        ServerPlayNetworking.send(player, new OpenGestorPayload(tieneLobby, lista));
    }

    private static void procesarAccion(ServerPlayer player, MinecraftServer server, AdminActionPayload p) {
        String id = p.id().toLowerCase().trim();

        switch (p.accion()) {
            case "CREAR" -> {
                String nombre = p.nombre().trim();
                String bloque = p.bloque().trim();
                String skin = p.skin().trim();

                if (id.isEmpty() || nombre.isEmpty() || bloque.isEmpty() || skin.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§c[Escondite] Completa todos los campos para crear la arena."));
                    return;
                }
                if (ConfigManager.get().arenas.containsKey(id)) {
                    player.sendSystemMessage(Component.literal("§c[Escondite] Ya existe una arena con el ID '" + id + "'."));
                    return;
                }

                String bloqueNorm = normalizarBloqueId(bloque);
                for (ArenaData existente : ConfigManager.get().arenas.values()) {
                    if (existente.nombreDisplay != null && existente.nombreDisplay.equalsIgnoreCase(nombre)) {
                        player.sendSystemMessage(Component.literal("§c[Escondite] Ya existe una arena llamada '" + nombre + "'."));
                        return;
                    }
                    if (existente.bloqueBase != null && normalizarBloqueId(existente.bloqueBase).equals(bloqueNorm)) {
                        player.sendSystemMessage(Component.literal("§c[Escondite] Ya existe una arena con ese bloque base."));
                        return;
                    }
                }

                int ancho = Math.max(20, Math.min(200, p.ancho()));
                int alto = Math.max(10, Math.min(100, p.alto()));
                int largo = Math.max(20, Math.min(200, p.largo()));

                ArenaData nueva = ArenaBuilder.crearArena(player.serverLevel(), id, nombre, bloque, skin, ancho, alto, largo);
                nueva.spawnArena.teleport(player, server);
                player.sendSystemMessage(Component.literal("§a[Escondite] Arena '" + nombre + "' creada y teletransportado a su interior."));
            }

            case "EDITAR" -> {
                ArenaData arena = ConfigManager.get().arenas.get(id);
                if (arena == null) return;
                String nuevoNombre = p.nombre().trim();
                String nuevoBloque = p.bloque().trim();
                String nuevaSkin = p.skin().trim();

                if (!nuevoNombre.isEmpty()) arena.nombreDisplay = nuevoNombre;
                if (!nuevoBloque.isEmpty()) arena.bloqueBase = nuevoBloque;
                if (!nuevaSkin.isEmpty()) arena.skinEscondido = nuevaSkin;

                ConfigManager.save();
                PortalManager.sincronizarPortales(server);
                player.sendSystemMessage(Component.literal("§a[Escondite] Cambios guardados para '" + arena.nombreDisplay + "'."));
                abrirVentana(player);
            }

            case "TOGGLE_HABILITAR" -> {
                ArenaData arena = ConfigManager.get().arenas.get(id);
                if (arena == null) return;
                arena.habilitada = !arena.habilitada;
                ConfigManager.save();
                PortalManager.sincronizarPortales(server);
                player.sendSystemMessage(Component.literal("§a[Escondite] Arena '" + arena.nombreDisplay + "' -> " + (arena.habilitada ? "§aHabilitada" : "§cDeshabilitada")));
                abrirVentana(player);
            }

            case "TP_ARENA" -> {
                ArenaData arena = ConfigManager.get().arenas.get(id);
                if (arena != null && arena.spawnArena != null) {
                    arena.spawnArena.teleport(player, server);
                    player.sendSystemMessage(Component.literal("§a[Escondite] Teletransportado a '" + arena.nombreDisplay + "'."));
                }
            }

            case "FIJAR_LOBBY_GENERAL" -> {
                ConfigManager.get().lobbyGeneral = PosData.fromPlayer(player);
                ConfigManager.save();
                player.sendSystemMessage(Component.literal("§a[Escondite] Lobby General fijado en tu posición."));
                abrirVentana(player);
            }

            case "SPAWN_ARENA", "SPAWN_SUBLOBBY", "SPAWN_BUSCADOR", "FIJAR_PORTAL", "QUITAR_PORTAL" -> {
                ArenaData arena = ConfigManager.get().arenas.get(id);
                if (arena == null) return;
                PosData pos = PosData.fromPlayer(player);

                switch (p.accion()) {
                    case "SPAWN_ARENA" -> arena.spawnArena = pos;
                    case "SPAWN_SUBLOBBY" -> arena.spawnSubLobby = pos;
                    case "SPAWN_BUSCADOR" -> arena.spawnEsperaBuscador = pos;
                    case "FIJAR_PORTAL" -> {
                        pos.y += 0.25;
                        arena.posPortalLobby = pos;
                        PortalManager.sincronizarPortales(server);
                    }
                    case "QUITAR_PORTAL" -> {
                        arena.posPortalLobby = null;
                        PortalManager.eliminarPortal(id, server);
                    }
                }
                ConfigManager.save();
                player.sendSystemMessage(Component.literal("§a[Escondite] Punto actualizado para '" + arena.nombreDisplay + "'."));
                abrirVentana(player);
            }

            case "ELIMINAR" -> {
                ArenaData arena = ConfigManager.get().arenas.get(id);
                if (arena == null) {
                    player.sendSystemMessage(Component.literal("§c[Escondite] No se encontró la arena '" + id + "'."));
                    return;
                }
                GameManager.eliminarSesion(id, server);
                PortalManager.eliminarPortal(id, server);

                net.minecraft.server.level.ServerLevel nivelArena = player.serverLevel();
                if (arena.spawnArena != null && arena.spawnArena.dimension != null) {
                    var key = net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            ResourceLocation.parse(arena.spawnArena.dimension)
                    );
                    var encontrado = server.getLevel(key);
                    if (encontrado != null) nivelArena = encontrado;
                }

                ArenaBuilder.eliminarEstructuraArena(nivelArena, arena);
                ConfigManager.get().arenas.remove(id);
                ConfigManager.save();
                player.sendSystemMessage(Component.literal("§e[Escondite] Arena '" + arena.nombreDisplay + "' eliminada por completo."));
                abrirVentana(player);
            }
        }
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