package com.owlmaddie.player2.auth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.AlertScreen;

public class Player2OAuthScreen extends Screen {
    private final Screen parent;
    private final OAuthData data;
    private Button openBrowserButton;
    private Button cancelButton;
    private Button helpButton;

    public Player2OAuthScreen(Screen parent, OAuthData data) {
        super(Component.literal("Player2 Authentication"));
        this.parent = parent;
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int centerY = height / 2;

        // Title
        this.addRenderableWidget(new net.minecraft.client.gui.components.StringWidget(
                centerX - 100, centerY - 80, 200, 20,
                Component.literal("Player2 Authentication Required"),
                this.font));

        // Open browser button
        openBrowserButton = Button.builder(
                Component.literal("Authenticate"),
                button -> Player2OAuthHandler.openBrowser(data.verificationUri + "?user_code=" + data.userCode))
                .bounds(centerX - 75, centerY + 50, 150, 20).build();

        // Cancel button
        cancelButton = Button.builder(
                Component.literal("Cancel"),
                button -> onClose()).bounds(centerX - 75, centerY + 80, 150, 20).build();

        // Help button
        helpButton = Button.builder(
                Component.literal("Help"),
                button -> showHelp()).bounds(width - 125, centerY + 80, 100, 20).build();

        addRenderableWidget(openBrowserButton);
        addRenderableWidget(cancelButton);
        addRenderableWidget(helpButton);
    }

    private void showHelp() {
        minecraft.setScreen(new AlertScreen(
                new Runnable() {
                    @Override
                    public void run() {
                        minecraft.setScreen(Player2OAuthScreen.this);
                    }
                },
                Component.literal("Authentication Help"),
                Component.literal(
                        "1. Click 'Open Player2 Website' to open the verification page\n2. Enter the verification code shown above: "
                                + data.userCode
                                + "\n3. Log in to your Player2 account if needed\n4. Authorize the 'creature-pals-minecraft-mod' application\n5. The mod will automatically detect when authorization is complete\n\nIf the browser didn't open, manually visit:\n"
                                + data.verificationUri),
                Component.literal("OK"),
                true));
    }

    @Override
    public void onClose() {
        Player2OAuthHandler.resetAuthenticationState();
        if (parent != null) {
            minecraft.setScreen(parent);
        } else {
            minecraft.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }
    }

    private static class TitleScreen implements Runnable {
        @Override
        public void run() {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            }
        }
    }

    /**
     * Show a success notification when authentication is complete
     */
    public static void showSuccessNotification() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            // Always show success message, regardless of current screen
            client.setScreen(new AlertScreen(new TitleScreen(),
                    Component.literal("Authentication Successful!"),
                    Component.literal(
                            "Your Player2 API key has been automatically retrieved and saved.\n\nYou can now use all the mod features!"),
                    Component.literal("Continue"),
                    true));
        }
    }
}