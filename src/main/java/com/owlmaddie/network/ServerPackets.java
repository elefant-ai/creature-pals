// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later

package com.owlmaddie.network;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.ChatDataSaverScheduler;
import com.owlmaddie.chat.ChatGPTRequest;
import com.owlmaddie.chat.ClientSideEffects;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.EntityChatDataLight;
import com.owlmaddie.chat.EventQueueManager;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.inventory.ChatInventory;
import com.owlmaddie.inventory.InventoryLootTables;
import com.owlmaddie.inventory.LootTableHelper;
import com.owlmaddie.particle.Particles;
import com.owlmaddie.utils.Compression;
import com.owlmaddie.utils.ServerEntityFinder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.owlmaddie.ModInit.LOGGER;

/**
 * The {@code ServerPackets} class provides methods to send packets to/from the
 * client for generating greetings,
 * updating message details, and sending user messages.
 */
public class ServerPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    public static MinecraftServer serverInstance;
    public static ChatDataSaverScheduler scheduler = null;
    public static final ResourceLocation PACKET_C2S_GREETING = new ResourceLocation("creaturepals",
            "packet_c2s_greeting");
    public static final ResourceLocation PACKET_C2S_READ_NEXT = new ResourceLocation("creaturepals",
            "packet_c2s_read_next");
    public static final ResourceLocation PACKET_C2S_SET_STATUS = new ResourceLocation("creaturepals",
            "packet_c2s_set_status");
    public static final ResourceLocation PACKET_C2S_OPEN_CHAT = new ResourceLocation("creaturepals",
            "packet_c2s_open_chat");
    public static final ResourceLocation PACKET_C2S_CLOSE_CHAT = new ResourceLocation("creaturepals",
            "packet_c2s_close_chat");
    public static final ResourceLocation PACKET_C2S_SEND_CHAT = new ResourceLocation("creaturepals",
            "packet_c2s_send_chat");
    public static final ResourceLocation PACKET_C2S_AUTH_RESPONSE = new ResourceLocation("creaturepals",
            "packet_c2s_auth_response");
    public static final ResourceLocation PACKET_C2S_AUTH_FIXED_ERROR = new ResourceLocation("creaturepals",
            "packet_c2s_auth_fixed_error");
    public static final ResourceLocation PACKET_S2C_AUTH_REQUEST = new ResourceLocation("creaturepals",
            "packet_s2c_auth_request");
    public static final ResourceLocation PACKET_S2C_ENTITY_MESSAGE = new ResourceLocation("creaturepals",
            "packet_s2c_entity_message");
    public static final ResourceLocation PACKET_S2C_PLAYER_MESSAGE = new ResourceLocation("creaturepals",
            "packet_s2c_player_message");
    public static final ResourceLocation PACKET_S2C_LOGIN = new ResourceLocation("creaturepals", "packet_s2c_login");
    public static final ResourceLocation PACKET_S2C_WHITELIST = new ResourceLocation("creaturepals",
            "packet_s2c_whitelist");
    public static final ResourceLocation PACKET_S2C_PLAYER_STATUS = new ResourceLocation("creaturepals",
            "packet_s2c_player_status");
    public static final ResourceLocation PACKET_S2C_UNAUTH_ERR = new ResourceLocation("creaturepals",
            "packet_s2c_unauth_err");
    public static final ResourceLocation PACKET_S2C_SET_TTS = new ResourceLocation("creaturepals",
            "packet_s2c_set_tts");

    public static final ParticleType<?> HEART_SMALL_PARTICLE = Particles.HEART_SMALL_PARTICLE;
    public static final ParticleType<?> HEART_BIG_PARTICLE = Particles.HEART_BIG_PARTICLE;
    public static final ParticleType<?> FIRE_SMALL_PARTICLE = Particles.FIRE_SMALL_PARTICLE;
    public static final ParticleType<?> FIRE_BIG_PARTICLE = Particles.FIRE_BIG_PARTICLE;
    public static final ParticleType<?> ATTACK_PARTICLE = Particles.ATTACK_PARTICLE;
    public static final ParticleType<?> FLEE_PARTICLE = Particles.FLEE_PARTICLE;
    public static final ParticleType<?> FOLLOW_FRIEND_PARTICLE = Particles.FOLLOW_FRIEND_PARTICLE;
    public static final ParticleType<?> FOLLOW_ENEMY_PARTICLE = Particles.FOLLOW_ENEMY_PARTICLE;
    public static final ParticleType<?> PROTECT_PARTICLE = Particles.PROTECT_PARTICLE;
    public static final ParticleType<?> LEAD_FRIEND_PARTICLE = Particles.LEAD_FRIEND_PARTICLE;
    public static final ParticleType<?> LEAD_ENEMY_PARTICLE = Particles.LEAD_ENEMY_PARTICLE;
    public static final ParticleType<?> LEAD_PARTICLE = Particles.LEAD_PARTICLE;

    private static final Map<UUID, UUID> pendingAuthRequests = new ConcurrentHashMap<>();

    public static void register() {
        // Register custom particles
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "heart_small"),
                HEART_SMALL_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "heart_big"),
                HEART_BIG_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "fire_small"),
                FIRE_SMALL_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "fire_big"),
                FIRE_BIG_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "attack"),
                ATTACK_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "flee"), FLEE_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "follow_enemy"),
                FOLLOW_ENEMY_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "follow_friend"),
                FOLLOW_FRIEND_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "protect"),
                PROTECT_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "lead_enemy"),
                LEAD_ENEMY_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "lead_friend"),
                LEAD_FRIEND_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturepals", "lead"), LEAD_PARTICLE);

        // Handle packet for Greeting
        PacketHelper.registerReceiver(PACKET_C2S_GREETING, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String userLanguage = buf.readUtf(32767);

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance()
                            .getOrCreateChatData(entity.getStringUUID());
                    if (chatData.characterSheet.isEmpty()) {
                        LOGGER.info("C2S_GREETING");
                        EventQueueManager.addGreeting(entity, chatData, userLanguage, player, false);
                    }
                }
            });
        });

        // Handle packet for reading lines of message
        PacketHelper.registerReceiver(PACKET_C2S_READ_NEXT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            int lineNumber = buf.readInt();

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    LOGGER.info("ServerPackets/read_Next entityID={} lineNumber={} playerID={}", entityId, lineNumber,
                            player.getUUID());
                    EntityChatData chatData = ChatDataManager.getServerInstance()
                            .getOrCreateChatData(entity.getStringUUID());
                    LOGGER.info("Update read lines to " + lineNumber + " for: " + entity.getType().toString());
                    ClientSideEffects.setLineNumberUsingParamsFromChatData(entity.getStringUUID(), lineNumber);
                }
            });
        });

        // Handle packet for setting status of chat bubbles
        PacketHelper.registerReceiver(PACKET_C2S_SET_STATUS, (server, player, buf) -> {

            UUID entityId = UUID.fromString(buf.readUtf());
            String status_name = buf.readUtf(32767);

            LOGGER.info("ServerPackets/setStatus entityID={} status={} playerID={}", entityId, status_name,
                    player.getUUID());
            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);
                    ClientSideEffects.setStatusUsingParamsFromChatData(entity.getStringUUID(),
                            ChatDataManager.ChatStatus.valueOf(status_name));
                }
            });
        });

        // Handle packet for Open Chat
        PacketHelper.registerReceiver(PACKET_C2S_OPEN_CHAT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 7F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);
                }

                // Sync player UI status to all clients
                BroadcastPlayerStatus(player, true);
            });
        });
        PacketHelper.registerReceiver(PACKET_C2S_AUTH_RESPONSE, (server, player, buf) -> {
            UUID requestId = UUID.fromString(buf.readUtf());
            String apiKey = buf.readUtf();
            server.execute(() -> {
                if (ChatGPTRequest.apiKeyAwaiter.get(requestId) != null) {
                    ChatGPTRequest.apiKeyAwaiter.remove(requestId).complete(apiKey);
                }
            });
        });

        // Handle packet for Close Chat
        PacketHelper.registerReceiver(PACKET_C2S_CLOSE_CHAT, (server, player, buf) -> {
            server.execute(() -> {
                // Sync player UI status to all clients
                BroadcastPlayerStatus(player, false);
            });
        });

        // Handle packet for new chat message
        PacketHelper.registerReceiver(PACKET_C2S_SEND_CHAT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String message = buf.readUtf(32767);
            String userLanguage = buf.readUtf(32767);
            Entity ent = ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), entityId);
            String RHS = ent != null && ent.getCustomName() != null && !ent.getCustomName().equals("N/A")
                    ? "> (to " + ent.getCustomName().getString() + ") "
                    : "> ";
            LOGGER.info("ServerPackets/sendChat entityID={} message={} playerID={}", entityId, message,
                    player.getUUID());
            BroadcastMessage(Component.literal("<" + player.getName().getString() + RHS + message));

            // Ensure that the task is synced with the server thread
            server.execute(() -> {
                Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), entityId);
                if (entity != null) {
                    ChatDataManager.getServerInstance()
                            .getOrCreateChatData(entity.getStringUUID());
                    EventQueueManager.addUserMessage(entity, userLanguage, player, message, false);
                    ClientSideEffects.setPending(entity.getStringUUID());
                }
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_AUTH_FIXED_ERROR, (server, player, buf) -> {
            LOGGER.info("Server: recieved packet that auth error was fixed.");
            EventQueueManager.fixedAuthError(player);
        });

        // Send lite chat data JSON to new player (to populate client data)
        // Data is sent in chunks, to prevent exceeding the 32767 limit per String.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;

            // Send entire whitelist / blacklist to logged in player
            send_whitelist_blacklist(player);

            LOGGER.info(
                    "Server send compressed, chunked login message packets to player: " + player.getDisplayName().getString());
            // Get lite JSON data & compress to byte array
            String chatDataJSON = ChatDataManager.getServerInstance()
                    .GetLightChatData(player.getDisplayName().getString());
            byte[] compressedData = Compression.compressString(chatDataJSON);
            if (compressedData == null) {
                LOGGER.error("Failed to compress chat data.");
                return;
            }

            final int chunkSize = 32000; // Define chunk size
            int totalPackets = (int) Math.ceil((double) compressedData.length / chunkSize);

            // Loop through each chunk of bytes, and send bytes to player
            for (int i = 0; i < totalPackets; i++) {
                int start = i * chunkSize;
                int end = Math.min(compressedData.length, start + chunkSize);

                FriendlyByteBuf buffer = BufferHelper.create();
                buffer.writeInt(i); // Packet sequence number
                buffer.writeInt(totalPackets); // Total number of packets

                // Write chunk as byte array
                byte[] chunk = Arrays.copyOfRange(compressedData, start, end);
                buffer.writeByteArray(chunk);

                PacketHelper.send(player, PACKET_S2C_LOGIN, buffer);
            }
            // If no server API key is configured, request from this player
            ConfigurationHandler.Config config = new ConfigurationHandler(server).loadConfig();
            String serverApiKey = config.getApiKey();
            if (serverApiKey == null || serverApiKey.isEmpty()) {
                requestPlayerApiKey(player);
            }
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            String world_name = world.dimension().location().getPath();
            if (world_name.equals("overworld")) {
                serverInstance = server;
                ChatDataManager.getServerInstance().loadChatData(server);

                // Start the auto-save task to save every X minutes
                scheduler = new ChatDataSaverScheduler();
                scheduler.startAutoSaveTask(server, 15, TimeUnit.MINUTES);
            }
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            String world_name = world.dimension().location().getPath();
            if (world_name.equals("overworld")) {
                ChatDataManager manager = ChatDataManager.getServerInstance();
                manager.saveChatData(server);
                manager.clearData();
                serverInstance = null;

                // Shutdown auto scheduler
                if (scheduler != null) {
                    scheduler.stopAutoSaveTask();
                    scheduler = null;
                }
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            String entityUUID = entity.getStringUUID();
            if (entity.getRemovalReason() == Entity.RemovalReason.KILLED
                    && ChatDataManager.getServerInstance().entityChatDataMap.containsKey(entityUUID)) {
                LOGGER.debug("Entity killed (" + entityUUID + "), updating death time stamp.");
                ChatDataManager.getServerInstance().entityChatDataMap.get(entityUUID).death = System
                        .currentTimeMillis();
            }
            // TODO: Maybe add system to remove from queue
        });

    }

    public static void send_whitelist_blacklist(ServerPlayer player) {
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        FriendlyByteBuf buffer = BufferHelper.create();

        // Write the whitelist data to the buffer
        List<String> whitelist = config.getWhitelist();
        buffer.writeInt(whitelist.size());
        for (String entry : whitelist) {
            buffer.writeUtf(entry);
        }

        // Write the blacklist data to the buffer
        List<String> blacklist = config.getBlacklist();
        buffer.writeInt(blacklist.size());
        for (String entry : blacklist) {
            buffer.writeUtf(entry);
        }

        if (player != null) {
            // Send packet to specific player
            LOGGER.info("Sending whitelist / blacklist packet to player: " + player.getDisplayName().getString());
            PacketHelper.send(player, PACKET_S2C_WHITELIST, buffer);
        } else {
            // Iterate over all players and send the packet
            for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
                PacketHelper.send(serverPlayer, PACKET_S2C_WHITELIST, buffer);
            }
        }
    }

    // Writing a Map<String, PlayerData> to the buffer
    public static void writePlayerDataMap(FriendlyByteBuf buffer, Map<String, PlayerData> map) {
        buffer.writeInt(map.size()); // Write the size of the map
        for (Map.Entry<String, PlayerData> entry : map.entrySet()) {
            buffer.writeUtf(entry.getKey()); // Write the key (playerName)
            PlayerData data = entry.getValue();
            buffer.writeInt(data.friendship); // Write PlayerData field(s)
        }
    }

    // Send new message to all connected players
    public static void BroadcastEntityMessage(EntityChatDataLight chatData) {
        // Log useful information before looping through all players
        LOGGER.info(
                "Broadcasting entity message: entityId={}, status={}, currentMessage={}, currentLineNumber={}, senderType={}",
                chatData.entityId, chatData.status,
                chatData.currentMessage.length() > 24 ? chatData.currentMessage.substring(0, 24) + "..."
                        : chatData.currentMessage,
                chatData.currentLineNumber, chatData.sender);

        // for (ServerLevel world : serverInstance.getAllLevels()) {
        // Find Entity by UUID and update custom name

        // Iterate over all players and send the packet
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            FriendlyByteBuf buffer = BufferHelper.create();
            buffer.writeUtf(chatData.entityId);
            buffer.writeUtf(chatData.currentMessage);
            buffer.writeInt(chatData.currentLineNumber);
            buffer.writeUtf(chatData.status.toString());
            buffer.writeUtf(chatData.sender.toString());
            writePlayerDataMap(buffer, chatData.players);

            // Send message to player
            PacketHelper.send(player, PACKET_S2C_ENTITY_MESSAGE, buffer);
        }
        // break;
        // }
        }


    // Send new message to all connected players
    public static void BroadcastPlayerMessage(EntityChatData chatData, ServerPlayer sender) {
        // Log the specific data being sent
        LOGGER.info("Broadcasting player message: senderUUID={}, message={}", sender.getStringUUID(),
                chatData.currentMessage);

        // Create the buffer for the packet
        FriendlyByteBuf buffer = BufferHelper.create();

        // Write the sender's UUID and the chat message to the buffer
        buffer.writeUtf(sender.getStringUUID());
        buffer.writeUtf(sender.getDisplayName().getString());
        buffer.writeUtf(chatData.currentMessage);

        // Iterate over all connected players and send the packet
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            PacketHelper.send(serverPlayer, PACKET_S2C_PLAYER_MESSAGE, buffer);
        }
    }

    // Send new message to all connected players
    public static void BroadcastPlayerStatus(Player player, boolean isChatOpen) {
        FriendlyByteBuf buffer = BufferHelper.create();

        // Write the entity's chat updated data
        buffer.writeUtf(player.getStringUUID());
        buffer.writeBoolean(isChatOpen);

        // Iterate over all players and send the packet
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            LOGGER.debug("Server broadcast " + player.getDisplayName().getString() + " player status to client: "
                    + serverPlayer.getDisplayName().getString() + " | isChatOpen: " + isChatOpen);
            PacketHelper.send(serverPlayer, PACKET_S2C_PLAYER_STATUS, buffer);
        }
    }

    public static void SendTTSStatus(ServerPlayer player, boolean tts) {
        FriendlyByteBuf buf = BufferHelper.create();
        buf.writeBoolean(tts);

        PacketHelper.send(player, PACKET_S2C_SET_TTS, buf);
    }

    public static void BroadcastUnauthErr(ServerPlayer player) {
        LOGGER.info("Server: Sending unauth err packet");
        FriendlyByteBuf buffer = BufferHelper.create();
        PacketHelper.send(player, PACKET_S2C_UNAUTH_ERR, buffer);
    }

    // Send a chat message to all players (i.e. death message)
    public static void BroadcastMessage(Component message) {
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            serverPlayer.displayClientMessage(message, false);
        }
    }

    // Send a chat message to a player which is clickable (for error messages with a
    // link for help)
    public static void SendClickableError(Player player, String message, String url) {
        MutableComponent text = Component.literal(message)
                .withStyle(ChatFormatting.BLUE)
                .withStyle(style -> style
                        .withClickEvent(ClickEventHelper.openUrl(url))
                        .withUnderlined(true));
        player.displayClientMessage(text, false);
    }

    // Send a clickable message to ALL Ops
    public static void sendErrorToAllOps(MinecraftServer server, String message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Check if the player is an operator
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                ServerPackets.SendClickableError(player, message, "https://player2.game/discord");
            }
        }
    }

    public static UUID requestPlayerApiKey(ServerPlayer player) {
        UUID requestId = UUID.randomUUID();
        pendingAuthRequests.put(requestId, player.getUUID());
        return requestPlayerApiKeyWithId(Optional.of(player), requestId);
    }

    public static UUID requestPlayerApiKeyWithId(Optional<ServerPlayer> player, UUID requestId) {
        FriendlyByteBuf buffer = BufferHelper.create();
        buffer.writeUtf(requestId.toString());

        if (player.isPresent()) {
            ServerPlayer realPlayer = player.get();
            PacketHelper.send(realPlayer, PACKET_S2C_AUTH_REQUEST, buffer);
            LOGGER.info("Sent API key request to '{}' with requestId={}", realPlayer.getGameProfile().getName(), requestId);

        }
        return requestId;
    }

}
