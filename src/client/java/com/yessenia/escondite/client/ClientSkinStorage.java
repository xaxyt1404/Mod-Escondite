package com.yessenia.escondite.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientSkinStorage {
    private static final Map<UUID, String> skins = new HashMap<>();

    public static void actualizarSkin(UUID uuid, String skinName) {
        if (skinName == null || skinName.isEmpty()) {
            skins.remove(uuid);
        } else {
            skins.put(uuid, skinName);
        }
    }

    public static String getSkin(UUID uuid) {
        return skins.get(uuid);
    }

    public static void limpiar() {
        skins.clear();
    }
}