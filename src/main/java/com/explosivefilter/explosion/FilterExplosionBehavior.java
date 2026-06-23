package com.explosivefilter.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;
import java.util.UUID;

public class FilterExplosionBehavior extends ExplosionDamageCalculator {

    private final boolean dropItems;
    private final boolean dealDamage;
    private final boolean damageOthers;
    private final UUID speakerUuid;

    public FilterExplosionBehavior(boolean dropItems, boolean dealDamage, boolean damageOthers, UUID speakerUuid) {
        this.dropItems = dropItems;
        this.dealDamage = dealDamage;
        this.damageOthers = damageOthers;
        this.speakerUuid = speakerUuid;
    }

    // Returning Optional.empty() tells the explosion to skip this block entirely
    // (no destruction, no drops) — equivalent of shouldBlockExplode returning false in 1.21.
    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, FluidState fluid) {
        if (!dropItems) return Optional.empty();
        return super.getBlockExplosionResistance(explosion, reader, pos, state, fluid);
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        if (!dealDamage) return false;
        if (entity.getUUID().equals(speakerUuid)) return false;
        if (!damageOthers) return false;
        return true;
    }
}
