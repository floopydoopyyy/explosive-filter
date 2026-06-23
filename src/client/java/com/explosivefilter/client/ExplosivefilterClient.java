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
				FilterPackets.EXPLODE_TRIGGER_ID,
				(client, handler, buf, responseSender) -> {
					buf.readDouble(); buf.readDouble(); buf.readDouble(); // x, y, z (unused)
					buf.readFloat(); buf.readFloat();                     // power, shakeIntensity (unused)
					int shakeDuration = buf.readVarInt();
					client.execute(() -> {
						if (client.player == null) return;
						int hurtTicks = Math.min(20, shakeDuration / 2);
						client.player.hurtTime     = hurtTicks;
						client.player.hurtDuration = hurtTicks;
					});
				});
	}
}
