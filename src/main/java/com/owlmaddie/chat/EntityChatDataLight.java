// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;
import com.owlmaddie.chat.ChatDataManager.ChatSender;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;

/**
 * The {@code EntityChatDataLight} class represents the current displayed
 * message, and no
 * previous messages or player message history. This is primarily used to
 * broadcast the
 * currently displayed messages to players as they connect to the server.
 */
public class EntityChatDataLight {
    @Expose
    public String entityId;
    @Expose
    public String currentMessage;
    @Expose
    public int currentLineNumber;
    @Expose
    public ChatDataManager.ChatStatus status;
    @Expose
    public ChatDataManager.ChatSender sender;
    @Expose
    public Map<String, PlayerData> players;
    @Expose
    public String characterSheet;
    @Expose(serialize = false, deserialize = false)
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    // Constructor to initialize the light version from the full version
    public EntityChatDataLight(EntityChatData fullData, String playerName) {
        this.entityId = fullData.entityId;
        if (fullData.currentMessage != null) {
            this.currentMessage = fullData.currentMessage;
        } else {
            this.currentMessage = "";
        }
        this.currentLineNumber = fullData.currentLineNumber;
        this.status = fullData.status;
        this.sender = fullData.sender;
        this.characterSheet = fullData.characterSheet;

        // Initialize the players map and add only the current player's data
        this.players = new HashMap<>();
        PlayerData playerData = fullData.getPlayerData(playerName);
        this.players.put(playerName, playerData);
    }

    public EntityChatDataLight(String entityId, String currentMessage, int currentLineNumber, ChatStatus status,
            ChatSender sender, String characterSheet, Map<String, PlayerData> players) {
        this.entityId = entityId;
        if (currentMessage == null) {
            LOGGER.error("current message null!");
        }
        this.currentMessage = currentMessage;
        this.currentLineNumber = currentLineNumber;
        this.status = status;
        this.sender = sender;
        this.characterSheet = characterSheet;
        this.players = players;
    }

    public static EntityChatDataLight CreateData(String entityId) {
        return new EntityChatDataLight(entityId, "", 0, ChatStatus.NONE, ChatSender.USER, "", null);
    }

    public EntityChatDataLight(String entityId) {
        this.entityId = entityId;
        this.currentMessage = "";
        this.currentLineNumber = 0;
        this.status = ChatStatus.NONE;
        this.sender = ChatSender.USER;
        this.characterSheet = "";
        this.players = null;
    }

    // Get wrapped lines
    public List<String> getWrappedLines() {
        if (this.currentMessage == null) {
            LOGGER.info("nullWrapped entityID={}, currentLineNumber={}, status={}", entityId, currentLineNumber,
                    status);
        }
        return LineWrapper.wrapLines(this.currentMessage, ChatDataManager.MAX_CHAR_PER_LINE);
    }

    public boolean isEndOfMessage() {
        int totalLines = this.getWrappedLines().size();
        // Check if the current line number plus DISPLAY_NUM_LINES covers or exceeds the
        // total number of lines
        return currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES >= totalLines;
    }

    public PlayerData getPlayerData(String playerName) {
        if (this.players == null) {
            return new PlayerData();
        }

        // Check if the playerId exists in the players map
        if (this.players.containsKey("")) {
            // If a blank migrated legacy entity is found, always return this
            return this.players.get("");

        } else if (this.players.containsKey(playerName)) {
            // Return a specific player's data
            return this.players.get(playerName);

        } else {
            // Return a blank player data
            PlayerData newPlayerData = new PlayerData();
            this.players.put(playerName, newPlayerData);
            return newPlayerData;
        }
    }

}