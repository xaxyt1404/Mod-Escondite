package com.yessenia.escondite.client;

import com.yessenia.escondite.client.gui.GestorArenasScreen;
import com.yessenia.escondite.client.render.PortalArenaRenderer;
import com.yessenia.escondite.entity.ModEntities;
import com.yessenia.escondite.item.ModItems;
import com.yessenia.escondite.network.OpenGestorPayload;
import com.yessenia.escondite.network.SkinSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomModelData;

public class EsconditeClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// 1. Predicados visuales para cambios de modelo en tiempo real
		ResourceLocation estadoPredicate = ResourceLocation.fromNamespaceAndPath("escondite", "estado");
		ResourceLocation estiradoPredicate = ResourceLocation.fromNamespaceAndPath("escondite", "estirado");
		ClampedItemPropertyFunction estadoFn = (stack, level, entity, seed) -> {
			CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
			return cmd != null ? (float) cmd.value() : 0.0f;
		};

		ItemProperties.register(ModItems.ORBE_PREPARACION, estadoPredicate, estadoFn);
		ItemProperties.register(ModItems.BACULO_ANFITRION, estadoPredicate, estadoFn);
		ItemProperties.register(ModItems.CASTIGO, estiradoPredicate, estadoFn);

		// 2. Sincronización de skins dinámicas desde el servidor
		ClientPlayNetworking.registerGlobalReceiver(SkinSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				ClientSkinStorage.actualizarSkin(payload.playerUuid(), payload.skinName(), payload.skinBytes());
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientSkinStorage.limpiar();
		});

		// 3. Renderizador del Bloque Flotante (Portal de Arena)
		EntityRendererRegistry.register(ModEntities.PORTAL_ARENA, PortalArenaRenderer::new);

		// 4. Apertura y actualización en vivo de la ventana del Gestor de Arenas
		ClientPlayNetworking.registerGlobalReceiver(OpenGestorPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				if (context.client().screen instanceof GestorArenasScreen actual) {
					actual.actualizarDatos(payload.tieneLobbyGeneral(), payload.arenas());
				} else {
					context.client().setScreen(new GestorArenasScreen(payload.tieneLobbyGeneral(), payload.arenas()));
				}
			});
		});
	}
}