package com.explosivefilter.listener;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.explosion.FilterExplosionBehavior;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
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

        String content = message.signedContent().plain();
        float power = ExplosiveFilterConfig.getPowerFor(content);
        if (power < 0) return;

        ServerLevel world = (ServerLevel) sender.getLevel();
        double x = sender.getX();
        double y = sender.getY() + 1.0;
        double z = sender.getZ();

        Component announcement = Component.translatable("chat.explosivefilter.trigger", sender.getDisplayName());
        world.players().forEach(p -> p.sendSystemMessage(announcement));

        // 1.19.2 has no data-driven damage types, so custom death message IDs
        // (word_explosion_self / word_explosion_blamed) are unavailable. Both sources
        // use DamageSource.explosion(sender) which gives the vanilla "blown up by X"
        // message and correctly attributes the cause.
        DamageSource selfSource    = DamageSource.explosion(sender);
        DamageSource blamedSource  = DamageSource.explosion(sender);

        // Speaker always takes direct damage first, matching main's logic.
        // Temporarily clear the invulnerability ability flag so creative players are
        // killed — this replicates the bypasses_invulnerability tag on the
        // word_explosion_self damage type in 1.21.1.
        if (ExplosiveFilterConfig.isDealDamage() && !sender.isDeadOrDying()) {
            boolean wasInvulnerable = sender.getAbilities().invulnerable;
            sender.getAbilities().invulnerable = false;
            float dmg = ExplosiveFilterConfig.isInstakill() ? 10_000f : power * 5f;
            sender.hurt(selfSource, dmg);
            // Only restore invulnerability if the player survived (they are already
            // protected by the natural hurt-cooldown window during the explosion call).
            if (!sender.isDeadOrDying()) {
                sender.getAbilities().invulnerable = wasInvulnerable;
            }
        }

        // FilterExplosionBehavior excludes the speaker from blast damage via
        // shouldDamageEntity() on 1.21.1. That method does not exist on 1.19.2's
        // ExplosionDamageCalculator, but Minecraft's natural hurt-cooldown (set by the
        // direct hurt() call above) prevents the speaker from taking significant
        // additional blast damage in the same tick.
        Explosion.BlockInteraction interaction = ExplosiveFilterConfig.isWorldDamage()
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.NONE;

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
