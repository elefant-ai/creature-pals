package com.owlmaddie.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.NotNull;

// Create the particle type
public class LeadParticle extends Particle {
    public static final ParticleRenderType LEAD_PARTICLE = new ParticleRenderType("creaturepals:lead_particle");

    private final double angle;

    public LeadParticle(ClientLevel level, double x, double y, double z, double angle) {
        super(level, x, y, z, 0, 0, 0);
        this.angle = angle;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setLifetime(40);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int getLightColor(float tint) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getGroup() {
        // Set the particle type to our group
        return LEAD_PARTICLE;
    }

    @FunctionalInterface
    public interface LeadParticleFactory<P extends LeadParticle> {
        P create(ClientLevel level, double x, double y, double z, double angle);
    }

    public static ParticleProvider<LeadParticleEffect> createProvider(LeadParticleFactory<LeadParticle> factory) {
        return (options, level, x, y, z, xd, yd, zd, random) -> factory.create(level, x, y, z, options.getItem());
    }
}
