// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.owlmaddie.mixin.client.WorldRenderMixin.MixinRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Helper class for MC 1.21.9 that extracts rendering context from the mixin.
 * Fabric API removed WorldRenderEvents in 1.21.9, so we use a mixin-based context.
 */
public final class WorldRenderContextHelper {
    private WorldRenderContextHelper() {
    }

    public static Camera getCamera(Object context) {
        MixinRenderContext ctx = (MixinRenderContext) context;
        return ctx.camera();
    }

    public static PoseStack getMatrices(Object context) {
        MixinRenderContext ctx = (MixinRenderContext) context;
        return ctx.matrices();
    }

    public static MultiBufferSource getConsumers(Object context) {
        MixinRenderContext ctx = (MixinRenderContext) context;
        return ctx.consumers();
    }
}
