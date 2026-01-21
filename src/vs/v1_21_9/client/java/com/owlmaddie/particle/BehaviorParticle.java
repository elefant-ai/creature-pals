// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code BehaviorParticle} class defines a custom CreaturePals behavior
 * particle with an initial upward velocity
 * that gradually decreases, ensuring it never moves downward.
 */
public class BehaviorParticle extends Particle {
    protected BehaviorParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY,
                               double velocityZ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.scale(2f);
        this.setLifetime(35);

        // Start with an initial upward velocity
        this.yd = 0.1;
        this.xd *= 0.1;
        this.zd *= 0.1;
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }
    @Override
    public int getLightColor(float tint) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();

        // Gradually decrease the upward velocity over time
        if (this.yd > 0) {
            this.yd -= 0.002;
        }

        // Ensure the particle doesn't start moving downwards
        if (this.yd < 0) {
            this.yd = 0;
        }
    }


    @FunctionalInterface
    public interface CreatureParticleFactory<P extends BehaviorParticle> {

        P create(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ);
    }

    protected static <P extends BehaviorParticle> ParticleProvider<SimpleParticleType> createProvider(BehaviorParticle.CreatureParticleFactory<P> factory) {
        return (options, level, x, y, z, xd, yd, zd, random) -> factory.create(level, x, y, z, xd, yd, zd);
    }

}
