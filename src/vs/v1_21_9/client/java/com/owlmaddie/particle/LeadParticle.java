package com.owlmaddie.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.Camera;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Create the particle type
public class LeadParticle extends SingleQuadParticle {
    private final SpriteSet spriteProvider;
    private final double angle;

    public LeadParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteProvider, double angle) {
        super(level, x, y, z, 0, 0, 0, spriteProvider.get(level.random));
        this.spriteProvider = spriteProvider;
        this.angle = angle;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.quadSize = 0.1F * 2.0F;
        this.scale(4.5F);
        this.setLifetime(40);
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    public int getLightColor(float tint) {
        return 0xF000F0;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float tickDelta) {
        // Custom flat rendering with rotation by angle
        Vec3 cameraPos = camera.getPosition();
        float particleX = (float) (Mth.lerp(tickDelta, this.xo, this.x) - cameraPos.x());
        float particleY = (float) (Mth.lerp(tickDelta, this.yo, this.y) - cameraPos.y());
        float particleZ = (float) (Mth.lerp(tickDelta, this.zo, this.z) - cameraPos.z());

        // Create a rotation quaternion for flat XZ plane rotated by angle
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotateX((float) Math.toRadians(-90)); // Lay flat
        quaternionf.rotateZ((float) angle); // Rotate by lead direction

        float size = this.getQuadSize(tickDelta);

        renderState.add(
            this.getLayer(),
            particleX,
            particleY,
            particleZ,
            quaternionf.x,
            quaternionf.y,
            quaternionf.z,
            quaternionf.w,
            size,
            this.getU0(),
            this.getU1(),
            this.getV0(),
            this.getV1(),
            ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
            this.getLightColor(tickDelta)
        );
    }

    public static class LeadParticleFactory implements ParticleProvider<LeadParticleEffect> {
        private final SpriteSet spriteProvider;

        public LeadParticleFactory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(LeadParticleEffect effect, ClientLevel world, double x, double y, double z,
                                           double velocityX, double velocityY, double velocityZ, RandomSource random) {
            double angle = effect.getItem();
            return new LeadParticle(world, x, y, z, this.spriteProvider, angle);
        }
    }
}
