package com.owlmaddie.player2;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.google.gson.*;

import java.io.*;
import java.net.URL;
import java.util.Map;

public class Player2APIService {
    
    private static final String BASE_URL = "http://127.0.0.1:4315"; // ACTUAL


    /**
     * Handles boilerplate logic for interacting with the API endpoint
     *
     * @param endpoint The API endpoint (e.g., "/v1/chat/completions").
     * @param postRequest True -> POST request, False -> GET request
     * @param requestBody JSON payload to send.
     * @return A map containing JSON keys and values from the response.
     * @throws Exception If there is an error.
     */
    private static Map<String, JsonElement> sendRequest(String endpoint, boolean postRequest, JsonObject requestBody) throws Exception {
        URL url = new URI(BASE_URL + endpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(postRequest ? "POST" : "GET");

        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("accept", "application/json; charset=utf-8");
        connection.setRequestProperty("player2-game-key", "creature-chat-evolved");

        System.out.printf("Sending %s request to %s\n", postRequest? "POST":"GET", endpoint);


        if (postRequest && requestBody != null) {
            System.out.printf("Request Body: %s\n", requestBody);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }



        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            // read error info:
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                System.err.println("Error response: " + errorResponse);
            }
            throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
        }


        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        Map<String, JsonElement> responseMap = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonResponse.entrySet()) {
            responseMap.put(entry.getKey(), entry.getValue());
        }
        System.out.printf("DONE %s request to %s \n", postRequest? "POST":"GET", endpoint);

        return responseMap;
    }

    public static void sendHeartbeat(){
        try{
            System.out.println("Sending Heartbeat");
            Map<String, JsonElement> responseMap = sendRequest("/v1/health", false, null);
            if(responseMap.containsKey("client_version")){
                System.out.println("Heartbeat Successful");
            }
        }
        catch(Exception e){
            System.err.printf("Heartbeat Fail: %s",e.getMessage());
        }
    }
}
