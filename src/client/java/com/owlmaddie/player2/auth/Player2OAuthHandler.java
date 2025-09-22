package com.owlmaddie.player2.auth;

import java.awt.Desktop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handles Player2 Device Authorization Flow authentication
 */
public class Player2OAuthHandler {
    private static final String OAUTH_BASE_URL = "https://api.player2.game/v1";
    private static final String DEVICE_AUTH_URL = OAUTH_BASE_URL + "/login/device/new";
    private static final String OAUTH_TOKEN_URL = OAUTH_BASE_URL + "/login/device/token";
    private static final String CLIENT_ID = "01977e1e-0b52-7269-ac8d-97522d4c1c21";

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static boolean isAuthenticating = false;

    /**
     * Start the Device Authorization Flow
     */
    public static void startOAuthFlow(Consumer<OAuthData> onData, Runnable onSuccess) {
        if (isAuthenticating) {
            return; // Already authenticating
        }

        isAuthenticating = true;

        // First attempt a web login that may already be authorized for this client
        attemptWebLoginOrFallback(onData, onSuccess);
    }

    // Try GET /login/web/{CLIENT_ID}; if it returns p2Key, use it; else start
    // device authorization
    private static void attemptWebLoginOrFallback(Consumer<OAuthData> onData, Runnable onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = "http://localhost:4315/v1/login/web/" + CLIENT_ID;
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Web login probe status: " + response.statusCode());
                System.out.println("Web login probe body: " + response.body());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("p2Key")) {
                        String p2Key = json.get("p2Key").getAsString();
                        Player2StartupHandler.setApiKey(p2Key);
                        System.out.println("Obtained Player2 API key via web login probe.");
                        // Notify user on main thread and finish
                        // Reset auth state; no OAuth UI needed
                        onSuccess.run();
                        isAuthenticating = false;
                        return;
                    }
                }

                // Fallback to device authorization if not successful
                startDeviceAuthorization(onData, onSuccess);
            } catch (Exception e) {
                System.err.println("Web login probe failed: " + e.getMessage());
                // On any failure, fallback to device authorization
                startDeviceAuthorization(onData, onSuccess);
            }
        });
    }

    /**
     * Start the device authorization process
     */
    private static void startDeviceAuthorization(Consumer<OAuthData> onData, Runnable onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                // Step 1: Request device authorization
                JsonObject deviceRequest = new JsonObject();
                deviceRequest.addProperty("client_id", CLIENT_ID);
                deviceRequest.addProperty("scope", "api");

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(DEVICE_AUTH_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(deviceRequest.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Log the raw response for debugging
                System.out.println("Device authorization response status: " + response.statusCode());
                System.out.println("Device authorization response body: " + response.body());

                if (response.statusCode() == 200) {
                    JsonObject deviceResponse = JsonParser.parseString(response.body()).getAsJsonObject();

                    // Debug: Log the full response to see what fields are available
                    System.out.println("Device authorization response: " + deviceResponse.toString());

                    // Check if all required fields are present
                    if (!deviceResponse.has("deviceCode")) {
                        throw new RuntimeException("Missing 'deviceCode' field in response");
                    }
                    if (!deviceResponse.has("userCode")) {
                        throw new RuntimeException("Missing 'userCode' field in response");
                    }
                    if (!deviceResponse.has("verificationUri")) {
                        throw new RuntimeException("Missing 'verificationUri' field in response");
                    }
                    if (!deviceResponse.has("interval")) {
                        throw new RuntimeException("Missing 'interval' field in response");
                    }

                    // Extract device code and user code
                    String deviceCode = deviceResponse.get("deviceCode").getAsString();
                    String userCode = deviceResponse.get("userCode").getAsString();
                    String verificationUri = deviceResponse.get("verificationUri").getAsString();
                    int interval = deviceResponse.get("interval").getAsInt();
                    OAuthData data = new OAuthData(deviceCode, userCode, verificationUri, interval);
                    onData.accept(data);

                    // Start polling for token
                    startTokenPolling(deviceCode, interval, onSuccess);

                } else {
                    System.err.println("Device authorization request failed with status " + response.statusCode());
                    System.err.println("Response body: " + response.body());
                    throw new RuntimeException("Device authorization request failed with status "
                            + response.statusCode() + ": " + response.body());
                }

            } catch (Exception e) {
                System.err.println("Device authorization failed: " + e.getMessage());
                isAuthenticating = false;
            }
        });
    }

    /**
     * Poll for the access token
     */
    private static void startTokenPolling(String deviceCode, int interval, Runnable onSuccess) {
        executor.scheduleWithFixedDelay(() -> {
            try {
                JsonObject tokenRequest = new JsonObject();
                tokenRequest.addProperty("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
                tokenRequest.addProperty("client_id", CLIENT_ID);
                tokenRequest.addProperty("device_code", deviceCode);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(OAUTH_TOKEN_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(tokenRequest.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Debug: Log the token polling response
                System.out.println("Token polling response status: " + response.statusCode());
                System.out.println("Token polling response body: " + response.body());

                if (response.statusCode() == 200) {
                    JsonObject tokenResponse = JsonParser.parseString(response.body()).getAsJsonObject();

                    if (tokenResponse.has("p2Key")) {
                        String p2Key = tokenResponse.get("p2Key").getAsString();

                        // Store the token
                        Player2StartupHandler.setApiKey(p2Key);

                        // Stop polling
                        executor.shutdown();

                        System.out.println("Successfully obtained Player2 API key via Device Authorization Flow");

                        // Notify the user on the main thread
                        onSuccess.run();
                    }
                } else if (response.statusCode() == 400) {
                    // Still pending, continue polling
                    JsonObject errorResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (errorResponse.has("error")
                            && "authorization_pending".equals(errorResponse.get("error").getAsString())) {
                        // This is expected, continue polling
                        return;
                    }
                }

            } catch (Exception e) {
                System.err.println("Token polling failed: " + e.getMessage());
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * Open the user's default web browser
     */
    public static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for systems without desktop support
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("cmd /c start " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }

    /**
     * Check if we're currently in the authentication process
     */
    public static boolean isAuthenticating() {
        return isAuthenticating;
    }

    /**
     * Reset authentication state
     */
    public static void resetAuthenticationState() {
        isAuthenticating = false;
    }
}