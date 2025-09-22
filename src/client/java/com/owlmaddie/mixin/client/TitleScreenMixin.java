package com.owlmaddie.mixin.client;

import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.owlmaddie.player2.auth.Player2OAuthHandler;
import com.owlmaddie.player2.auth.Player2StartupHandler;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onTitleScreenRender(CallbackInfo ci) {
        Logger LOGGER = LoggerFactory.getLogger("creaturepals");

        // Check Player2 API key when title screen is rendered
        // This ensures it happens after the mixin is fully loaded
        if (!Player2OAuthHandler.hasCheckedApiKey()) {
            LOGGER.info("TitleScreenMixin: Title screen rendering, checking API key...");
            Player2StartupHandler.checkApiKeyOnStartup();
        }
    }
}