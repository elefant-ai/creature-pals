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
import java.util.function.Consumer;

import org.apache.commons.lang3.function.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class EventQueueManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static boolean addingEntityQueues = false;

    private static class LLMCompleter {
        private boolean isProcessing = false;

        public boolean isAvailable() {
            return !isProcessing;
        }

        public void process(UUID entityId, BiConsumer<String, ServerPlayerEntity> onUncleanResponse,
                BiConsumer<String, ServerPlayerEntity> onError,
                TriConsumer<String, Boolean, ServerPlayerEntity> onCharacterSheetAndShouldGreet) {

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

    private static ConcurrentHashMap<UUID, EventQueueData> queueData = new ConcurrentHashMap<>();

    // entitys to add next tick. EventQueueData only has entityId, so need to loop
    // through all players to find entity object
    private static Set<UUID> entityIdsToAdd = new HashSet<>();

    public static void addEntityIdToCreate(UUID entityId) {
        entityIdsToAdd.add(entityId);
    }

    public static void addGreeting(Entity entity, String userLangauge, ServerPlayerEntity player) {
        if (player == null) {
            throw new RuntimeException("Null player for addGreeting");
        }
        ClientSideEffects.setPending(entity.getUuid());

        getOrCreateQueueData(entity.getUuid(), entity).requestGreeting(userLangauge, player);
    }

    private static Optional<UUID> getEntityIdToProcess(MinecraftServer server) {
        return queueData.values().stream()
                .filter(EventQueueData::shouldProcess)
                .max(Comparator.comparingInt(EventQueueData::getPriority))
                .map(EventQueueData::getId);
    }

    private static void errorCooldown(UUID entityId) {
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
            Optional<UUID> entityIdOption = getEntityIdToProcess(server);
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

    public static EventQueueData getOrCreateQueueData(UUID entityId, Entity entity) {
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
        Iterator<UUID> iterator = entityIdsToAdd.iterator();
        while (iterator.hasNext()) {
            UUID entityId = iterator.next();
            boolean added = false;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Entity cur = ServerEntityFinder.getEntityByUUID(player.getServerWorld(), entityId);
                if (cur != null) {
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

    public static void updateUUID(UUID oldId, UUID newId, Entity newEntity) {
        EventQueueData data = queueData.remove(oldId);

        if (data == null) {
            LOGGER.info("Unable to update chat data, UUID not found: " + oldId);
            return;
        }
        data.updateUUID(newId, newEntity);
        queueData.put(newId, data);
    }

    public static void addUserMessage(Entity entity, String userLanguage, ServerPlayerEntity player,
            String userMessage, boolean is_auto_message) {
        EventQueueData q = getOrCreateQueueData(entity.getUuid(), entity);
        q.addUserMessage(entity, userLanguage, player, userMessage, is_auto_message);
    }

    public static void addUserMessageToAllClose(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        addingEntityQueues = true; // if dont have this, then will first create queue data and poll before
        ServerEntityFinder.getCloseEntities(player.getServerWorld(), player, 6).stream().filter(
                (e) -> !e.isPlayer()).forEach((e) -> {
                    // adding user message.
                    getOrCreateQueueData(e.getUuid(), e);
                    addUserMessage(e, userLanguage, player, userMessage, is_auto_message);
                });
        addingEntityQueues = false;
    }

}
