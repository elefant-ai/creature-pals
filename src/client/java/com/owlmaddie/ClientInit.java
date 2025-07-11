package com.owlmaddie;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.commands.CreaturePalsCommands;
import com.owlmaddie.network.ClientPackets;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.particle.ClientParticle;
import com.owlmaddie.particle.CreatureParticleFactory;
import com.owlmaddie.particle.Particles;
import com.owlmaddie.player2.HeartbeatManager;
import com.owlmaddie.player2.TTS;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.ClickHandler;
import com.owlmaddie.ui.PlayerMessageManager;
import com.owlmaddie.ui.TTSToggleButton;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

import static com.owlmaddie.network.ServerPackets.*;

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
        Particles.register();

        ClientParticle.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            PlayerMessageManager.tickUpdate();
            // AAA add client ontick handlers here
            HeartbeatManager.injectIntoOnTick();
            // STT.handleTick();
            // if(TTSToggleButton.tick()){
            //     TTS.enabled = !TTS.enabled;
            // }

        });

        // Register events
        ClickHandler.register();
        ClientPackets.register();

        // Register an event callback to render text bubbles
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register((context) -> {
            BubbleRenderer.drawTextAboveEntities(context, tickCounter, context.tickCounter().getTickDelta(false));
        });
        
        
        // Register an event callback for when the client disconnects from a server or
        // changes worlds
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Clear or reset the ChatDataManager
            ChatDataManager.getClientInstance().clearData();
        });
        
    }
}
