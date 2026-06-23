package com.explosivefilter.client;

import com.explosivefilter.network.FilterPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ExplosivefilterClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		// Camera shake on explosion — hurtTime drives the red-screen tint and roll.
		// Vanilla ExplosionS2CPacket already delivers particles and sound.
		ClientPlayNetworking.registerGlobalReceiver(
				FilterPackets.ExplodeTriggerPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					if (context.client().player == null) return;

					int hurtTicks = Math.min(20, payload.shakeDuration() / 2);
					context.client().player.hurtTime     = hurtTicks;
					context.client().player.hurtDuration = hurtTicks;
				}));
	}
}
