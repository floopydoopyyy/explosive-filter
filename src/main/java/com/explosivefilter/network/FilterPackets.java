package com.explosivefilter.network;

import com.explosivefilter.Explosivefilter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FilterPackets {

    // S→C, broadcast to nearby players.
    // Vanilla ExplosionS2CPacket delivers particles and sound automatically;
    // this payload adds camera shake
    public record ExplodeTriggerPayload(
            double x, double y, double z,
            float power,
            float shakeIntensity,
            int shakeDuration
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ExplodeTriggerPayload> TYPE =
                new CustomPacketPayload.Type<>(Explosivefilter.id("explode_trigger"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ExplodeTriggerPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.x());
                            buf.writeDouble(p.y());
                            buf.writeDouble(p.z());
                            buf.writeFloat(p.power());
                            buf.writeFloat(p.shakeIntensity());
                            buf.writeVarInt(p.shakeDuration());
                        },
                        buf -> new ExplodeTriggerPayload(
                                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                                buf.readFloat(), buf.readFloat(), buf.readVarInt()
                        )
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private FilterPackets() {}
}
