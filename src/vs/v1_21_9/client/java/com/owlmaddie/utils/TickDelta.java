// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import com.owlmaddie.mixin.client.WorldRenderMixin.MixinRenderContext;

/**
 * Returns the per-frame tick-delta for MC 1.21.9.
 * Fabric API removed WorldRenderEvents in 1.21.9, so we extract from mixin context.
 */
public final class TickDelta {
    private TickDelta() {
    }

    public static float get(Object context) {
        MixinRenderContext ctx = (MixinRenderContext) context;
        return ctx.tickDelta();
    }
}
