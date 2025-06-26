package com.owlmaddie.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.owlmaddie.chat.ChatDataManager.ChatSender;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;

/**
 * The {@code EntityChatDataLight} class represents the current displayed message, and no
 * previous messages or player message history. This is primarily used to broadcast the
 * currently displayed messages to players as they connect to the server.
 */
public class EntityChatDataLight {
    public UUID entityId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public ChatDataManager.ChatSender sender;
    public Map<UUID, PlayerData> players;
    public String characterSheet;
    // Constructor to initialize the light version from the full version
    public EntityChatDataLight(EntityChatData fullData, UUID playerId) {
        this.entityId = fullData.entityId;
        this.currentMessage = fullData.currentMessage;
        this.currentLineNumber = fullData.currentLineNumber;
        this.status = fullData.status;
        this.sender = fullData.sender;
        this.characterSheet = fullData.characterSheet;
        // Initialize the players map and add only the current player's data
        this.players = new HashMap<>();
        PlayerData playerData = fullData.getPlayerData(playerId);
        this.players.put(playerId, playerData);
    }
    public EntityChatDataLight(UUID entityId, String currentMessage, int currentLineNumber, ChatStatus status, ChatSender sender, String characterSheet, Map<UUID, PlayerData> players) {
        this.entityId = entityId;
        this.currentMessage = currentMessage;
        this.currentLineNumber = currentLineNumber;
        this.status = status;
        this.sender = sender;
        this.characterSheet = characterSheet;
        this.players = players;
    }

    public static EntityChatDataLight PendingData(UUID entityId){
        return  new EntityChatDataLight(entityId, "", 0, ChatStatus.PENDING, ChatSender.USER, "", null);
    }

    public String getCharacterProp(String propertyName) {
        // Create a case-insensitive regex pattern to match the property name and
        // capture its value
        Pattern pattern = Pattern.compile("-?\\s*" + Pattern.quote(propertyName) + ":\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(characterSheet);

        if (matcher.find()) {
            // Return the captured value, trimmed of any excess whitespace
            return matcher.group(1).trim().replace("\"", "");
        }

        return "N/A";
    }

}