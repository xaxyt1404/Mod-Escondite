package com.yessenia.escondite.client;

import com.yessenia.escondite.network.HudSyncPayload;

import java.util.List;

public class ClientHudStorage {
    public static boolean enPartidaActiva = false;
    public static String rolLocal = "NINGUNO";
    public static String nombreArena = "";
    public static String fase = "";
    public static int segundosRestantes = 0;
    public static List<String> buscadores = List.of();
    public static List<String> escondidos = List.of();

    public static void actualizar(HudSyncPayload p) {
        enPartidaActiva = p.enPartidaActiva();
        rolLocal = p.rolLocal();
        nombreArena = p.nombreArena();
        fase = p.fase();
        segundosRestantes = p.segundosRestantes();
        buscadores = p.buscadores();
        escondidos = p.escondidos();
    }

    public static void limpiar() {
        enPartidaActiva = false;
        rolLocal = "NINGUNO";
        nombreArena = "";
        fase = "";
        segundosRestantes = 0;
        buscadores = List.of();
        escondidos = List.of();
    }
}