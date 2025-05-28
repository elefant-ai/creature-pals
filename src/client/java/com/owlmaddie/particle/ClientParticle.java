package com.owlmaddie.particle;


import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class ClientParticle {

    public static void register() {
        ParticleFactoryRegistry.getInstance().register(Particles.ATTACK_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FLEE_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FIRE_BIG_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FOLLOW_ENEMY_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.HEART_SMALL_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FIRE_SMALL_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FOLLOW_FRIEND_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.HEART_BIG_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.PROTECT_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_FRIEND_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_ENEMY_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(LeadParticleEffect.TYPE, LeadParticleFactory::new);
    }
}
