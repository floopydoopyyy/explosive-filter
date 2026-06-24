package com.explosivefilter;

import com.explosivefilter.command.FilterCommand;
import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.listener.ChatListener;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Explosivefilter implements ModInitializer {
	public static final String MOD_ID = "explosivefilter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 26.1: ResourceLocation renamed to Identifier in net.minecraft.resources.
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// 26.1: PayloadTypeRegistry.playS2C() renamed to clientboundPlay().
		PayloadTypeRegistry.clientboundPlay().register(
				FilterPackets.ExplodeTriggerPayload.TYPE,
				FilterPackets.ExplodeTriggerPayload.STREAM_CODEC
		);

		FilterCommand.register();
		ChatListener.register();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ExplosiveFilterConfig.load();
			LOGGER.info("[ExplosiveFilter] Loaded {} trigger phrase(s).", ExplosiveFilterConfig.getPhrases().size());
		});

		LOGGER.info("[ExplosiveFilter] Mod initialized.");
	}
}
