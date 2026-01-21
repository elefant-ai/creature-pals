// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;

public final class TickDelta {
    private TickDelta() {
    }

    /**
     * 1.21.10+: Get tick delta from Minecraft client's delta tracker
     */
    public static float get(WorldRenderContext ctx) {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
