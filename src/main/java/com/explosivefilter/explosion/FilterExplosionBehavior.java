package com.explosivefilter.explosion;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.Optional;
import java.util.UUID;

public class FilterExplosionBehavior extends ExplosionBehavior {

    private final boolean dropItems;

    public FilterExplosionBehavior(boolean dropItems, boolean dealDamage, boolean damageOthers, UUID speakerUuid) {
        this.dropItems = dropItems;
    }

    // Returning Optional.empty() skips this block in the explosion ray calculation.
    @Override
    public Optional<Float> getBlastResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (!dropItems) return Optional.empty();
        return super.getBlastResistance(explosion, world, pos, blockState, fluidState);
    }
}
