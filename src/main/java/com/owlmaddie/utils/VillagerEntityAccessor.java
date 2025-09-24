// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import net.minecraft.world.entity.ai.gossip.GossipContainer;

/**
 * The {@code VillagerEntityAccessor} interface provides a method to access
 * the gossip system of a villager. It enables interaction with a villager's
 * gossip data for custom behavior or modifications.
 */
public interface VillagerEntityAccessor {
    GossipContainer getGossip();
}
