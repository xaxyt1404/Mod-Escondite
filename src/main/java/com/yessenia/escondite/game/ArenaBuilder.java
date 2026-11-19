package com.yessenia.escondite.game;

import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.config.PosData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ArenaBuilder {

    public static ArenaData crearArena(ServerLevel level, String id, String nombreDisplay, String bloqueId, String skinName, int ancho, int alto, int largo) {
        Block bloque = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(bloqueId));
        if (bloque == Blocks.AIR) {
            bloque = Blocks.STONE;
        }
        BlockState paredState = bloque.defaultBlockState();
        BlockState aireState = Blocks.AIR.defaultBlockState();
        BlockState barreraState = Blocks.BARRIER.defaultBlockState();
        BlockState luzState = Blocks.SEA_LANTERN.defaultBlockState();

        int x0 = ConfigManager.getSiguienteXInicio();
        int y0 = 64;
        // La pared frontal siempre queda alineada en z1 = 0; el largo crece hacia atrás (-Z)
        int z1 = 0;
        int z0 = -(largo - 1);

        int x1 = x0 + ancho - 1;
        int y1 = y0 + alto - 1;

        // 1. Caja principal de la arena (piso + 4 paredes, techo abierto)
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    boolean esPiso = (y == y0);
                    boolean esPared = (x == x0 || x == x1 || z == z0 || z == z1);
                    BlockPos pos = new BlockPos(x, y, z);
                    if (esPiso || esPared) {
                        level.setBlock(pos, paredState, 2);
                    } else {
                        level.setBlock(pos, aireState, 2);
                    }
                }
            }
        }

        // 2. Anillo exterior de barreras (1 bloque afuera de los muros, 2 bloques de alto sobre el borde)
        for (int y = y1 + 1; y <= y1 + 2; y++) {
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                level.setBlock(new BlockPos(x, y, z0 - 1), barreraState, 2);
                level.setBlock(new BlockPos(x, y, z1 + 1), barreraState, 2);
            }
            for (int z = z0; z <= z1; z++) {
                level.setBlock(new BlockPos(x0 - 1, y, z), barreraState, 2);
                level.setBlock(new BlockPos(x1 + 1, y, z), barreraState, 2);
            }
        }

        // 3. Sub-Lobby alineado en Z = 1: 9x9 interior (11x11 exterior), techo de linternas
        int subX0 = x0 + 2;
        int subZ0 = z1 + 1;
        construirSalaIluminada(level, subX0, y0, subZ0, 11, 6, 11, paredState, luzState);

        // 4. Sala de Espera del Buscador alineada en Z = 1: 3x3 interior (5x5 exterior)
        int busX0 = subX0 + 13;
        int busZ0 = z1 + 1;
        construirSalaIluminada(level, busX0, y0, busZ0, 5, 6, 5, paredState, luzState);

        // 5. Registrar metadatos y spawns en JSON
        ArenaData arena = new ArenaData();
        arena.id = id.toLowerCase();
        arena.nombreDisplay = nombreDisplay;
        arena.bloqueBase = bloqueId;
        arena.skinEscondido = skinName;
        arena.habilitada = false;

        arena.xInicio = x0;
        arena.xFin = x1;
        arena.yPiso = y0;
        arena.zInicio = z0;
        arena.zFin = z1;

        arena.spawnArena = PosData.fromBlockPos(level, new BlockPos(x0 + ancho / 2, y0 + 1, z0 + largo / 2));
        arena.spawnSubLobby = PosData.fromBlockPos(level, new BlockPos(subX0 + 5, y0 + 1, subZ0 + 5));
        arena.spawnEsperaBuscador = PosData.fromBlockPos(level, new BlockPos(busX0 + 2, y0 + 1, busZ0 + 2));

        ConfigManager.get().arenas.put(arena.id, arena);
        ConfigManager.save();

        return arena;
    }

    private static void construirSalaIluminada(ServerLevel level, int x0, int y0, int z0, int anchoExt, int altoExt, int largoExt, BlockState pared, BlockState luz) {
        int x1 = x0 + anchoExt - 1;
        int yTecho = y0 + altoExt - 1;
        int z1 = z0 + largoExt - 1;

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= yTecho; y++) {
                for (int z = z0; z <= z1; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (y == yTecho) {
                        boolean esBorde = (x == x0 || x == x1 || z == z0 || z == z1);
                        level.setBlock(pos, esBorde ? pared : luz, 2);
                    } else {
                        boolean borde = (x == x0 || x == x1 || y == y0 || z == z0 || z == z1);
                        level.setBlock(pos, borde ? pared : Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    public static void eliminarEstructuraArena(ServerLevel level, ArenaData arena) {
        BlockState aire = Blocks.AIR.defaultBlockState();

        // 1. Limpiar caja principal y anillo exterior de barreras (+2 bloques de alto)
        for (int x = arena.xInicio - 1; x <= arena.xFin + 1; x++) {
            for (int y = arena.yPiso; y <= arena.yPiso + 102; y++) {
                for (int z = arena.zInicio - 1; z <= arena.zFin + 1; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, aire, 2);
                    }
                }
            }
        }

        // 2. Limpiar Sub-Lobby (11x11 ext) y Sala del Buscador (5x5 ext) adosadas en zFin + 1
        int subX0 = arena.xInicio + 2;
        int busX1 = subX0 + 13 + 4;
        int salasZ0 = arena.zFin + 1;
        int salasZ1 = salasZ0 + 10;

        for (int x = subX0; x <= busX1; x++) {
            for (int y = arena.yPiso; y <= arena.yPiso + 6; y++) {
                for (int z = salasZ0; z <= salasZ1; z++) {
                    level.setBlock(new BlockPos(x, y, z), aire, 2);
                }
            }
        }
    }
}