package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.LOGGER;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.owlmaddie.chat.ChatDataManager.ChatStatus;
import com.owlmaddie.chat.MessageData.MessageDataType;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.utils.ServerEntityFinder;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class EventQueueData {
    String entityId;
    Entity entity;
    Deque<MessageData> queue;
    long lastTimePolled;
    long randomInterval;
    MessageData lastMessageData;
    String characterName;
    boolean immediatePolling = false;

    public EventQueueData(String entityId, Entity entity) {
        this.entityId = entityId;
        this.entity = entity;
        queue = new ArrayDeque<>();
        lastTimePolled = System.nanoTime();
        // poll between 1000 ms -> 1500s
        randomInterval = ThreadLocalRandom.current().nextLong(1_000_000_000L, 1_500_000_001L);
        lastMessageData = null;
    }

    public boolean shouldPoll() {
        boolean shouldPoll = entity != null &&
                entityId != EventQueueManager.blacklistedEntityId &&
                lastMessageData != null &&
                lastMessageData.player != null &&
                !queue.isEmpty() &&
                !EventQueueManager.llmProcessing && // technically dont need this but in case
                System.nanoTime() > lastTimePolled + randomInterval &&
                lastMessageData.player.distanceTo(entity) < EventQueueManager.maxDistance &&
                !EventQueueManager.shouldWaitBecauseOfError()
                && !immediatePolling;
        if (!shouldPoll) {
            return false;
        }
        // start polling:
        return true;
    }

    public void add(MessageData toAdd) {
        queue.addLast(toAdd);
        if (needToGenCharacter()) {
            // if we are calling add, then no need to send greeting because conversation has
            // started already
            addOnlyCharacterToQueue(toAdd.userLanguage, toAdd.player);
        } else if (queue.size() > 3) {
            // discard if queue size is too much
            queue.poll();
        }
    }

    public void addOnlyCharacterToQueue(String userLanguage, ServerPlayerEntity player) {
        addCharacterAndMaybeGreeting(userLanguage, player, MessageDataType.Character);
    }

    // use to check if queue will generate character
    public boolean queueContainsCharacterGen() {
        if (queue.isEmpty()) {
            return false;
        }
        MessageDataType firstType = queue.getFirst().type;
        return firstType == MessageDataType.GreetingAndCharacter || firstType == MessageDataType.Character;
    }

    public void addGreeting(String userLanguage, ServerPlayerEntity player) {
        addCharacterAndMaybeGreeting(userLanguage, player, MessageDataType.GreetingAndCharacter);
    }

    private void addCharacterAndMaybeGreeting(String userLanguage, ServerPlayerEntity player,
            MessageDataType type) {

        // GUARDS
        if (type != MessageDataType.Character && type != MessageDataType.GreetingAndCharacter) {
            throw new Error("Tried to call addCharacterAndMaybeGreeting with wrong type!");
        }
        if (!needToGenCharacter()) {
            // if character has been set, do not send greeting/regenerate character
            LOGGER.warn("Character name already set! Cancelling generateCharacter request");
            return;
        }

        if (queueContainsCharacterGen()) {
            if (queue.getFirst().type == MessageDataType.Character) {
                // already have Character queued up
                LOGGER.info("Cancelling addGreeting, already have characterQueued up");
                return;
            }
            // removes character gen so that can be replaced with greeting
            queue.pollFirst();
        }
        LOGGER.info("Adding/updating queue to contain greeting/characterGen");
        MessageData toAdd = MessageData.genCharacterAndOrGreetingMessage(userLanguage, player, entity, type);
        lastMessageData = toAdd;
        queue.addFirst(toAdd);
    }

    private boolean needToGenCharacter() {
        if (characterName != null && !characterName.equals("N/A")) {
            return false;
        }
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        String name = chatData.getCharacterProp("name");
        if (name == null) {
            return true;
        }
        characterName = name;
        return true;
    }

    // MAKE SURE THAT EXTERNAL ENTITY IS DIFFERENT FROM CURRENT WHEN CALLING THIS:
    public void addExternalEntityMessage(String userLanguage, ServerPlayerEntity player, String entityMessage,
            String entityCustomName, String entityTypeName) {
        EventQueueManager.blacklistedEntityId = null;
        String newMessage = String.format("[%s the %s] said %s", entityCustomName, entityTypeName, entityMessage);
        MessageData toAdd = new MessageData(userLanguage, player, newMessage, false, MessageDataType.Normal);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public void addUserMessage(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        EventQueueManager.blacklistedEntityId = null;
        LOGGER.info(String.format("EventQueueData/addUserMessage (%s) to entity (%s)", userMessage, entityId));
        MessageData toAdd = new MessageData(userLanguage, player, userMessage, is_auto_message, MessageDataType.Normal);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public boolean shouldDelete() {
        if (lastMessageData != null && lastMessageData.player != null) {
            return ServerEntityFinder.getEntityByUUID(lastMessageData.player.getServerWorld(),
                    UUID.fromString(entityId)) == null || !entity.isAlive();
        }
        return false;
    }

    public void immediateGreeting() {
        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) immediate greeting", entityId));

        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        immediatePolling = true;
        chatData.setStatus(ChatStatus.PENDING);
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(lastMessageData.player, (MobEntity) entity, 3.5F);
        EntityBehaviorManager.addGoal((MobEntity) entity, talkGoal, GoalPriority.TALK_PLAYER);
        MessageData greetingMsg = queue.poll();
        chatData.generateCharacterMessage(greetingMsg, (name) -> {
            // on characterName
            setCharacterName(name);
            LOGGER.info(
                    String.format("EventQueueData/injectOnServerTick(entity %s) generated character with name (%s)",
                            entityId, characterName));
            // if generated character without greeting, then should poll if possible so that
            // it talks
            if (!queue.isEmpty()) {
                messagePoll(chatData, false);
            } else {
                doneImmediatePolling();
            }
        }, (errMsg) -> {
            onError(errMsg);
            LOGGER.info(String.format(
                    "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                    entityId, errMsg));
            doneImmediatePolling();
        }, (greetMsg) -> {
            onGreetingGenerated(greetMsg);
            doneImmediatePolling();
        });
        return;
    }

    public void bubblePoll() {
        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) Bubble poll", entityId));
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        immediatePolling = true;
        chatData.setStatus(ChatStatus.PENDING);
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(lastMessageData.player, (MobEntity) entity, 3.5F);
        EntityBehaviorManager.addGoal((MobEntity) entity, talkGoal, GoalPriority.TALK_PLAYER);
        messagePoll(chatData, true);
    }

    public void poll() {
        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) process event queue", entityId));
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        startPolling(chatData);
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(lastMessageData.player, (MobEntity) entity, 3.5F);
        EntityBehaviorManager.addGoal((MobEntity) entity, talkGoal, GoalPriority.TALK_PLAYER);

        // Need to send character msg/greeting
        if (queueContainsCharacterGen()) {
            MessageData greetingMsg = queue.poll();
            chatData.generateCharacterMessage(greetingMsg, (name) -> {
                // on characterName
                setCharacterName(name);
                LOGGER.info(
                        String.format("EventQueueData/injectOnServerTick(entity %s) generated character with name (%s)",
                                entityId, characterName));
                // if generated character without greeting, then should poll if possible so that
                // it talks
                if (!queue.isEmpty()) {
                    messagePoll(chatData, false);
                } else {
                    donePolling();
                }
            }, (errMsg) -> {
                onError(errMsg);
                LOGGER.info(String.format(
                        "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                        entityId, errMsg));
                donePolling();
            }, (greetMsg) -> {
                onGreetingGenerated(greetMsg);
                donePolling();
            });
            return; // dont continue to generate other msg
        } else {
            messagePoll(chatData, false);
        }
    }

    public void messagePoll(EntityChatData chatData, boolean isBubblePoll) {
        while (!queue.isEmpty()) {
            // add all messages to chatData
            MessageData cur = queue.poll();
            chatData.addMessage(cur.userMessage, ChatDataManager.ChatSender.USER, cur.player, "system-chat");
        }
        LOGGER.info(
                String.format("EventQueueData/injectOnServerTick(entity %s) done polling, generating msg", entityId));
        chatData.generateEntityResponse(lastMessageData.userLanguage, lastMessageData.player, (responseMsg) -> {
            LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) generated message (%s)",
                    entityId, responseMsg));
            onMessageGenerated(responseMsg);
            if (isBubblePoll) {
                doneImmediatePolling();
            } else {
                donePolling();
            }
        }, (errMsg) -> {
            onError(errMsg);
            if (isBubblePoll) {
                doneImmediatePolling();
            } else {
                donePolling();
            }
        });
    }

    public void startPolling(EntityChatData chatData) {
        EventQueueManager.llmProcessing = true;
        LOGGER.info("Start polling");
        chatData.setStatus(ChatStatus.PENDING);
    }

    public void donePolling() {
        lastTimePolled = System.nanoTime();
        // EntityChatData chatData =
        // ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        // chatData.setStatus(ChatStatus.DISPLAY); // do not set this here. Need to set
        // on client after message is generated.
        EventQueueManager.llmProcessing = false;
    }

    public void doneImmediatePolling() {
        lastTimePolled = System.nanoTime();
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        immediatePolling = false;
    }

    // SIDE EFFECTS

    public void onMessageGenerated(String message) {
        if (message.isBlank()) {
            EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
            LOGGER.info("Assistant generated empty message in order not to respond");
            chatData.setStatus(ChatStatus.DISPLAY);
            return;
        }
        // TODO: BROADCAST ENTITY MESSAGE HERE:
        if (entity.getCustomName() == null) {
            return;
        }
        String entityCustomName = entity.getCustomName().getString();

        String entityType = entity.getType().getName().getString();
        lastMessageData.player.server.getPlayerManager().broadcast(Text.of("<" + entityCustomName
                + " the " + entityType + "> " + message), false);

        // add msg to all nearby entities: (broken, maybe fix later)
        // EventQueueManager.addEntityMessageToAllClose(entity,
        // lastMessageData.userLanguage, lastMessageData.player, message,
        // entityCustomName, entityType);

    }

    public void onGreetingGenerated(String message) {
        if (message.isBlank()) {
            message = "Hello!"; // Do not want to send "" for greeting message as this starts the conversation
        }
        onMessageGenerated(message);
    }

    public void onError(String errorMessage) {
        EventQueueManager.onError();
        ServerPackets.SendClickableError(lastMessageData.player, errorMessage, "https://elefant.gg/discord");
    }

    public void setCharacterName(String name) {
        if (!name.equals("N/A")) {
            this.characterName = name;
        }
    }
}
