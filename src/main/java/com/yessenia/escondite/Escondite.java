package com.yessenia.escondite;

import com.yessenia.escondite.command.EsconditeCommands;
import com.yessenia.escondite.item.ModItems;
import com.yessenia.escondite.network.SkinNetworkManager;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.game.GameEvents;
import com.yessenia.escondite.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Escondite implements ModInitializer {
	public static final String MOD_ID = "escondite";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.load();
		GameEvents.register();
		SkinNetworkManager.registerPayload();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			EsconditeCommands.register(dispatcher);
		});
		ModSounds.register();
		ModItems.register();
		LOGGER.info("Mod Escondite inicializado correctamente.");
	}
}