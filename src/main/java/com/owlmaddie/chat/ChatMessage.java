// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import com.google.gson.annotations.Expose;

/**
 * The {@code ChatMessage} class represents a single message.
 */
public class ChatMessage {
    @Expose
    public String message;
    @Expose
    public String name;
    @Expose
    public ChatDataManager.ChatSender sender;
    @Expose
    public Long timestamp;

    public ChatMessage(String message, ChatDataManager.ChatSender sender, String playerName) {
        this.message = message;
        this.sender = sender;
        this.name = playerName;
        this.timestamp = System.currentTimeMillis();
    }
}