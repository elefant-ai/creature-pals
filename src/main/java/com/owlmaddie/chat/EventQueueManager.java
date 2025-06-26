package com.owlmaddie.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.block.entity.VaultBlockEntity.Client;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventQueueManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    private static class LLMCompleter {
        private boolean isProcessing = false;

        public boolean isAvailable() {
            return isProcessing;
        }

        public void process(UUID entityId, Consumer<String> onUncleanResponse, Consumer<String> onError) {
            isProcessing = true;
            queueData.get(entityId).process((resp) -> {
                onUncleanResponse.accept(resp);
                isProcessing = false;
            }, onError);
        }
    }

    private static List<LLMCompleter> completers = List.of(new LLMCompleter()); // TODO: add another completer, more if premium

    private static ConcurrentHashMap<UUID, EventQueueData> queueData = new ConcurrentHashMap<>();
    // entitys to add next tick. EventQueueData only has entityId, so need to loop
    // through all players to find entity object
    private static Set<UUID> entityIdsToAdd = new HashSet<>();

    public static void addEntityIdToCreate(UUID entityId) {
        entityIdsToAdd.add(entityId);
    }

    private static Optional<UUID> getEntityIdToProcess(MinecraftServer server) {
        return queueData.values().stream().filter((queueData) -> queueData.shouldProcess()).findFirst()
                .map((q) -> q.getId());
    }
    
    private static void errorCooldown(UUID entityId){
        queueData.get(entityId).errorCooldown();
    }

    public static void injectOnServerTick(MinecraftServer server) {
        // first make sure queueData is up to date (as much as possible, 
        // because maybe no players have tracked entity)
        tryAddAllNewEntities(server);
        completers.forEach((completer) -> {
            if (!completer.isAvailable()) {
                return;
            }
            // find entityId and player somehow
            Optional<UUID> entityIdOption = getEntityIdToProcess(server);
            entityIdOption.ifPresent(
                    (entityId) -> {
                        ClientSideEffects.setPending(entityId);
                        completer.process(entityId, (uncleanMsg) -> {
                            ClientSideEffects.onEntityGeneratedMessage(entityId, uncleanMsg);
                        }, (errMsg) -> {
                            ClientSideEffects.onLLMGenerateError(entityId, errMsg);
                            // make entity on cooldown
                            errorCooldown(entityId);
                        });
                    });
        });
    }

    public static EventQueueData getOrCreateQueueData(UUID entityId, Entity entity) {
        return queueData.computeIfAbsent(entityId, k -> {
            LOGGER.info(String.format("EventQueueManager/creating new queue data for ent id (%s)", entityId));
            return new EventQueueData(entityId, entity);
        });
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
}
