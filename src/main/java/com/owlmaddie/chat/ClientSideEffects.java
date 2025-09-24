package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.BroadcastEntityMessage;
import static com.owlmaddie.network.ServerPackets.serverInstance;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlmaddie.chat.ChatDataManager.ChatSender;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;
import com.owlmaddie.message.MessageParser;
import com.owlmaddie.message.ParsedMessage;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

// side effects that are broadcast to client or modify chatData
public class ClientSideEffects {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    private static EntityChatData getChatData(UUID entityId) {
        return ChatDataManager.getServerInstance().getOrCreateChatData(entityId.toString());
    }

    public static void onEntityGeneratedMessage(String entityId,
            String uncleanEntityMessageResponse, ServerPlayer player) {
        LOGGER.info("sideEffect/onEntityGeneratedMessage entityId={} uncleanEntityResponse={} player={}", entityId,
                uncleanEntityMessageResponse, player);
        ParsedMessage result = MessageParser.parseMessage(uncleanEntityMessageResponse.replace("\n", " "));
        PlayerData playerData = getChatData(UUID.fromString(entityId)).getPlayerData(player.getStringUUID());
        BehaviorApplier.apply(result.getBehaviors(), player, entityId, playerData);
        String cleanedMessage = result.getCleanedMessage();
        if (cleanedMessage.isEmpty()) {
            getChatData(UUID.fromString(entityId)).addMessage("...", ChatDataManager.ChatSender.ASSISTANT, player);
            BroadcastEntityMessage(new EntityChatDataLight(entityId, "...", 0, ChatStatus.DISPLAY, ChatSender.ASSISTANT,
                    getChatData(UUID.fromString(entityId)).characterSheet,
                    getChatData(UUID.fromString(entityId)).players));
        } else {
            getChatData(UUID.fromString(entityId)).addMessage(uncleanEntityMessageResponse,
                    ChatDataManager.ChatSender.ASSISTANT,
                    player);
            sendChatAsEntity(entityId, cleanedMessage, player, true);
        }
    }

    // note that this includes greeting, only sends if shouldGreet is true
    public static void onCharacterSheetGenerated(String entityId, String characterSheet,
            boolean shouldGreet, ServerPlayer player) {
        LOGGER.info("sideEffect/onCharacterSheetGen entityid={} characterSheet={} shouldGreet={} player={}", entityId,
                characterSheet, shouldGreet, player);
        getChatData(UUID.fromString(entityId)).characterSheet = characterSheet;
        String characterName = Optional.ofNullable(getChatData(UUID.fromString(entityId)).getCharacterProp("name"))
                .filter(s -> !s.isEmpty())
                .orElse("N/A");
        if (characterName.equals("N/A")) {
            throw new RuntimeException(
                    "Generated \"\" or \"N/A\" as a character name");
        }
        String shortGreeting = Optional
                .ofNullable(getChatData(UUID.fromString(entityId)).getCharacterProp("short greeting"))
                .filter(s -> !s.isEmpty())
                .orElse(Randomizer.getRandomMessage(Randomizer.RandomType.NO_RESPONSE))
                .replace("\n", " ");
        setNameOfEntity(entityId, characterName);
        if (shouldGreet) {
            getChatData(UUID.fromString(entityId)).addMessage(shortGreeting, ChatDataManager.ChatSender.ASSISTANT,
                    player);
            sendChatAsEntity(entityId, shortGreeting, player, true);
        }
    }

    private static void setNameOfEntity(String entityId, String characterName) {
        LOGGER.info("SideEffect/setNameOfEntity entityId={} characterName={}", entityId, characterName);
        for (ServerLevel world : serverInstance.getAllLevels()) {
            // Find Entity by UUID and update custom name.
            // Also sends to clients I think?
            Mob entity = (Mob) ServerEntityFinder.getEntityByUUID(world, UUID.fromString(entityId));
            if (entity != null) {
                if (!characterName.isEmpty() && !characterName.equals("N/A") && entity.getCustomName() == null) {
                    LOGGER.debug("Setting MC Server Entity name to " + characterName + " for " + entityId);
                    entity.setCustomName(Component.literal(characterName));
                    entity.setCustomNameVisible(true);
                    entity.setPersistenceRequired();
                }
            }
        }
    }

