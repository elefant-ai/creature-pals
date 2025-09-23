package com.owlmaddie.chat;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import com.owlmaddie.commands.ConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.network.PacketHelper.TriConsumer;
import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import static com.owlmaddie.network.ServerPackets.serverInstance;

public class EventQueueManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    private static boolean addingEntityQueues = false;
    private static Set<ServerPlayer> unauthPlayers = new HashSet<>();

    private static class LLMCompleter {
        private boolean isProcessing = false;

        public boolean isAvailable() {
            return !isProcessing;
        }

        public void process(String entityId, BiConsumer<String, ServerPlayer> onUncleanResponse,
                BiConsumer<String, ServerPlayer> onError,
                TriConsumer<String, Boolean, ServerPlayer> onCharacterSheetAndShouldGreet) {

            LOGGER.info("LLMCompleter/processing entityId={}", entityId);
            isProcessing = true;
            queueData.get(entityId).process((resp, player) -> {
                LOGGER.info("LLMCompleter/doneProcessing/Success entityId={} resp={}", entityId, resp);
                onUncleanResponse.accept(resp, player);
                isProcessing = false;
            }, (errMsg, player) -> {
                LOGGER.info("LLMCompleter/doneProcessing/Error entityId={} errMsg={}", entityId, errMsg);
                onError.accept(errMsg, player);
                isProcessing = false;
            }, (characterSheet, shouldGreet, player) -> {
                LOGGER.info("LLMCompleter/doneProcessing/Greeting entityId={} characterSheet={} shouldGreet={}",
                        entityId, characterSheet, shouldGreet);
                onCharacterSheetAndShouldGreet.accept(characterSheet, shouldGreet, player);
                isProcessing = !shouldGreet; // if we do not greet, then we continue processing.
            });
        }
    }

    private static List<LLMCompleter> completers = List.of(new LLMCompleter()); // TODO: add another completer, more if
                                                                                // premium

    private static ConcurrentHashMap<String, EventQueueData> queueData = new ConcurrentHashMap<>();

    // entitys to add next tick. EventQueueData only has entityId, so need to loop
    // through all players to find entity object
    private static Set<String> entityIdsToAdd = new HashSet<>();

    public static void addEntityIdToCreate(String entityId) {
        entityIdsToAdd.add(entityId);
    }

    public static void addGreeting(Entity entity, EntityChatData chatData, String userLangauge, ServerPlayer player, boolean is_auto_message) {
        if (player == null) {
            throw new RuntimeException("Null player for addGreeting");
        }
        ConfigurationHandler.Config config = new ConfigurationHandler(serverInstance).loadConfig();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        if (!manager.handleAutoResponse(chatData, player, is_auto_message, config)) {
            return;
        }
        LOGGER.info("AddGreeting, entityId={} playerID={}", entity.getUUID(), player.getUUID());
        ClientSideEffects.setPending(entity.getStringUUID());

        getOrCreateQueueData(entity.getStringUUID(), entity).requestGreeting(userLangauge, player);
    }

    private static Optional<String> getEntityIdToProcess(MinecraftServer server) {
        return queueData.values().stream()
                .filter(EventQueueData::shouldProcess)
                .filter((data) -> !unauthPlayers.contains(data.getPlayer()))
                .max(Comparator.comparingInt(EventQueueData::getPriority))
                .map(EventQueueData::getId);
    }

    private static void errorCooldown(String entityId) {
        queueData.get(entityId).errorCooldown();
    }

    public static void injectOnServerTick(MinecraftServer server) {
        // first make sure queueData is up to date (as much as possible,
        // because maybe no players have tracked entity)
        tryAddAllNewEntities(server);
        removeDeadEntities();
        if (addingEntityQueues) {
            return;
        }
        for (LLMCompleter completer : completers) {
            // completers.forEach((completer) -> {
            if (!completer.isAvailable()) {
                return;
            }
            // find entityId and player somehow
            Optional<String> entityIdOption = getEntityIdToProcess(server);
            entityIdOption.ifPresent(
                    (entityId) -> {
                        LOGGER.info("PRESENT " + entityId);
                        ClientSideEffects.setPending(entityId);
                        completer.process(entityId, (uncleanMsg, player) -> {
                            ClientSideEffects.onEntityGeneratedMessage(entityId, uncleanMsg, player);
                        }, (errMsg, player) -> {
                            ClientSideEffects.onLLMGenerateError(entityId, errMsg, player);
                            // make entity on cooldown
                            errorCooldown(entityId);
                        }, (characterSheet, shouldGreet, player) -> {
                            ClientSideEffects.onCharacterSheetGenerated(entityId, characterSheet, shouldGreet, player);
                        });
                    });
            // });
        }
    }

    public static EventQueueData getOrCreateQueueData(String entityId, Entity entity) {
        return queueData.computeIfAbsent(entityId, k -> {
            LOGGER.info(String.format("EventQueueManager/creating new queue data for ent id (%s)", entityId));
            return new EventQueueData(entityId, entity);
        });
    }

    private static void removeDeadEntities() {
        for (EventQueueData curQueue : queueData.values()) {
            // remove entity if despawn/died so dont poll and err:
            if (curQueue.shouldDelete()) { // if entity died, etc.
                queueData.remove(curQueue.getId());
                continue;
            }
        }
    }

    private static void tryAddAllNewEntities(MinecraftServer server) {
        Iterator<String> iterator = entityIdsToAdd.iterator();
        while (iterator.hasNext()) {
            String entityId = iterator.next();
            boolean added = false;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Entity cur = ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(),
                        UUID.fromString(entityId));
                if (cur != null) {
                    LOGGER.info("tryAddAllNewEntities entityId={}", entityId);
                    getOrCreateQueueData(entityId, cur);
                    added = true;
                    break;
                }
            }
            if (added) {
                iterator.remove();
            }
        }
    }

    public static void updateUUID(String oldId, String newId, Entity newEntity) {
        EventQueueData data = queueData.remove(oldId);

        if (data == null) {
            LOGGER.info("Unable to update chat data, UUID not found: " + oldId);
            return;
        }
        data.updateUUID(newId, newEntity);
        queueData.put(newId, data);
    }

    public static void addUserMessage(Entity entity, String userLanguage, ServerPlayer player,
            String userMessage, boolean is_auto_message) {
        LOGGER.info("Add user message entityID={}, playerID={}, message={} ", entity.getStringUUID(), player.getUUID(),
                userMessage);
        EventQueueData q = getOrCreateQueueData(entity.getStringUUID(), entity);
        q.addUserMessage(entity, userLanguage, player, userMessage, is_auto_message);
    }

    public static void addUserMessageToAllClose(String userLanguage, ServerPlayer player, String userMessage,
            boolean is_auto_message) {
        addingEntityQueues = true; // if dont have this, then will first create queue data and poll before
        ServerEntityFinder.getCloseEntities((ServerLevel) player.level(), player, 6).stream().filter(
                (e) -> !(e instanceof Player)).forEach((e) -> {
                    LOGGER.info("Sending user msg={} to ent_id={}", userMessage, e.getStringUUID());
                    // adding user message.
                    getOrCreateQueueData(e.getStringUUID(), e);
                    addUserMessage(e, userLanguage, player, userMessage, is_auto_message);
                });
        addingEntityQueues = false;
    }

    public static void unauthError(ServerPlayer player) {
        unauthPlayers.add(player);
    }

    public static void fixedAuthError(ServerPlayer player) {
        unauthPlayers.remove(player);
    }

}