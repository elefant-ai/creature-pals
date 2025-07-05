package com.owlmaddie.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.chat.ChatDataManager.ChatSender;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;
import com.owlmaddie.network.C2S.*;
import com.owlmaddie.network.S2C.*;
import com.owlmaddie.player2.TTS;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.PlayerMessageManager;
import com.owlmaddie.utils.ClientEntityFinder;
import com.owlmaddie.utils.Decompression;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code ClientPackets} class provides methods to send packets to/from the server for generating greetings,
 * updating message details, and sending user messages.
 */
public class ClientPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    static HashMap<Integer, byte[]> receivedChunks = new HashMap<>();

    public static void sendGenerateGreeting(Entity entity) {
        // Get user language
        String userLanguageCode = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        String userLanguageName = MinecraftClient.getInstance().getLanguageManager().getLanguage(userLanguageCode).getDisplayText().getString();

        // Send C2S packet
        ClientPlayNetworking.send(new GreetingPayload(entity.getUuid(), userLanguageName));
    }

    public static void sendUpdateLineNumber(Entity entity, Integer lineNumber) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(entity.getUuidAsString());
        buf.writeInt(lineNumber);

        // Send C2S packet
        ClientPlayNetworking.send(new ReadNextPayload(entity.getUuid(), lineNumber));
    }

    public static void sendOpenChat(Entity entity) {
        // Send C2S packet
        ClientPlayNetworking.send(new OpenChatPayload(entity.getUuid()));
    }

    public static void sendCloseChat() {
        // Send C2S packet
        ClientPlayNetworking.send(new CloseChatPayload(false));
    }

    public static void setChatStatus(Entity entity, ChatDataManager.ChatStatus newStatus) {
        // Send C2S packet
        ClientPlayNetworking.send(new SetStatusPayload(entity.getUuid(), newStatus.toString()));
    }

    public static void sendChat(Entity entity, String message) {
        // AAA use this to actually send a chat msg to an entity.
        // Get user language
        String userLanguageCode = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        String userLanguageName = MinecraftClient.getInstance().getLanguageManager().getLanguage(userLanguageCode).getDisplayText().getString();

        // Send C2S packet
        ClientPlayNetworking.send(new SendChatPayload(entity.getUuid(), message, userLanguageName));
    }

    // Reading a Map<String, PlayerData> from the buffer
    public static Map<UUID, PlayerData> readPlayerDataMap(byte[] buffer) {
        PacketByteBuf pBuffer = new PacketByteBuf(Unpooled.copiedBuffer(buffer));

        int size = pBuffer.readInt(); // Read the size of the map
        Map<UUID, PlayerData> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            UUID key = pBuffer.readUuid(); // Read the key (playerName)
            PlayerData data = new PlayerData();
            data.friendship = pBuffer.readInt(); // Read PlayerData field(s)
            map.put(key, data); // Add to the map
        }
        return map;
    }

    public static void register() {
        // Client-side packet handler, message sync

        ClientPlayNetworking.registerGlobalReceiver(EntityMessagePayload.ID, (payload, context) -> {
            // Read the data from the server packet
            UUID entityId = payload.entityID();
            String message = payload.currentMessage();
            int line = payload.currentLineNumber();
            String status_name = payload.status();
            ChatDataManager.ChatStatus status = ChatDataManager.ChatStatus.valueOf(status_name);
            String sender_name = payload.sender();
            ChatDataManager.ChatSender sender = ChatDataManager.ChatSender.valueOf(sender_name);
            Map<UUID, PlayerData> players = readPlayerDataMap(payload.playerMap());

            MinecraftClient client = context.client();
            // Update the chat data manager on the client-side
            client.execute(() -> { // Make sure to run on the client thread
                // Ensure client.player is initialized
                if (client.player == null || client.world == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for entity '{}'.", entityId);
                    return;
                }

                // Get entity chat data for current entity & player
                ChatDataManager chatDataManager = ChatDataManager.getClientInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(entityId);

                // Add entity message
                if (!message.isEmpty()) {
                    chatData.currentMessage = message;
                }
                chatData.currentLineNumber = line;
                chatData.status = ChatDataManager.ChatStatus.valueOf(payload.status());
                chatData.sender = ChatDataManager.ChatSender.valueOf(payload.sender());
                chatData.players = players == null? chatData.players : players;

                // Play sound with volume based on distance (from player or entity)
                MobEntity entity = ClientEntityFinder.getEntityByUUID(client.world, entityId);
                if (entity != null) {
                    playNearbyUISound(client, entity, 0.2f);
                }
                if(status == ChatStatus.DISPLAY && chatData.sender == ChatSender.ASSISTANT){
                    if(message.contains("Error:")){
                        // for now skip error
                        return;
                    }
                    TTS.speak(message, entityId);
                }
            });
        });

        // Client-side packet handler, message sync
        ClientPlayNetworking.registerGlobalReceiver(PlayerMessagePayload.ID, (payload, context) -> {
            // Read the data from the server packet
            UUID senderPlayerId = payload.senderId();
            String senderPlayerName = payload.senderName();
            String message = payload.message();
            boolean fromMinecraftChat = payload.fromMinecraftChat();

            MinecraftClient client = context.client();
            // Update the chat data manager on the client-side
            client.execute(() -> { // Make sure to run on the client thread
                // Ensure client.player is initialized
                if (client.player == null || client.world == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for sender '{}'.", senderPlayerId);
                    return;
                }
                // AAA trigger for player message
                LOGGER.info("Player message" + message);

                // Add player message to queue for rendering
                PlayerMessageManager.addMessage(senderPlayerId, message, senderPlayerName, ChatDataManager.TICKS_TO_DISPLAY_USER_MESSAGE);

                // if the msg was from minecraft's chat, and this is the client for that player, then send to nearest entity with bubble open.
                // if(fromMinecraftChat && senderPlayerName.equals(client.player.getName().getString())){
                //     Optional<Entity> entityToSendChatTo = ClientEntityFinder.getClosestEntityToPlayerWithChatBubbleOpen();
                //     entityToSendChatTo.ifPresent(entity -> {
                //         ClientPackets.sendChat(entity, message);
                //     });
                // }
            });
        });

        // Client-side player login: get all chat data
        ClientPlayNetworking.registerGlobalReceiver(LoginPayload.ID, (payload, context) -> {
            int sequenceNumber = payload.sequence(); // Sequence number of the current packet
            int totalPackets = payload.totalPackets(); // Total number of packets for this data
            byte[] chunk = payload.chunk(); // Read the byte array chunk from the current packet
            MinecraftClient client = context.client();
            client.execute(() -> { // Make sure to run on the client thread
                // Store the received chunk
                receivedChunks.put(sequenceNumber, chunk);

                // Check if all chunks have been received
                if (receivedChunks.size() == totalPackets) {
                    LOGGER.info("Reassemble chunks on client and decompress lite JSON data string");

                    // Combine all byte array chunks
                    ByteArrayOutputStream combined = new ByteArrayOutputStream();
                    for (int i = 0; i < totalPackets; i++) {
                        combined.write(receivedChunks.get(i), 0, receivedChunks.get(i).length);
                    }

                    // Decompress the combined byte array to get the original JSON string
                    String chatDataJSON = Decompression.decompressString(combined.toByteArray());
                    if (chatDataJSON == null || chatDataJSON.isEmpty()) {
                        LOGGER.warn("Received invalid or empty chat data JSON. Skipping processing.");
                        return;
                    }

                    // Parse JSON and update client chat data
                    Gson GSON = new Gson();
                    Type type = new TypeToken<ConcurrentHashMap<String, EntityChatData>>(){}.getType();
                    ChatDataManager.getClientInstance().entityChatDataMap = GSON.fromJson(chatDataJSON, type);

                    // Clear receivedChunks for future use
                    receivedChunks.clear();
                }
            });
        });

        // Client-side packet handler, receive entire whitelist / blacklist, and update BubbleRenderer
        ClientPlayNetworking.registerGlobalReceiver(WhitelistPayload.ID, (payload, context) -> {
            // Read the whitelist data from the buffer
            List<String> whitelist = payload.whitlelist();
            List<String> blacklist = payload.blacklist();


            context.client().execute(() -> {
                BubbleRenderer.whitelist = whitelist;
                BubbleRenderer.blacklist = blacklist;
            });
        });

        // Client-side packet handler, player status sync
        ClientPlayNetworking.registerGlobalReceiver(PlayerStatusPayload.ID, (payload, context) -> {
            // Read the data from the server packet
            UUID playerId = payload.senderId();
            boolean isChatOpen = payload.isChatOpen();

            // Get player instance
            PlayerEntity player = ClientEntityFinder.getPlayerEntityFromUUID(playerId);

            MinecraftClient client = context.client();
            // Update the player status data manager on the client-side
            client.execute(() -> {
                if (player == null) {
                    LOGGER.warn("Player entity is null. Skipping status update.");
                    return;
                }

                if (isChatOpen) {
                    PlayerMessageManager.openChatUI(playerId);
                    playNearbyUISound(client, player, 0.2f);
                } else {
                    PlayerMessageManager.closeChatUI(playerId);
                }
            });
        });
    }

    private static void playNearbyUISound(MinecraftClient client, Entity player, float maxVolume) {
        // Play sound with volume based on distance
        int distance_squared = 144;
        if (client.player != null) {
            double distance = client.player.squaredDistanceTo(player.getX(), player.getY(), player.getZ());
            if (distance <= distance_squared) {
                // Decrease volume based on distance
                float volume = maxVolume - (float)distance / distance_squared * maxVolume;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), volume, 0.8F);
            }
        }
    }
}

