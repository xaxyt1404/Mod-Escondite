package com.yessenia.escondite.game;

import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.item.ItemFactory;
import com.yessenia.escondite.network.SkinNetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private static final Map<String, ArenaSession> sesiones = new HashMap<>();
    private static final Map<UUID, String> jugadorArena = new HashMap<>();

    public static ArenaSession getSesion(String arenaId) {
        ArenaData data = ConfigManager.get().arenas.get(arenaId.toLowerCase());
        if (data == null) return null;
        return sesiones.computeIfAbsent(data.id, k -> new ArenaSession(data));
    }

    public static ArenaSession getSesionDeJugador(UUID uuid) {
        String id = jugadorArena.get(uuid);
        return id != null ? sesiones.get(id) : null;
    }

    public static void entrarArena(ServerPlayer player, String arenaId, MinecraftServer server) {
        ArenaData data = ConfigManager.get().arenas.get(arenaId.toLowerCase());
        if (data == null || !data.habilitada) {
            player.displayClientMessage(Component.literal("§cEsta arena no está disponible."), true);
            return;
        }
        salirArenaActual(player, server, false);

        ArenaSession sesion = getSesion(data.id);
        jugadorArena.put(player.getUUID(), data.id);
        sesion.agregarJugador(player, server);
    }

    public static void salirArenaActual(ServerPlayer player, MinecraftServer server, boolean desconexion) {
        UUID uuid = player.getUUID();
        String arenaId = jugadorArena.remove(uuid);
        if (arenaId != null) {
            ArenaSession sesion = sesiones.get(arenaId);
            if (sesion != null) {
                sesion.removerJugador(player, server, desconexion);
            }
        }
    }

    public static void enviarALobbyGeneral(ServerPlayer player, MinecraftServer server) {
        salirArenaActual(player, server, false);
        ArenaSession.actualizarEquipoVisual(player, PlayerRole.NINGUNO, server);
        player.setCamera(player);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.removeAllEffects();

        if (ConfigManager.get().lobbyGeneral != null) {
            ConfigManager.get().lobbyGeneral.teleport(player, server);
        }
        player.getInventory().setItem(0, ItemFactory.crearLibroArenas());
    }

    public static void alEntrarServidor(ServerPlayer player, MinecraftServer server) {
        SkinNetworkManager.sincronizarTodoAJugador(player);
        // Verificar si estaba desconectado dentro de su minuto de gracia en alguna sesión
        for (ArenaSession sesion : sesiones.values()) {
            if (sesion.desconectadosTicks.containsKey(player.getUUID())) {
                jugadorArena.put(player.getUUID(), sesion.data.id);
                sesion.agregarJugador(player, server);
                return;
            }
        }
        enviarALobbyGeneral(player, server);
    }

    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            if (overworld.getDayTime() != 6000L) {
                overworld.setDayTime(6000L);
            }
            if (overworld.isRaining() || overworld.isThundering()) {
                overworld.setWeatherParameters(6000, 0, false, false);
            }
            if (!server.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
                server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true, server);
            }
        }

        for (ArenaSession sesion : sesiones.values()) {
            sesion.tick(server);
        }
    }

    public static void eliminarSesion(String arenaId, MinecraftServer server) {
        ArenaSession sesion = sesiones.remove(arenaId.toLowerCase());
        if (sesion != null) {
            for (UUID id : new java.util.ArrayList<>(sesion.jugadores)) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    enviarALobbyGeneral(p, server);
                }
            }
            sesion.reiniciarSalaCompleta();
        }
    }
}