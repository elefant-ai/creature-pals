// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.core.particles.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code LeadParticleEffect} class allows for an 'angle' to be passed along
 * with the Particle, to rotate it in the direction of LEAD behavior.
 */
public class LeadParticleEffect implements ParticleOptions {

    public static final MapCodec<LeadParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(Codec.DOUBLE.fieldOf("angle").forGetter(leadParticleEffect -> leadParticleEffect.angle))
                    .apply(instance, LeadParticleEffect::new)
    );
    private final double angle;


    public static final StreamCodec<RegistryFriendlyByteBuf, LeadParticleEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, leadParticleEffect -> leadParticleEffect.angle, LeadParticleEffect::new
    );

    public LeadParticleEffect( double angle) {
        this.angle = angle;
    }

    @Override
    public @NotNull ParticleType<LeadParticleEffect> getType() {
        return ServerPackets.LEAD_PARTICLE;
    }

    public double getItem() {
        return this.angle;
    }

}
