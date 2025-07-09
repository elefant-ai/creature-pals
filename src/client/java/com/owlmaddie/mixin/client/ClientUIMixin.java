package com.owlmaddie.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.owlmaddie.ui.TTSToggleButton;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

@Mixin(InGameHud.class)
public final class ClientUIMixin {
    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void clientRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        // TTSToggleButton.render(context);
    }
}