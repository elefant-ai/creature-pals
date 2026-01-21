// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Helper class to abstract version-specific WorldRenderContext API differences.
 * This base version is for pre-1.21.10 Minecraft versions.
 */
public final class WorldRenderContextHelper {
    private WorldRenderContextHelper() {
    }

    public static Camera getCamera(Object context) {
        WorldRenderContext ctx = (WorldRenderContext) context;
        return ctx.camera();
    }

    public static PoseStack getMatrices(Object context) {
        WorldRenderContext ctx = (WorldRenderContext) context;
        return ctx.matrixStack();
    }

    public static MultiBufferSource getConsumers(Object context) {
        WorldRenderContext ctx = (WorldRenderContext) context;
        return ctx.consumers();
    }
}
