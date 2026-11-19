package com.yessenia.escondite.client;

import com.yessenia.escondite.network.SkinSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EsconditeClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SkinSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				ClientSkinStorage.actualizarSkin(payload.playerUuid(), payload.skinName());
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientSkinStorage.limpiar();
		});
	}
}