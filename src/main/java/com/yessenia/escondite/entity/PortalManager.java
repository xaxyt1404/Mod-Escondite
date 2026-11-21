package com.yessenia.escondite.entity;

import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.game.ArenaSession;
import com.yessenia.escondite.game.GameManager;
import com.yessenia.escondite.game.GameState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalManager {

    public static void sincronizarPortales(MinecraftServer server) {
        if (server == null) return;

        // Agrupar entidades de portal existentes por arenaId y eliminar duplicados
        Map<String, PortalArenaEntity> existentes = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends PortalArenaEntity> enMundo = level.getEntities(ModEntities.PORTAL_ARENA, e -> true);
            for (PortalArenaEntity entidad : enMundo) {
                String id = entidad.getArenaId();
                ArenaData data = ConfigManager.get().arenas.get(id);
                if (data == null || data.posPortalLobby == null || existentes.containsKey(id)) {
                    entidad.discard();
                } else {
                    existentes.put(id, entidad);
                }
            }
        }

        // Crear o actualizar cada portal configurado
        for (ArenaData arena : ConfigManager.get().arenas.values()) {
            if (arena.posPortalLobby == null) continue;

            ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(arena.posPortalLobby.dimension)
            );
            ServerLevel targetLevel = server.getLevel(dimKey);
            if (targetLevel == null) continue;

            PortalArenaEntity portal = existentes.get(arena.id);
            if (portal == null || portal.level() != targetLevel) {
                if (portal != null) portal.discard();
                portal = new PortalArenaEntity(ModEntities.PORTAL_ARENA, targetLevel);
                portal.moveTo(arena.posPortalLobby.x, arena.posPortalLobby.y, arena.posPortalLobby.z, 0.0f, 0.0f);
                targetLevel.addFreshEntity(portal);
            } else {
                portal.moveTo(arena.posPortalLobby.x, arena.posPortalLobby.y, arena.posPortalLobby.z, 0.0f, 0.0f);
            }

            portal.setDatosPortal(arena.id, arena.nombreDisplay, arena.bloqueBase, construirTextoEstado(arena));
        }
    }

    public static void eliminarPortal(String arenaId, MinecraftServer server) {
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (PortalArenaEntity entidad : level.getEntities(ModEntities.PORTAL_ARENA, e -> arenaId.equalsIgnoreCase(e.getArenaId()))) {
                entidad.discard();
            }
        }
    }

    private static String construirTextoEstado(ArenaData arena) {
        if (!arena.habilitada) {
            return "§cDeshabilitada";
        }
        ArenaSession sesion = GameManager.getSesion(arena.id);
        int cantJugadores = (sesion != null) ? sesion.jugadores.size() : 0;
        boolean enCurso = (sesion != null && (sesion.estado == GameState.ESCONDIENDOSE || sesion.estado == GameState.BUSCANDO));

        if (enCurso) {
            return "§eEn partida §7(" + cantJugadores + " jug.)";
        }
        return "§aDisponible §7(" + cantJugadores + " jug.)";
    }
}