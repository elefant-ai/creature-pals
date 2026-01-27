// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * The {@code BehaviorParticle} class defines a custom CreaturePals behavior
 * particle with an initial upward velocity
 * that gradually decreases, ensuring it never moves downward.
 */
public class BehaviorParticle extends SingleQuadParticle {
    public BehaviorParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY,
                               double velocityZ, SpriteSet spriteSet) {
        super(world, x, y, z, velocityX, velocityY, velocityZ, spriteSet.get(world.random));
        this.scale(2f);
        this.setLifetime(35);

        // Start with an initial upward velocity
        this.yd = 0.1;
        this.xd *= 0.1;
        this.zd *= 0.1;
        this.hasPhysics = false;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
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

    public static class CreatureParticleFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public CreatureParticleFactory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                               double velocityX, double velocityY, double velocityZ, RandomSource random) {
            return new BehaviorParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteSet);
        }
    }
}
