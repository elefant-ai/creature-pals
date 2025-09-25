package com.owlmaddie.chat;

import java.util.concurrent.ConcurrentHashMap;

public class ClientChatDataManager {
    public static ConcurrentHashMap<String, EntityChatDataLight> entityChatDataMap = new ConcurrentHashMap<>();

    public static EntityChatDataLight getOrCreateChatData(String entityId) {
        return entityChatDataMap.computeIfAbsent(entityId, k -> EntityChatDataLight.CreateData(entityId));
    }

    public static void clearData() {
        // Clear the chat data for the previous session
        entityChatDataMap.clear();
    }
}
