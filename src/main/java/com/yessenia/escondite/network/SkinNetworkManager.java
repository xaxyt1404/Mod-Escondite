package com.yessenia.escondite.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkinNetworkManager {
    private static final Map<UUID, String> skinsActivas = new HashMap<>();

    public static void registerPayload() {
        PayloadTypeRegistry.playS2C().register(SkinSyncPayload.TYPE, SkinSyncPayload.STREAM_CODEC);
    }

    public static void setSkinJugador(ServerPlayer target, String skinName, MinecraftServer server) {
        UUID uuid = target.getUUID();
        String valor = (skinName == null) ? "" : skinName.toLowerCase();

        if (valor.isEmpty()) {
            skinsActivas.remove(uuid);
        } else {
            skinsActivas.put(uuid, valor);
        }

        SkinSyncPayload payload = new SkinSyncPayload(uuid, valor);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(online, payload);
        }
    }

    public static void sincronizarTodoAJugador(ServerPlayer receptor) {
        for (Map.Entry<UUID, String> entry : skinsActivas.entrySet()) {
            ServerPlayNetworking.send(receptor, new SkinSyncPayload(entry.getKey(), entry.getValue()));
        }
    }

    public static void limpiarJugador(UUID uuid, MinecraftServer server) {
        if (skinsActivas.remove(uuid) != null && server != null) {
            SkinSyncPayload payload = new SkinSyncPayload(uuid, "");
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(online, payload);
            }
        }
    }
}