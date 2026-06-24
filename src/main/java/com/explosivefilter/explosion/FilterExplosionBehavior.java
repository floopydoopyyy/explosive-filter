package com.explosivefilter.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;
import java.util.UUID;

public class FilterExplosionBehavior extends ExplosionDamageCalculator {

    private final boolean dropItems;

    public FilterExplosionBehavior(boolean dropItems, boolean dealDamage, boolean damageOthers, UUID speakerUuid) {
        this.dropItems = dropItems;
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, FluidState fluid) {
        if (!dropItems) return Optional.empty();
        return super.getBlockExplosionResistance(explosion, reader, pos, state, fluid);
    }
}
