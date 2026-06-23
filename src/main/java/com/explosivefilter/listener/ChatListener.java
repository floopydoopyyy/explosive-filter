package com.explosivefilter.listener;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.explosion.FilterExplosionBehavior;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;

public final class ChatListener {

    private static final double FX_RADIUS = 64.0;

    // 1.18.2: ServerMessageEvents does not exist. Chat is intercepted via
    // ChatMixin on ServerGamePacketListenerImpl.handleChat(). This method is
    // called by the mixin directly.
    public static void register() {}

    public static void handle(String content, ServerPlayer sender) {
        float power = ExplosiveFilterConfig.getPowerFor(content);
        if (power < 0) return;

        ServerLevel world = (ServerLevel) sender.getLevel();
        double x = sender.getX();
        double y = sender.getY() + 1.0;
        double z = sender.getZ();

        Component announcement = new TranslatableComponent("chat.explosivefilter.trigger", sender.getDisplayName());
        // 1.18.2: sendSystemMessage was added in 1.19; use sendMessage with NIL UUID.
        world.players().forEach(p -> p.sendMessage(announcement, Util.NIL_UUID));

        // 1.18.2 has no data-driven damage types. DamageSource.explosion(Entity)
        // is the closest available attribution.
        DamageSource selfSource   = DamageSource.explosion(sender);
        DamageSource blamedSource = DamageSource.explosion(sender);

        Explosion.BlockInteraction interaction = ExplosiveFilterConfig.isWorldDamage()
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.NONE;

        // Explosion first so the visual packet reaches the client before the death packet.
        // In 1.18.2, Netty flushes each send immediately, so ordering matters.
        world.explode(
                null,
                blamedSource,
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

        if (ExplosiveFilterConfig.isDealDamage() && !sender.isDeadOrDying()) {
            boolean wasInvulnerable = sender.getAbilities().invulnerable;
            sender.getAbilities().invulnerable = false;
            float dmg = ExplosiveFilterConfig.isInstakill() ? 10_000f : power * 5f;
            sender.hurt(selfSource, dmg);
            if (!sender.isDeadOrDying()) {
                sender.getAbilities().invulnerable = wasInvulnerable;
            }
        }

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
