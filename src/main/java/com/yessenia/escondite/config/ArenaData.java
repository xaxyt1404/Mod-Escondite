package com.yessenia.escondite.config;

public class ArenaData {
    public String id;
    public String nombreDisplay;
    public String bloqueBase;
    public String skinEscondido;
    public boolean habilitada;

    // Límites espaciales para encadenar la siguiente arena en fila (+X)
    public int xInicio;
    public int xFin;
    public int yPiso;
    public int zInicio;
    public int zFin;

    // Spawns principales
    public PosData spawnArena;
    public PosData spawnSubLobby;
    public PosData spawnEsperaBuscador;

    public ArenaData() {}
}