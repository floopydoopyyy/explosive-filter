package com.explosivefilter.network;

import com.explosivefilter.Explosivefilter;
import net.minecraft.resources.ResourceLocation;

public final class FilterPackets {

    // S→C channel for camera-shake payload (x, y, z, power, shakeIntensity, shakeDuration)
    public static final ResourceLocation EXPLODE_TRIGGER_ID = Explosivefilter.id("explode_trigger");

    private FilterPackets() {}
}
