package com.explosivefilter.network;

import com.explosivefilter.Explosivefilter;
import net.minecraft.util.Identifier;

public final class FilterPackets {

    // S→C channel for camera-shake payload (x, y, z, power, shakeIntensity, shakeDuration)
    public static final Identifier EXPLODE_TRIGGER_ID = Explosivefilter.id("explode_trigger");

    private FilterPackets() {}
}
