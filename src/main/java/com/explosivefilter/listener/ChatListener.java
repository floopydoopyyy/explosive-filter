package com.explosivefilter.listener;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.explosion.FilterExplosionBehavior;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;

public final class ChatListener {

    private static final double FX_RADIUS = 64.0;

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register(ChatListener::onChatMessage);
    }

    private static void onChatMessage(
            PlayerChatMessage message,
            ServerPlayer sender,
            ChatType.Bound params) {

        String content = message.signedContent();
        float power = ExplosiveFilterConfig.getPowerFor(content);
        if (power < 0) return;

        ServerLevel world = (ServerLevel) sender.getLevel();
        double x = sender.getX();
        double y = sender.getY() + 1.0;
        double z = sender.getZ();

        // 1.19.2 has no data-driven damage types; use the static explosion factory.
        // Instakill will not affect creative players on this version (no
        // bypasses_invulnerability damage type tag support).
        DamageSource blastSource = DamageSource.explosion(sender);

        if (ExplosiveFilterConfig.isDealDamage() && !sender.isDeadOrDying()) {
            float dmg = ExplosiveFilterConfig.isInstakill()
                    ? 10_000f
                    : power * 5f;
            sender.hurt(blastSource, dmg);
        }

        // Level.ExplosionInteraction does not exist in 1.19.2; use Explosion.BlockInteraction.
        Explosion.BlockInteraction interaction = ExplosiveFilterConfig.isWorldDamage()
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.NONE;

        world.explode(
                null,
                blastSource,
                new FilterExplosionBehavior(
                        ExplosiveFilterConfig.isDropItems(),
                        ExplosiveFilterConfig.isDealDamage(),
                        ExplosiveFilterConfig.isDamageOthers(),
                        sender.getUUID()),
                x, y, z,
                power,
                ExplosiveFilterConfig.isFire(),
                interaction
        );

        // Camera-shake packet.
        float shakeIntensity = Math.min(1.0f, power / 10f);
        int shakeDuration    = Math.max(10, (int)(power * 3));

        for (ServerPlayer nearby : world.players()) {
            if (nearby.distanceToSqr(x, y, z) <= FX_RADIUS * FX_RADIUS
                    && ServerPlayNetworking.canSend(nearby, FilterPackets.EXPLODE_TRIGGER_ID)) {
                var buf = PacketByteBufs.create();
                buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
                buf.writeFloat(power); buf.writeFloat(shakeIntensity); buf.writeVarInt(shakeDuration);
                ServerPlayNetworking.send(nearby, FilterPackets.EXPLODE_TRIGGER_ID, buf);
            }
        }
    }
}
