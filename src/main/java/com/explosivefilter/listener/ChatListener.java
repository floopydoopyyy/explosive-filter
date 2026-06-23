package com.explosivefilter.listener;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.explosion.FilterExplosionBehavior;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ChatListener {

    private static final double FX_RADIUS = 64.0;

    private static final ResourceKey<DamageType> SELF_EXPLOSION_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("explosivefilter", "word_explosion_self"));

    private static final ResourceKey<DamageType> BLAMED_EXPLOSION_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("explosivefilter", "word_explosion_blamed"));

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

        ServerLevel world = sender.serverLevel();
        double x = sender.getX();
        double y = sender.getY() + 1.0;
        double z = sender.getZ();

        var typeLookup = world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);

        // Speaker always takes damage directly with the self-explosion source so they get
        // the comical self-death message. The word_explosion_self damage type is in the
        // bypasses_invulnerability tag, so creative players are killed just as reliably.
        if (ExplosiveFilterConfig.isDealDamage() && !sender.isDeadOrDying()) {
            DamageSource selfSource = new DamageSource(
                    typeLookup.getOrThrow(SELF_EXPLOSION_KEY));
            float dmg = ExplosiveFilterConfig.isInstakill()
                    ? 10_000f
                    : power * 5f;
            sender.hurt(selfSource, dmg);
        }

        // Blamed source carries the sender as the causing entity so that nearby players
        // who die see "%1$s was caught in %2$s's blast" with correct attribution.
        // FilterExplosionBehavior always excludes the speaker from blast damage since
        // their death is already handled above.
        DamageSource blamedSource = new DamageSource(
                typeLookup.getOrThrow(BLAMED_EXPLOSION_KEY),
                sender,
                sender);

        Level.ExplosionInteraction interaction = ExplosiveFilterConfig.isWorldDamage()
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;

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

        // Camera-shake packet (on top of vanilla explosion FX already sent by explode()).
        float shakeIntensity = Math.min(1.0f, power / 10f);
        int shakeDuration    = Math.max(10, (int)(power * 3));
        FilterPackets.ExplodeTriggerPayload fxPayload =
                new FilterPackets.ExplodeTriggerPayload(x, y, z, power, shakeIntensity, shakeDuration);

        for (ServerPlayer nearby : world.players()) {
            if (nearby.distanceToSqr(x, y, z) <= FX_RADIUS * FX_RADIUS
                    && ServerPlayNetworking.canSend(nearby, FilterPackets.ExplodeTriggerPayload.TYPE)) {
                ServerPlayNetworking.send(nearby, fxPayload);
            }
        }
    }
}
