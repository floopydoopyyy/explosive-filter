package com.explosivefilter.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;

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

    // When dropItems=false, nothing is added to toBlow so no blocks break and no items drop.
    // (1.21.1 has no public API for "destroy block, suppress drops" without a mixin.)
    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, float power) {
        return dropItems && super.shouldBlockExplode(explosion, reader, pos, state, power);
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        if (!dealDamage) return false;
        // Speaker damage is always applied directly in ChatListener with the self-explosion
        // damage source before explode() is called. Exclude them from blast damage entirely
        // so they get the correct death message and aren't double-hit.
        if (entity.getUUID().equals(speakerUuid)) return false;
        if (!damageOthers) return false;
        return true;
    }
}
