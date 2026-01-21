package com.owlmaddie.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
// Create the particle type
public class LeadParticle extends Particle {
    public static final ParticleRenderType LEAD_PARTICLE = new ParticleRenderType("creaturepals:lead_particle");

    // You can handle passing to the particle group however you want
    // Making the fields accessible or having a dedicated method
    public final Model<Unit> model;

    protected LeadParticle(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Function<EntityModelSet, Model<Unit>> modelFactory, double angle) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.model = modelFactory.apply(Minecraft.getInstance().getEntityModels());
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;


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

        P create(ClientLevel level, double x, double y, double z);
    }

    protected static <P extends LeadParticle> ParticleProvider<LeadParticleEffect> createProvider(LeadParticleFactory<P> factory) {
        return (options, level, x, y, z, xd, yd, zd, random) -> factory.create(level, x, y, z);
    }
}