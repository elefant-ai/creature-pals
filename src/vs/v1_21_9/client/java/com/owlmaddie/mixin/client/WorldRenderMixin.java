// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.owlmaddie.ClientInit;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.utils.TickDelta;
import com.owlmaddie.utils.WorldRenderContextHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Active mixin for MC 1.21.9 that injects world rendering.
 * Fabric API removed WorldRenderEvents in 1.21.9, so we use mixin injection.
 * This supersedes the base no-op WorldRenderMixin.
 */
@Mixin(LevelRenderer.class)
public class WorldRenderMixin {

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=blockentities",
            ordinal = 0
        )
    )
    private void onAfterEntities(
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightTexture,
            Matrix4f modelView,
            Matrix4f projection,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        // Create a rendering context wrapper for BubbleRenderer
        PoseStack matrices = new PoseStack();
        matrices.mulPose(modelView);

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

        // Create a context object that WorldRenderContextHelper can use
        MixinRenderContext ctx = new MixinRenderContext(camera, matrices, bufferSource, partialTick);

        long tickCounter = ClientInit.getTickCounter();
        float tickDelta = TickDelta.get(ctx);

        BubbleRenderer.drawTextAboveEntities(ctx, tickCounter, tickDelta);
    }

    /**
     * Simple context wrapper for mixin-based rendering.
     */
    public static class MixinRenderContext {
        private final Camera camera;
        private final PoseStack matrices;
        private final MultiBufferSource consumers;
        private final float tickDelta;

        public MixinRenderContext(Camera camera, PoseStack matrices, MultiBufferSource consumers, float tickDelta) {
            this.camera = camera;
            this.matrices = matrices;
            this.consumers = consumers;
            this.tickDelta = tickDelta;
        }

        public Camera camera() {
            return camera;
        }

        public PoseStack matrices() {
            return matrices;
        }

        public MultiBufferSource consumers() {
            return consumers;
        }

        public float tickDelta() {
            return tickDelta;
        }
    }
}