    public static void onLLMGenerateError(String entityId, String errMsg, ServerPlayer player) {
        LOGGER.error("Side effect: onLLMGenerateError, clearing msg. errMsg={}", errMsg);
        String errorMessage = "Error: ";
        errorMessage += EntityChatData.truncateString(errMsg, 55) + "\n";
        errorMessage += "Help is available at player2.game/discord";
        EntityChatData data = getChatData(UUID.fromString(entityId));
        data.setError(errorMessage);

        if (errMsg.contains("Connection refused")) {
            LOGGER.info("Connection refused error! handling case");
            String displayedErrorMessage = "Error: Player2 must be running. Download and run Player2.\nhttps://player2.game\n";
            sendChatAsEntity(entityId, displayedErrorMessage, player, false);
            ServerPackets.SendClickableError(player, displayedErrorMessage, "https://player2.game");
            ServerPackets.SendClickableError(player,
                    "If Player2 is running and it still doesn't work, make a ticket on discord.\nhttps://player2.game/discord",
                    "https://player2.game/discord");
            return;
        }
        if (errMsg.contains("Unauthorized")) {
            EventQueueManager.unauthError(player); // stop processing queues linked to this player.
            ServerPackets.BroadcastUnauthErr(player);
            sendChatAsEntity(entityId, "Please authorize to use AI features.", player, false);
            return;
        }
        sendChatAsEntity(entityId, errorMessage, player, false);
        LOGGER.error("After chat as ent ");
        getChatData(UUID.fromString(entityId)).status = ChatStatus.DISPLAY;
        LOGGER.info("Sending clickable error");
        ServerPackets.SendClickableError(player, errorMessage, "https://player2.game/discord");
    }

    public static void sendChatAsEntity(String entityId, String message, ServerPlayer player,
            boolean shouldBroadcast) {
        LOGGER.info("SIDEEFFECT/sendChatAsEntity entityId={} message={} ", entityId.toString(), message);
        ServerPackets.BroadcastEntityMessage(new EntityChatDataLight(entityId, message, 0, ChatStatus.DISPLAY,
                ChatSender.ASSISTANT, getChatData(UUID.fromString(entityId)).characterSheet,
                getChatData(UUID.fromString(entityId)).players));

        LOGGER.info("Finding entity ");
        Entity entity = ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(),
                UUID.fromString(entityId));
        LOGGER.info("Custom name");
        if (entity == null || entity.getCustomName() == null) {
            return;
        }
        String entityCustomName = entity.getCustomName().getString();
        LOGGER.info("Find entity Type");
        String entityType = entity.getType().toShortString();

        LOGGER.info("player broadcast");
        if (shouldBroadcast) {
            ServerPackets.BroadcastMessage(Component.literal("<" + entityCustomName
                    + " the " + entityType + "> " + message));
        }
    }

    public static void setPending(String entityId) {
        LOGGER.info("SIDEEFFECT/setPending entityId={} ", entityId.toString());
        if (getChatData(UUID.fromString(entityId)).previousMessages.size() == 0) {
            ServerPackets.BroadcastEntityMessage(new EntityChatDataLight(entityId, "", 0, ChatStatus.PENDING,
                    ChatSender.USER, getChatData(UUID.fromString(entityId)).characterSheet,
                    getChatData(UUID.fromString(entityId)).players));
            return;
        }
        setStatusUsingParamsFromChatData(entityId, ChatStatus.PENDING);
    }

    public static void setStatusUsingParamsFromChatData(String entityId, ChatStatus status) {
        LOGGER.info("SideEffect/setStatusUSingParamsFromChatData entityId={} status={}", entityId, status);
        if (getChatData(UUID.fromString(entityId)).previousMessages.size() == 0) {
            throw new RuntimeException("Only call setStatusUsingParamsFromChatData when msgs > 0");
        }
        ChatMessage topMessage = getChatData(UUID.fromString(entityId)).getTopMessage();

        // update chat data
        getChatData(UUID.fromString(entityId)).status = status;

        // broadcast
        ServerPackets.BroadcastEntityMessage(
                new EntityChatDataLight(entityId, topMessage.message,
                        getChatData(UUID.fromString(entityId)).currentLineNumber, status,
                        topMessage.sender, getChatData(UUID.fromString(entityId)).characterSheet,
                        getChatData(UUID.fromString(entityId)).players));

    }

    public static void updateUUID(UUID oldUUID, UUID newUUID) {
        throw new RuntimeException("implement this");
    }

    public static void setLineNumberUsingParamsFromChatData(String entityId, int lineNumber) {
        ChatMessage topMessage = getChatData(UUID.fromString(entityId)).getTopMessage();
        LOGGER.info("sideEffect/setLineNumber entityId={} lineNumber={} topMessage.message={}", entityId, lineNumber,
                topMessage.message);
        // // Ensure the lineNumber is within the valid range
        int totalLines = getChatData(UUID.fromString(entityId)).getWrappedLines().size();

        // update chat data
        getChatData(UUID.fromString(entityId)).currentLineNumber = Math.min(Math.max(lineNumber, 0), totalLines);

        ServerPackets.BroadcastEntityMessage(new EntityChatDataLight(entityId, topMessage.message,
                getChatData(UUID.fromString(entityId)).currentLineNumber, ChatStatus.DISPLAY, topMessage.sender,
                getChatData(UUID.fromString(entityId)).characterSheet, getChatData(UUID.fromString(entityId)).players));
    }
}