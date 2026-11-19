package com.yessenia.escondite.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("escondite_arenas.json");

    public static class ModConfigData {
        public PosData lobbyGeneral = null;
        public Map<String, ArenaData> arenas = new LinkedHashMap<>();
    }

    private static ModConfigData data = new ModConfigData();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfigData loaded = GSON.fromJson(reader, ModConfigData.class);
                if (loaded != null) {
                    data = loaded;
                    if (data.arenas == null) data.arenas = new LinkedHashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModConfigData get() {
        return data;
    }

    public static int getSiguienteXInicio() {
        if (data.arenas.isEmpty()) {
            return 0;
        }
        int maxFin = Integer.MIN_VALUE;
        for (ArenaData arena : data.arenas.values()) {
            if (arena.xFin > maxFin) {
                maxFin = arena.xFin;
            }
        }
        return maxFin + 6; // 5 bloques de separación exacta entre paredes externas
    }
}