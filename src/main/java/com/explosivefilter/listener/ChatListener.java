package com.explosivefilter.listener;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.explosivefilter.explosion.FilterExplosionBehavior;
import com.explosivefilter.network.FilterPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.world.explosion.Explosion;

public final class ChatListener {

    private static final double FX_RADIUS = 64.0;

    // 1.16.5: ServerMessageEvents does not exist. Chat is intercepted via
    // ChatMixin on ServerPlayNetworkHandler.onChatMessage(). This method is
    // called by the mixin directly.
    public static void register() {}

    public static void handle(String content, ServerPlayerEntity sender) {
        float power = ExplosiveFilterConfig.getPowerFor(content);
        if (power < 0) return;

        ServerWorld world = sender.getServerWorld();
        double x = sender.getX();
        double y = sender.getY() + 1.0;
        double z = sender.getZ();

        // 1.16.5: Text.translatable() doesn't exist; use new TranslatableText().
        // sendSystemMessage() doesn't exist; use sendMessage(text, uuid).
        world.getPlayers().forEach(p ->
                p.sendMessage(new TranslatableText("chat.explosivefilter.trigger", sender.getDisplayName()), Util.NIL_UUID));

        DamageSource selfSource   = DamageSource.explosion(sender);
        DamageSource blamedSource = DamageSource.explosion(sender);

        Explosion.DestructionType destructionType = ExplosiveFilterConfig.isWorldDamage()
                ? Explosion.DestructionType.DESTROY
                : Explosion.DestructionType.NONE;

        // Explosion first so its packet reaches the client before the death packet.
        world.createExplosion(
                null,
                blamedSource,
                new FilterExplosionBehavior(
                        ExplosiveFilterConfig.isDropItems(),
                        ExplosiveFilterConfig.isDealDamage(),
                        ExplosiveFilterConfig.isDamageOthers(),
                        sender.getUuid()),
                x, y, z,
                power,
                ExplosiveFilterConfig.isFire(),
                destructionType
        );

        // 1.16.5: hurt() is called damage() in Yarn. isDead() replaces isDeadOrDying().
        if (ExplosiveFilterConfig.isDealDamage() && !sender.isDead()) {
            boolean wasInvulnerable = sender.getAbilities().invulnerable;
            sender.getAbilities().invulnerable = false;
            float dmg = ExplosiveFilterConfig.isInstakill() ? 10_000f : power * 5f;
            sender.damage(selfSource, dmg);
            if (!sender.isDead()) {
                sender.getAbilities().invulnerable = wasInvulnerable;
            }
        }

        float shakeIntensity = Math.min(1.0f, power / 10f);
        int shakeDuration    = Math.max(10, (int)(power * 3));

        for (ServerPlayerEntity nearby : world.getPlayers()) {
            if (nearby.squaredDistanceTo(x, y, z) <= FX_RADIUS * FX_RADIUS
                    && ServerPlayNetworking.canSend(nearby, FilterPackets.EXPLODE_TRIGGER_ID)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
                buf.writeFloat(power); buf.writeFloat(shakeIntensity); buf.writeVarInt(shakeDuration);
                ServerPlayNetworking.send(nearby, FilterPackets.EXPLODE_TRIGGER_ID, buf);
            }
        }
    }
}
