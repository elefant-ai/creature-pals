// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie;

import com.owlmaddie.chat.EventQueueManager;
import com.owlmaddie.commands.CreaturePalsCommands;
import com.owlmaddie.inventory.ModMenus;
import com.owlmaddie.network.ServerPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ModInit} class initializes this mod on the server and defines all
 * the server message
 * identifiers. It also listens for messages from the client, and has code to
 * send
 * messages to the client.
 */
public class ModInit implements ModInitializer {
    public static final String MODID = "creaturepals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        // Register server commands
		CreaturePalsCommands.register();

		// Register events
        ModMenus.register();
		ServerPackets.register();

		LOGGER.info("CreaturePals MOD Initialized!");

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			EventQueueManager.injectOnServerTick(server);
		});
	}
}