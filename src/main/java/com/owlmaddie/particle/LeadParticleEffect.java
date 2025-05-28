package com.owlmaddie.particle;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.*;
import net.minecraft.util.dynamic.Codecs;

/**
 * The {@code LeadParticleEffect} class allows for an 'angle' to be passed along with the Particle, to rotate it in the direction of LEAD behavior.
 */
public class LeadParticleEffect implements ParticleEffect {
    public static final ParticleType<LeadParticleEffect> TYPE = FabricParticleTypes.<LeadParticleEffect>complex(
            type -> LeadParticleEffect.CODEC,
            type -> LeadParticleEffect.PACKET_CODEC
    );
    private final float angle;

    public float getAngle() {
        return angle;
    }

    public static final MapCodec<LeadParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            Codecs.POSITIVE_FLOAT.fieldOf("angle").forGetter(effect -> effect.angle)
                    )
                    .apply(instance, LeadParticleEffect::new)
    );
    public static final PacketCodec<RegistryByteBuf, LeadParticleEffect> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, effect -> effect.angle, LeadParticleEffect::new
    );
    public LeadParticleEffect(float angle) {
        this.angle = angle;
    }

    @Override
    public ParticleType<LeadParticleEffect> getType() {
        return TYPE;
    }

}