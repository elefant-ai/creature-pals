// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Helper class to abstract version-specific WorldRenderContext API differences.
 * This version is for 1.21.10+ with the updated Fabric API.
 */
public final class WorldRenderContextHelper {
    private WorldRenderContextHelper() {
    }

    public static Camera getCamera(Object context) {
        // In 1.21.10+, camera() was removed from context, get from Minecraft client
        return Minecraft.getInstance().gameRenderer.getMainCamera();
    }

    public static PoseStack getMatrices(Object context) {
        WorldRenderContext ctx = (WorldRenderContext) context;
        // In 1.21.10+, matrixStack() was renamed to matrices()
        return ctx.matrices();
    }

    public static MultiBufferSource getConsumers(Object context) {
        WorldRenderContext ctx = (WorldRenderContext) context;
        return ctx.consumers();
    }
}
