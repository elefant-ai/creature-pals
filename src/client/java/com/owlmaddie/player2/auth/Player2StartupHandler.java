package com.owlmaddie.player2.auth;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
// import net.minecraft.client.gui.screens.dialog.ButtonListDialogScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Handles Player2 API key validation and setup on Minecraft startup
 */
public class Player2StartupHandler {
    static Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    /**
     * Show the API key error screen
     */
    private static void showApiKeyErrorScreen(String errorMessage) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen != null) {
            client.setScreen(new Player2ApiKeyErrorScreen(client.screen, errorMessage));
        }
    }

    /**
     * Screen for setting up the Player2 API key
     */
    public static class Player2ApiKeySetupScreen extends Screen {
        private final Screen parent;
        private EditBox apiKeyField;
        private Button saveButton;
        private Button skipButton;
        private Button helpButton;

        public Player2ApiKeySetupScreen(Screen parent) {
            super(Component.literal("Player2 API Key Setup"));
            this.parent = parent;
            LOGGER.debug("Player2ApiKeySetupScreen: Constructor called with parent: "
                    + (parent != null ? parent.getClass().getSimpleName() : "null"));
        }

        @Override
        protected void init() {
            super.init();
            LOGGER.debug("Player2ApiKeySetupScreen: init() called");

            int centerX = width / 2;
            int centerY = height / 2;

            // Title
            addRenderableWidget(Button.builder(
                    Component.literal("Player2 API Key Required"),
                    button -> {
                    }).bounds(centerX - 100, centerY - 80, 200, 20).build());

            // Description
            addRenderableWidget(Button.builder(
                    Component.literal("This mod requires a Player2 API key to function."),
                    button -> {
                    }).bounds(centerX - 150, centerY - 50, 300, 20).build());

            addRenderableWidget(Button.builder(
                    Component.literal("Get your key at: player2.game"),
                    button -> {
                    }).bounds(centerX - 150, centerY - 30, 300, 20).build());

            // API Key input field
            apiKeyField = new net.minecraft.client.gui.components.EditBox(
                    minecraft.font,
                    centerX - 100,
                    centerY,
                    200,
                    20,
                    Component.literal("Enter your Player2 API key"));

            apiKeyField.setMaxLength(100);
            apiKeyField.setResponder(text -> {
                saveButton.active = !text.trim().isEmpty();
            });

            // Save button
            saveButton = Button.builder(
                    Component.literal("Save & Validate"),
                    button -> saveApiKey()).bounds(centerX - 100, centerY + 30, 200, 20).build();
            saveButton.active = false;

            // Skip button
            skipButton = Button.builder(
                    Component.literal("Skip for now"),
                    button -> onClose()).bounds(centerX - 100, centerY + 60, 200, 20).build();

            // Help button
            helpButton = Button.builder(
                    Component.literal("Help"),
                    button -> showHelp()).bounds(centerX - 100, centerY + 90, 200, 20).build();

            addWidget(apiKeyField);
            addRenderableWidget(apiKeyField);
            addRenderableWidget(saveButton);
            addRenderableWidget(skipButton);
            addRenderableWidget(helpButton);

            setInitialFocus(apiKeyField);
        }

        private void saveApiKey() {
            String apiKey = apiKeyField.getMessage().getString(); // .getString(); // instead of getString
                                                                  // maybe tryToCollapseToString()
            if (apiKey.isEmpty()) {
                return;
            }

            // Set the API key in system property for this session
            try {
                System.setProperty("PLAYER2_API_KEY", apiKey);

                // Validate the key
                Player2OAuthHandler.validateApiKey(apiKey, Player2ApiKeyErrorScreen.onValidationErrStr);

                // Show success message
                minecraft.setScreen(new ConfirmScreen(
                        this::onValidationComplete,
                        Component.literal("API Key Saved"),
                        Component.literal(
                                "Your Player2 API key has been saved for this session.\n\nNote: You'll need to set this as an environment variable for permanent storage."),
                        Component.literal("Continue"),
                        Component.literal("Exit")
                // ButtonListDialogScreen.DISCONNECT
                ));

            } catch (Exception e) {
                minecraft.setScreen(new ConfirmScreen(
                        button -> minecraft.setScreen(this),
                        Component.literal("Error"),
                        Component.literal("Failed to save API key: " + e.getMessage()),
                        Component.literal("OK"),
                        Component.literal("Exit")
                // ButtonListDialogScreen.DISCONNECT
                ));
            }
        }

        private void onValidationComplete(boolean confirmed) {
            if (confirmed) {
                onClose();
            }
        }

        private void showHelp() {
            minecraft.setScreen(new ConfirmScreen(
                    button -> minecraft.setScreen(this),
                    Component.literal("How to Set Player2 API Key"),
                    Component.literal(
                            "1. Visit player2.game and sign up\n2. Get your API key from your account\n3. Set it as an environment variable:\n\nWindows (PowerShell):\n$env:PLAYER2_API_KEY=\"your_key\"\n\nWindows (CMD):\nset PLAYER2_API_KEY=your_key\n\nLinux/macOS:\nexport PLAYER2_API_KEY=\"your_key\"\n\n4. Restart Minecraft"),
                    Component.literal("OK"),
                    Component.literal("Exit")
            // ButtonListDialogScreen.DISCONNECT

            ));
        }

        @Override
        public void onClose() {
            if (parent != null) {
                minecraft.setScreen(parent);
            } else {
                // If no parent screen, go back to title screen
                minecraft.setScreen(new TitleScreen());
            }
        }

        /**
         * Show the API key setup screen
         */
        private static void showApiKeySetupScreen() {
            LOGGER.info("Player2StartupHandler: showApiKeySetupScreen called");
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                // If we're on the title screen, show the OAuth screen directly
                // Otherwise, show it on the current screen
                Screen currentScreen = client.screen;
                LOGGER.info("Player2StartupHandler: Current screen: "
                        + (currentScreen != null ? currentScreen.getClass().getSimpleName() : "null"));
                Runnable onSuccess = () -> {
                    Minecraft.getInstance().execute(() -> {
                        Player2OAuthScreen.showSuccessNotification();
                    });
                };

                if (currentScreen instanceof TitleScreen) {
                    LOGGER.info("Player2StartupHandler: On title screen, starting OAuth flow");
                    Consumer<OAuthData> onData = (data) -> {
                        // Show the authentication screen with the user code
                        Minecraft.getInstance().execute(() -> {
                            Minecraft mcClient = Minecraft.getInstance();
                            if (mcClient != null) {
                                mcClient.setScreen(new Player2OAuthScreen(null, data));
                            }
                        });
                    };
                    Player2OAuthHandler.startOAuthFlow(onData, onSuccess);
                } else if (currentScreen != null) {
                    LOGGER.info("Player2StartupHandler: On other screen, starting OAuth flow");
                    Consumer<OAuthData> onData = (data) -> {
                        // Show the authentication screen with the user code
                        Minecraft.getInstance().execute(() -> {
                            Minecraft mcClient = Minecraft.getInstance();
                            if (mcClient != null) {
                                mcClient.setScreen(new Player2OAuthScreen(currentScreen, data));
                            }
                        });
                    };
                    Player2OAuthHandler.startOAuthFlow(onData, onSuccess);
                }
            } else {
                LOGGER.warn("Player2StartupHandler: Minecraft client is null!");
            }
        }

    }

    /**
     * Screen for showing API key validation errors
     */
    public static class Player2ApiKeyErrorScreen extends Screen {
        private final Screen parent;
        private final String errorMessage;

        public Player2ApiKeyErrorScreen(Screen parent, String errorMessage) {
            super(Component.literal("Player2 API Key Error"));
            this.parent = parent;
            this.errorMessage = errorMessage;
        }

        public static final Consumer<String> onValidationErrStr = (errStr) -> {
            // Show error screen on main thread
            Minecraft.getInstance().execute(() -> {
                showApiKeyErrorScreen(errStr);
            });
        };

        @Override
        protected void init() {
            super.init();

            int centerX = width / 2;
            int centerY = height / 2;

            // Error title
            addRenderableWidget(Button.builder(
                    Component.literal("API Key Validation Failed"),
                    button -> {
                    }).bounds(centerX - 100, centerY - 80, 200, 20).build());

            // Error message
            addRenderableWidget(Button.builder(
                    Component.literal("Error: " + errorMessage),
                    button -> {
                    }).bounds(centerX - 100, centerY - 50, 300, 20).build());

            // Retry button
            addRenderableWidget(Button.builder(
                    Component.literal("Retry"),
                    button -> retryValidation(Player2ApiKeySetupScreen::showApiKeySetupScreen, onValidationErrStr))
                    .bounds(centerX - 100, centerY, 200, 20).build());

            // Setup button
            addRenderableWidget(Button.builder(
                    Component.literal("Setup New Key"),
                    button -> setupNewKey()).bounds(centerX - 100, centerY + 30, 200, 20).build());

            // Continue anyway button
            addRenderableWidget(Button.builder(
                    Component.literal("Continue Anyway"),
                    button -> onClose()).bounds(centerX - 100, centerY + 60, 200, 20).build());
        }

        private void retryValidation(Runnable onNotFoundAPiKey, Consumer<String> onValidationErrStr) {
            Player2OAuthHandler.revalidateApiKey(onNotFoundAPiKey, onValidationErrStr);
            onClose();
        }

        private void setupNewKey() {
            minecraft.setScreen(new Player2ApiKeySetupScreen(this));
        }

        @Override
        public void onClose() {
            if (parent != null) {
                minecraft.setScreen(parent);
            } else {
                // If no parent screen, go back to title screen
                minecraft.setScreen(new TitleScreen());
            }
        }

    }

    public static void checkApiKeyOnStartup() {
        Player2OAuthHandler.checkApiKey(Player2ApiKeySetupScreen::showApiKeySetupScreen,
                Player2ApiKeyErrorScreen.onValidationErrStr);
    }
}