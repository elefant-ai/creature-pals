package com.owlmaddie.player2.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.network.ClickEventHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class OAuthThroughChat {
        private static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

        public static void handleAuthError() {
                Player2OAuthHandler.startOAuthFlow(data -> {
                        LOGGER.info("Starting oauth flow through chat");
                        String chatMsg = String.format("To use AI features, please authorize here: %s",
                                        data.verificationUriComplete);
                        Minecraft.getInstance().player.displayClientMessage(
                                        Component.literal(chatMsg)
                                                        .withStyle(Style.EMPTY
                                                                        .withClickEvent(ClickEventHelper.openUrl(
                                                                                        data.verificationUriComplete))),
                                        false);
                }, () -> {
                        Minecraft.getInstance().player.displayClientMessage(
                                        Component.literal(
                                                        "Your Player2 API key has been automatically retrieved and saved."
                                                                        + "You can now use all the mod features!"),
                                        false);
                });
        }
}