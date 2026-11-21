package com.yessenia.escondite;

import com.yessenia.escondite.command.EsconditeCommands;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.entity.ModEntities;
import com.yessenia.escondite.game.GameEvents;
import com.yessenia.escondite.item.ItemFactory;
import com.yessenia.escondite.item.ModItems;
import com.yessenia.escondite.network.AdminNetworkManager;
import com.yessenia.escondite.network.SkinNetworkManager;
import com.yessenia.escondite.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Escondite implements ModInitializer {
	public static final String MOD_ID = "escondite";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Map<UUID, Integer> latigoEstiradoTicks = new HashMap<>();

	@Override
	public void onInitialize() {
		ModSounds.register();
		ModItems.register();
		ModEntities.register();
		ConfigManager.load();
		SkinNetworkManager.registerPayload();
		GameEvents.register();
		AdminNetworkManager.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			EsconditeCommands.register(dispatcher);
		});

		// Tick del servidor: Cubo Teseracto (slot 8) para admins y restauración del Látigo enrollado
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				// Restaurar textura del látigo tras 16 ticks (0.8s)
				if (latigoEstiradoTicks.containsKey(p.getUUID())) {
					int rest = latigoEstiradoTicks.get(p.getUUID()) - 1;
					if (rest <= 0) {
						latigoEstiradoTicks.remove(p.getUUID());
						for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
							ItemStack st = p.getInventory().getItem(i);
							if (st.is(ModItems.CASTIGO)) {
								st.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0));
							}
						}
					} else {
						latigoEstiradoTicks.put(p.getUUID(), rest);
					}
				}

				boolean esAdminCreativo = p.isCreative() && p.hasPermissions(2);
				boolean tieneGestor = false;

				for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
					if (p.getInventory().getItem(i).is(ModItems.GESTOR_ARENAS)) {
						tieneGestor = true;
						if (!esAdminCreativo) {
							p.getInventory().setItem(i, ItemStack.EMPTY);
						}
					}
				}

				if (esAdminCreativo && !tieneGestor) {
					if (p.getInventory().getItem(8).isEmpty()) {
						p.getInventory().setItem(8, ItemFactory.crearGestorArenas());
					} else {
						p.getInventory().add(ItemFactory.crearGestorArenas());
					}
				}
			}
		});

		LOGGER.info("Mod Escondite inicializado correctamente.");
	}

	public static void activarLatigo(ServerPlayer player) {
		if (player == null || player.level().isClientSide()) return;
		ItemStack stack = player.getMainHandItem();
		if (!stack.is(ModItems.CASTIGO)) return;

		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
		latigoEstiradoTicks.put(player.getUUID(), 16); // 16 ticks = 0.8 segundos

		ServerLevel serverLevel = player.serverLevel();
		Vec3 inicio = player.getEyePosition().add(0, -0.25, 0);
		Vec3 direccion = player.getLookAngle().normalize();
		for (double d = 0.6; d <= 3.6; d += 0.3) {
			Vec3 punto = inicio.add(direccion.scale(d));
			serverLevel.sendParticles(ParticleTypes.FLAME, punto.x, punto.y, punto.z, 2, 0.04, 0.04, 0.04, 0.01);
			serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, punto.x, punto.y, punto.z, 1, 0.05, 0.05, 0.05, 0.02);
		}
		serverLevel.playSound(null, player.blockPosition(), ModSounds.LATIGO_ATAQUE, SoundSource.PLAYERS, 0.9f, 1.1f);
	}
}