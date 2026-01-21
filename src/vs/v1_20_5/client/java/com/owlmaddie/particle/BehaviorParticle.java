package com.owlmaddie.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The {@code BehaviorParticle} class defines a custom CreaturePals behavior
 * particle with an initial upward velocity
 * that gradually decreases, ensuring it never moves downward.
 */
public class BehaviorParticle extends TextureSheetParticle {
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
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
    /**
     * The {@code CreatureParticleFactory} class is responsible for creating
     * instances of
     * {@link BehaviorParticle} with the specified parameters. Minecraft 1.20.5+
     * override of this class.
     */
    public static class CreatureParticleFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public CreatureParticleFactory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public BehaviorParticle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                               double velocityX, double velocityY, double velocityZ) {
            BehaviorParticle particle = new BehaviorParticle(world, x, y, z, velocityX, velocityY, velocityZ);
            particle.setSpriteFromAge(this.spriteProvider);
            return particle;
        }
    }

}
