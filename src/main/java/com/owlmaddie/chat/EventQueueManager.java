package com.owlmaddie.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventQueueManager {
    public static ConcurrentHashMap<String, EventQueueData> queueData = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static final long maxDistance = 12;
    public static boolean llmProcessing = false;
    public static boolean addingEntityQueues = false;
    public static String blacklistedEntityId = null;

    private static long lastErrorTime = 0L;
    private static long waitTimeAfterError = 10_000_000_000L; // wait 3 sec after err before doing any polling

    public static long entityToEntityCutoffDistance = 12;
    public static long playerToEntityCutoffDistance = 12;
    private static Set<String> entityIdsToAdd = new HashSet<>();

    public static void onError() {
        lastErrorTime = System.nanoTime();
    }

    public static boolean shouldWaitBecauseOfError() {
        return System.nanoTime() <= lastErrorTime + waitTimeAfterError;
    }

    // TODO : look for all stuff like this:
    // public static void updateUUID(String oldUUID, String newUUID) {
    // EventQueueData data = queueData.remove(oldUUID);
    // if (data == null) {
    // LOGGER.info("Unable to update chat data, UUID not found: " + oldUUID);
    // return;
    // }
    // data.updateUUID(newUUID);
    // queueData.put(newUUID, data);

    // }

    public static void addEntityIdToCreate(String entityId) {
        entityIdsToAdd.add(entityId);
    }

    public static EventQueueData getOrCreateQueueData(String entityId, Entity entity) {
        return queueData.computeIfAbsent(entityId, k -> {
            LOGGER.info(String.format("EventQueueManager/creating new queue data for ent id (%s)", entityId));
            return new EventQueueData(entityId, entity);
        });
    }

    public static void addUserMessage(Entity entity, String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message, boolean shouldImmediatlyPoll) {
        EventQueueData q = getOrCreateQueueData(entity.getUuidAsString(), entity);
        q.addUserMessage(userLanguage, player, userMessage, is_auto_message);
        if (shouldImmediatlyPoll) {
            q.bubblePoll();
        }
    }

    public static void addGreeting(Entity entity, String userLangauge, ServerPlayerEntity player) {
        LOGGER.info("EventQueueManager: callign addGreeting");
        EventQueueData q = getOrCreateQueueData(entity.getUuidAsString(), entity);
        q.addGreeting(userLangauge, player);
        q.immediateGreeting();
    }

    public static void addUserMessageToAllClose(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        // for (EventQueueData curQueue : queueData.values()) {
        // Entity entity = curQueue.entity;
        // if (entity.distanceTo(player) > playerToEntityCutoffDistance) {
        // continue;
        // }
        // addUserMessage(curQueue.entity, userLanguage, player, userMessage,
        // is_auto_message);
        // }
        addingEntityQueues = true; // if dont have this, then will first create queue data and poll before
        ServerEntityFinder.getCloseEntities(player.getServerWorld(), player, 6).stream().filter(
                (e) -> !e.isPlayer()).forEach((e) -> {
                    // adding user message.
                    getOrCreateQueueData(e.getUuidAsString(), e);
                    addUserMessage(e, userLanguage, player, userMessage, is_auto_message, false);
                });
        addingEntityQueues = false;
    }

    public static void addEntityMessageToAllClose(Entity fromEntity, String userLanguage, ServerPlayerEntity player,
            String entityMessage,
            String entityCustomName, String entityTypeName) {
        for (EventQueueData curQueue : queueData.values()) {
            if (curQueue.entityId.equals(fromEntity.getUuidAsString())) {
                continue;
            }
            Entity entity = curQueue.entity;
            if (entity.distanceTo(fromEntity) > entityToEntityCutoffDistance) {
                continue;
            }
            curQueue.addExternalEntityMessage(userLanguage, player, entityMessage, entityCustomName, entityTypeName);
        }
    }

    public static void injectOnServerTick(MinecraftServer server) {
        Iterator<String> iterator = entityIdsToAdd.iterator();
        while (iterator.hasNext()) {
            String entityId = iterator.next();
            boolean added = false;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Entity cur = ServerEntityFinder.getEntityByUUID(player.getServerWorld(), UUID.fromString(entityId));
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
        if (llmProcessing || addingEntityQueues)
            return;

        // TODO: Figure out a smarter way to figure out which to poll?
        for (EventQueueData curQueue : queueData.values()) {
            // remove entity if despawn/died so dont poll and err:
            if (curQueue.shouldDelete()) { // if entity died, etc.
                queueData.remove(curQueue.entityId);
                continue;
            }
            if (curQueue.shouldPoll()) {
                llmProcessing = true;
                curQueue.poll();
                // only process one at a time:
                break;
            }
        }
    }
}