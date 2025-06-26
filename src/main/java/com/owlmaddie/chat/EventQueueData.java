package com.owlmaddie.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.chat.ChatDataManager.ChatSender;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.network.ServerPackets;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventQueueData {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static long waitTimeAfterError = 10_000_000_000L; // wait 10 sec after err before doing any polling

    private UUID entityId;
    private Entity entity;
    private boolean isProcessing = false;

    private Deque<MessageData> llmQueue;
    private String userLanguage = null;
    private ServerPlayerEntity player = null;

    private long lastErrorTime = 0L;

    private EntityChatData getChatData() {
        return ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
    }

    private void updateUserLanguageAndPlayer(MessageData message) {
        this.userLanguage = message.userLanguage;
        this.player = message.player;
    }

    public void addMessage(MessageData message) {
        updateUserLanguageAndPlayer(message);
        llmQueue.add(message);
    }

    private void moveFromLLMQueueToChatData() {
        while (!llmQueue.isEmpty()) {
            // add all messages to chatData
            MessageData cur = llmQueue.poll();
            getChatData().addMessage(cur.userMessage, ChatDataManager.ChatSender.USER, cur.player);
        }
    }

    public boolean shouldProcess() {
        return isProcessing &&
                System.nanoTime() >= lastErrorTime + waitTimeAfterError;
    }

    public void process(Consumer<String> onUncleanResponse, Consumer<String> onError) {
        isProcessing = true;
        moveFromLLMQueueToChatData();
        LOGGER.info("Moved from LLMQueue To Chat Data, now generating message.");
        if (!getChatData().lastMsgIsUserMsg()) {
            isProcessing = false;
            return;
        }
        getChatData().generateEntityResponse(userLanguage, player, onUncleanResponse, onError);
        isProcessing = false;
    }

    public EventQueueData(UUID entityId, Entity entity) {
        this.entityId = entityId;
        this.entity = entity;
        llmQueue = new ArrayDeque<>();
    }

    public void generateCharacterSheet(MessageData data, Consumer<String> onCharacterSheet,
            Consumer<String> onError) {
        String systemPrompt = "system-character";
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(),
                systemPrompt);
        Map<String, String> contextData = getChatData().getPlayerContext(data.player, data.userLanguage, config);
        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData,
                List.of(new ChatMessage(data.userMessage, ChatSender.USER, player.getName().getString())), false, "")
                .thenAccept(char_sheet -> {
                    try {
                        if (char_sheet == null) {
                            throw new RuntimeException(ChatGPTRequest.lastErrorMessage);
                        }
                        onCharacterSheet.accept(char_sheet);
                    } catch (Exception e) {
                        onError.accept(e.getMessage() != null ? e.getMessage() : "");
                    }
                });
    }

    public UUID getId() {
        return entityId;
    }

    public void errorCooldown() {
        lastErrorTime = System.nanoTime();
    }

}
