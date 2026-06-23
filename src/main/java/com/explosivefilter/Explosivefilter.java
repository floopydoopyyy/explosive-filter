package com.explosivefilter;

import com.explosivefilter.command.FilterCommand;
import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.listener.ChatListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Explosivefilter implements ModInitializer {
	public static final String MOD_ID = "explosivefilter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		FilterCommand.register();
		ChatListener.register();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ExplosiveFilterConfig.load();
			LOGGER.info("[ExplosiveFilter] Loaded {} trigger phrase(s).", ExplosiveFilterConfig.getPhrases().size());
		});

		LOGGER.info("[ExplosiveFilter] Mod initialized.");
	}
}
