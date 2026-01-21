// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie;

import com.owlmaddie.chat.ClientChatDataManager;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.particle.BehaviorParticle;
import com.owlmaddie.particle.BehaviorParticle.CreatureParticleFactory;
import com.owlmaddie.particle.LeadParticle;
import com.owlmaddie.particle.LeadParticleEffect;
import com.owlmaddie.particle.LeadParticle.LeadParticleFactory;
import com.owlmaddie.particle.Particles;
import com.owlmaddie.player2.HeartbeatManager;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.ClickHandler;
import com.owlmaddie.ui.InventoryKeyHandler;
import com.owlmaddie.ui.PlayerMessageManager;
import com.owlmaddie.utils.RegisterWorldRenderer;
import com.owlmaddie.utils.TickDelta;
import com.owlmaddie.inventory.ModMenus;
import com.owlmaddie.inventory.MobInventoryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * The {@code ClientInit} class initializes this mod in the client and defines
 * all hooks into the
 * render pipeline to draw chat bubbles, text, and entity icons.
 */
public class ClientInit implements ClientModInitializer {
    private static long tickCounter = 0;

    @Override
    public void onInitializeClient() {
        // Register particle factories
        ParticleFactoryRegistry.getInstance().register(Particles.HEART_SMALL_PARTICLE, BehaviorParticle.CreatureParticleFactory::createParticle);
        ParticleFactoryRegistry.getInstance().register(Particles.HEART_BIG_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FIRE_SMALL_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FIRE_BIG_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.ATTACK_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FLEE_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FOLLOW_FRIEND_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.FOLLOW_ENEMY_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.PROTECT_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_FRIEND_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_ENEMY_PARTICLE, CreatureParticleFactory::new);
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_PARTICLE, LeadParticle.LeadParticleFactory::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            PlayerMessageManager.tickUpdate();
            HeartbeatManager.injectIntoOnTick();
        });

        // Register events
        ClickHandler.register();
        InventoryKeyHandler.register();
        ClientPackets.register();
        MenuScreens.register(ModMenus.MOB_INVENTORY, MobInventoryScreen::new);

        // Register an event callback to render text bubbles

        RegisterWorldRenderer.register(ctx -> {
            float delta = TickDelta.get(ctx);
            BubbleRenderer.drawTextAboveEntities(ctx, tickCounter, delta);
        });

        // Register an event callback for when the client disconnects from a server or
        // changes worlds
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear or reset the ChatDataManager
            ClientChatDataManager.clearData();
        });
    }
}
