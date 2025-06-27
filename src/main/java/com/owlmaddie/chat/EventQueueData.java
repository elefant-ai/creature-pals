package com.owlmaddie.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.chat.ChatDataManager.ChatSender;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.utils.ServerEntityFinder;

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
    private long lastProcessTime = 0L;

    private boolean greetingRequested = false;
    private boolean generatedCharacter = false;

    private enum ProcessAction {
        Greeting,
        CharacterThenResponse,
        Response,
        NOOP
    };

    private EntityChatData getChatData() {
        return ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
    }

    private void updateUserLanguageAndPlayer(MessageData message) {
        this.userLanguage = message.userLanguage;
        this.player = message.player;
    }

    public void addUserMessage(Entity entity, String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        addMessage(new MessageData(userLanguage, player, userMessage, is_auto_message));
    }

    private void addMessage(MessageData message) {
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
                getAction() != ProcessAction.NOOP &&
                System.nanoTime() >= lastErrorTime + waitTimeAfterError;
    }

    private void processResponse(Consumer<String> onUncleanResponse, Consumer<String> onError) {
        moveFromLLMQueueToChatData();
        LOGGER.info("Moved from LLMQueue To Chat Data, now generating message.");
        if (!getChatData().lastMsgIsUserMsg()) {
            isProcessing = false;
            return;
        }
        getChatData().generateEntityResponse(userLanguage, player, onUncleanResponse, onError);
    }

    private ProcessAction getAction() {
        if (llmQueue.isEmpty()) {
            if (generatedCharacter || !greetingRequested)
                return ProcessAction.NOOP;
            return ProcessAction.Greeting;
        }
        if (generatedCharacter)
            return ProcessAction.Response;
        return ProcessAction.CharacterThenResponse;
    }

    public void requestGreeting(String userLangauge, ServerPlayerEntity player) {
        this.userLanguage = userLangauge;
        this.player = player;
        greetingRequested = true;
    }

    public void process(Consumer<String> onUncleanResponse, Consumer<String> onError,
            BiConsumer<String, Boolean> onCharacterSheetAndShouldGreet) {
        lastProcessTime = System.nanoTime();
        isProcessing = true;
        switch (getAction()) {
            case Greeting:
                generateCharacterSheet((characterSheet) -> {
                    generatedCharacter = true;
                    onCharacterSheetAndShouldGreet.accept(characterSheet, true);
                    isProcessing = false;
                }, (errMsg) -> {
                    onError.accept(errMsg);
                    isProcessing = false;
                });
                break;
            case CharacterThenResponse:
                generateCharacterSheet((characterSheet) -> {
                    generatedCharacter = true;
                    onCharacterSheetAndShouldGreet.accept(characterSheet, false);
                    processResponse(onUncleanResponse, onError);
                    isProcessing = false;
                }, (errMsg) -> {
                    onError.accept(errMsg);
                    isProcessing = false;
                });
                break;
            case Response:
                processResponse((uncleanRes) -> {
                    onUncleanResponse.accept(uncleanRes);
                    isProcessing = false;
                }, (errMsg) -> {
                    onError.accept(errMsg);
                    isProcessing = false;
                });
                break;
            case NOOP:
                isProcessing = false;
        }
    }

    public EventQueueData(UUID entityId, Entity entity) {
        this.entityId = entityId;
        this.entity = entity;
        llmQueue = new ArrayDeque<>();
    }

    private void generateCharacterSheet(Consumer<String> onCharacterSheet, Consumer<String> onError) {
        MessageData greetingMessage = MessageData.genCharacterAndOrGreetingMessage(userLanguage, player, entity);
        String systemPrompt = "system-character";
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(),
                systemPrompt);
        Map<String, String> contextData = getChatData().getPlayerContext(greetingMessage.player,
                greetingMessage.userLanguage, config);
        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData,
                List.of(new ChatMessage(greetingMessage.userMessage, ChatSender.USER, player.getName().getString())),
                false, "")
                .thenAccept(char_sheet -> {
                    try {
                        if (char_sheet == null) {
                            throw new RuntimeException(ChatGPTRequest.lastErrorMessage);
                        }
                        String characterName = Optional.ofNullable(getChatData().getCharacterProp("name"))
                                .filter(s -> !s.isEmpty())
                                .orElse("N/A");
                        if (characterName.equals("N/A")) {
                            throw new RuntimeException("Generated N/A or empty character name");
                        }
                        onCharacterSheet.accept(char_sheet);
                    } catch (Exception e) {
                        onError.accept(e.getMessage() != null ? e.getMessage() : "");
                    }
                });
    }

    public long getLastProcessTime() {
        return lastProcessTime;
    }

    public UUID getId() {
        return entityId;
    }

    public void errorCooldown() {
        lastErrorTime = System.nanoTime();
    }

    // private void addExternalEntityMessage(UUID entID, String userLanguage,
    // ServerPlayerEntity player,
    // String entityMessage,
    // String entityCustomName, String entityTypeName) {
    // if (entID.equals(entityId)) {
    // throw new Error("Tried to call add external entity Message with the same ent
    // id");
    // }
    // // EventQueueManager.blacklistedEntityId = null;
    // String newMessage = String.format("[%s the %s] said %s", entityCustomName,
    // entityTypeName, entityMessage);
    // MessageData toAdd = new MessageData(userLanguage, player, newMessage, false);
    // addMessage(toAdd);
    // }

    public boolean shouldDelete() {
        if (player != null) {
            return ServerEntityFinder.getEntityByUUID(player.getServerWorld(),
                    entityId) == null || !entity.isAlive();
        }
        return false;
    }
}
