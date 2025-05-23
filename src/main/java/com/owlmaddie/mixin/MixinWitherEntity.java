package com.owlmaddie.mixin;

import com.owlmaddie.utils.WitherEntityAccessor;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Mixin to expose the protected dropEquipment method from WitherEntity.
 */
@Mixin(WitherEntity.class)
public abstract class MixinWitherEntity implements WitherEntityAccessor {

    @Shadow
    protected abstract void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer);

    @Override
    public void callDropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        dropEquipment(world, source, causedByPlayer);
    }
}
