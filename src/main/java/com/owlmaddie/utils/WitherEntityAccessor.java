package com.owlmaddie.utils;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

/**
 * Accessor interface for WitherEntity to allow calling dropEquipment externally.
 */
public interface WitherEntityAccessor {
    void callDropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer);
}
