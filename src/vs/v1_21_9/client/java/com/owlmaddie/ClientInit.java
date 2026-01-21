// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie;

import com.owlmaddie.chat.ClientChatDataManager;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.particle.BehaviorParticle;
import com.owlmaddie.particle.LeadParticle;
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
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * The {@code ClientInit} class initializes this mod in the client and defines
 * all hooks into the
 * render pipeline to draw chat bubbles, text, and entity icons.
 *
 * This is the 1.21.9+ version with updated particle registration API.
 */
public class ClientInit implements ClientModInitializer {
    private static long tickCounter = 0;

    /**
     * Returns the current tick counter for mixin-based rendering.
     * Used by WorldRenderMixin in 1.21.9 since Fabric API removed WorldRenderEvents.
     */
    public static long getTickCounter() {
        return tickCounter;
    }

    @Override
    public void onInitializeClient() {
        // Register particle factories (1.21.9+ API)
        ParticleFactoryRegistry.getInstance().register(Particles.HEART_SMALL_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.HEART_BIG_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.FIRE_SMALL_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.FIRE_BIG_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.ATTACK_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.FLEE_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.FOLLOW_FRIEND_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.FOLLOW_ENEMY_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.PROTECT_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_FRIEND_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_ENEMY_PARTICLE, BehaviorParticle.createProvider(BehaviorParticle::new));
        ParticleFactoryRegistry.getInstance().register(Particles.LEAD_PARTICLE, LeadParticle.createProvider(LeadParticle::new));

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
