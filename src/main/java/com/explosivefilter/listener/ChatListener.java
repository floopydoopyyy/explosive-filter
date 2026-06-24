package com.explosivefilter.listener;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.explosion.FilterExplosionBehavior;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;

public final class ChatListener {

    private static final double FX_RADIUS = 64.0;

    // 1.16.5: ServerMessageEvents does not exist. Chat is intercepted via
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

        // 1.16.5: Text.translatable() doesn't exist; use new TranslatableComponent().
        // sendSystemMessage() doesn't exist; use sendMessage(component, uuid).
        world.players().forEach(p -> p.sendMessage(
                new TranslatableComponent("chat.explosivefilter.trigger", sender.getDisplayName()),
                Util.NIL_UUID));

        DamageSource selfSource   = DamageSource.explosion(sender);
        DamageSource blamedSource = DamageSource.explosion(sender);

        Explosion.BlockInteraction interaction = ExplosiveFilterConfig.isWorldDamage()
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.NONE;

        // Explosion first so its packet reaches the client before the death packet.
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
            // 1.16.5: getAbilities() not yet a method; access public field directly.
            boolean wasInvulnerable = sender.abilities.invulnerable;
            sender.abilities.invulnerable = false;
            float dmg = ExplosiveFilterConfig.isInstakill() ? 10_000f : power * 5f;
            sender.hurt(selfSource, dmg);
            if (!sender.isDeadOrDying()) {
                sender.abilities.invulnerable = wasInvulnerable;
            }
        }

        float shakeIntensity = Math.min(1.0f, power / 10f);
        int shakeDuration    = Math.max(10, (int)(power * 3));

        for (ServerPlayer nearby : world.players()) {
            if (nearby.distanceToSqr(x, y, z) <= FX_RADIUS * FX_RADIUS
                    && ServerPlayNetworking.canSend(nearby, FilterPackets.EXPLODE_TRIGGER_ID)) {
                // 1.16.5: no var (Java 8 target); use explicit FriendlyByteBuf type.
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
                buf.writeFloat(power); buf.writeFloat(shakeIntensity); buf.writeVarInt(shakeDuration);
                ServerPlayNetworking.send(nearby, FilterPackets.EXPLODE_TRIGGER_ID, buf);
            }
        }
    }
}
