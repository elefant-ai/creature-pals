package com.owlmaddie.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;
import com.owlmaddie.chat.MessageData.MessageDataType;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.controls.SpeedControls;
import com.owlmaddie.goals.*;
import com.owlmaddie.message.Behavior;
import com.owlmaddie.message.MessageParser;
import com.owlmaddie.message.ParsedMessage;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.particle.ParticleEmitter;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.utils.ServerEntityFinder;
import com.owlmaddie.utils.VillagerEntityAccessor;
import com.owlmaddie.utils.WitherEntityAccessor;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.VillageGossipType;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.owlmaddie.network.ServerPackets.*;

/**
 * The {@code EntityChatData} class represents a conversation between an
 * entity and one or more players, including friendship, character sheets,
 * and the status of the current displayed message.
 */
public class EntityChatData {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public String entityId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public String characterSheet;
    public ChatDataManager.ChatSender sender;
    public int auto_generated;
    public List<ChatMessage> previousMessages;
    public Long born;
    public Long death;

    @SerializedName("playerId")
    @Expose(serialize = false)
    private String legacyPlayerId;

    @SerializedName("friendship")
    @Expose(serialize = false)
    public Integer legacyFriendship;

    // The map to store data for each player interacting with this entity
    public Map<String, PlayerData> players;

    public EntityChatData(String entityId) {
        this.entityId = entityId;
        this.players = new HashMap<>();
        this.currentMessage = "";
        this.currentLineNumber = 0;
        this.characterSheet = "";
        this.status = ChatDataManager.ChatStatus.NONE;
        this.sender = ChatDataManager.ChatSender.USER;
        this.auto_generated = 0;
        this.previousMessages = new ArrayList<>();
        this.born = System.currentTimeMillis();

        // Old, unused migrated properties
        this.legacyPlayerId = null;
        this.legacyFriendship = null;
    }

    // Post-deserialization initialization
    public void postDeserializeInitialization() {
        if (this.players == null) {
            this.players = new HashMap<>(); // Ensure players map is initialized
        }
        if (this.legacyPlayerId != null && !this.legacyPlayerId.isEmpty()) {
            this.migrateData();
        }
        EventQueueManager.addEntityIdToCreate(entityId);
    }

    // Migrate old data into the new structure
    private void migrateData() {
        // Ensure the blank player data entry exists
        PlayerData blankPlayerData = this.players.computeIfAbsent("", k -> new PlayerData());

        // Update the previousMessages arraylist and add timestamps if missing
        if (this.previousMessages != null) {
            for (ChatMessage message : this.previousMessages) {
                if (message.timestamp == null) {
                    message.timestamp = System.currentTimeMillis();
                }
                if (message.name == null || message.name.isEmpty()) {
                    message.name = "";
                }
            }
        }
        blankPlayerData.friendship = this.legacyFriendship;
        if (this.born == null) {
            this.born = System.currentTimeMillis();
        }

        // Clean up old player data
        this.legacyPlayerId = null;
        this.legacyFriendship = null;
    }

    // Get the player data (or fallback to the blank player)
    public PlayerData getPlayerData(String playerName) {
        if (this.players == null) {
            return new PlayerData();
        }

        // Check if the playerId exists in the players map
        if (this.players.containsKey("")) {
            // If a blank migrated legacy entity is found, always return this
            return this.players.get("");

        } else if (this.players.containsKey(playerName)) {
            // Return a specific player's data
            return this.players.get(playerName);

        } else {
            // Return a blank player data
            PlayerData newPlayerData = new PlayerData();
            this.players.put(playerName, newPlayerData);
            return newPlayerData;
        }
    }

    // Generate light version of chat data (no previous messages)
    public EntityChatDataLight toLightVersion(String playerName) {
        return new EntityChatDataLight(this, playerName);
    }

    public String getCharacterProp(String propertyName) {
        // Create a case-insensitive regex pattern to match the property name and
        // capture its value
        Pattern pattern = Pattern.compile("-?\\s*" + Pattern.quote(propertyName) + ":\\s*(.+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(characterSheet);

        if (matcher.find()) {
            // Return the captured value, trimmed of any excess whitespace
            return matcher.group(1).trim().replace("\"", "");
        }

        return "N/A";
    }

    // Generate context object
    public Map<String, String> getPlayerContext(ServerPlayerEntity player, String userLanguage,
            ConfigurationHandler.Config config) {
        // Add PLAYER context information
        Map<String, String> contextData = new HashMap<>();
        contextData.put("player_name", player.getDisplayName().getString());
        contextData.put("player_health", Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()));
        contextData.put("player_hunger", String.valueOf(player.getHungerManager().getFoodLevel()));
        contextData.put("player_held_item", String.valueOf(player.getMainHandStack().getItem().toString()));
        contextData.put("player_biome",
                player.getWorld().getBiome(player.getBlockPos()).getKey().get().getValue().getPath());
        contextData.put("player_is_creative", player.isCreative() ? "yes" : "no");
        contextData.put("player_is_swimming", player.isSwimming() ? "yes" : "no");
        contextData.put("player_is_on_ground", player.isOnGround() ? "yes" : "no");
        contextData.put("player_language", userLanguage);

        ItemStack feetArmor = player.getInventory().armor.get(0);
        ItemStack legsArmor = player.getInventory().armor.get(1);
        ItemStack chestArmor = player.getInventory().armor.get(2);
        ItemStack headArmor = player.getInventory().armor.get(3);
        contextData.put("player_armor_head", headArmor.getItem().toString());
        contextData.put("player_armor_chest", chestArmor.getItem().toString());
        contextData.put("player_armor_legs", legsArmor.getItem().toString());
        contextData.put("player_armor_feet", feetArmor.getItem().toString());

        // Get active player effects
        String effectsString = player.getActiveStatusEffects().entrySet().stream()
                .map(entry -> entry.getKey().getTranslationKey() + " x" + (entry.getValue().getAmplifier() + 1))
                .collect(Collectors.joining(", "));
        contextData.put("player_active_effects", effectsString);

        // Add custom story section (if any)
        if (!config.getStory().isEmpty()) {
            contextData.put("story", "Story: " + config.getStory());
        } else {
            contextData.put("story", "");
        }

        // Get World time (as 24 hour value)
        int hours = (int) ((player.getWorld().getTimeOfDay() / 1000 + 6) % 24); // Minecraft day starts at 6 AM
        int minutes = (int) (((player.getWorld().getTimeOfDay() % 1000) / 1000.0) * 60);
        contextData.put("world_time", String.format("%02d:%02d", hours, minutes));
        contextData.put("world_is_raining", player.getWorld().isRaining() ? "yes" : "no");
        contextData.put("world_is_thundering", player.getWorld().isThundering() ? "yes" : "no");
        contextData.put("world_difficulty", player.getWorld().getDifficulty().getName());
        contextData.put("world_is_hardcore", player.getWorld().getLevelProperties().isHardcore() ? "yes" : "no");

        // Get moon phase
        String moonPhaseDescription = switch (player.getWorld().getMoonPhase()) {
            case 0 -> "Full Moon";
            case 1 -> "Waning Gibbous";
            case 2 -> "Last Quarter";
            case 3 -> "Waning Crescent";
            case 4 -> "New Moon";
            case 5 -> "Waxing Crescent";
            case 6 -> "First Quarter";
            case 7 -> "Waxing Gibbous";
            default -> "Unknown";
        };
        contextData.put("world_moon_phase", moonPhaseDescription);

        // Get Entity details
        MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(player.getServerWorld(),
                UUID.fromString(entityId));
        if (entity.getCustomName() == null) {
            contextData.put("entity_name", "");
        } else {
            contextData.put("entity_name", entity.getCustomName().getString());
        }
        contextData.put("entity_type", entity.getType().getName().getString());
        contextData.put("entity_health", Math.round(entity.getHealth()) + "/" + Math.round(entity.getMaxHealth()));
        contextData.put("entity_personality", getCharacterProp("Personality"));
        contextData.put("entity_speaking_style", getCharacterProp("Speaking Style / Tone"));
        contextData.put("entity_likes", getCharacterProp("Likes"));
        contextData.put("entity_dislikes", getCharacterProp("Dislikes"));
        contextData.put("entity_age", getCharacterProp("Age"));
        contextData.put("entity_alignment", getCharacterProp("Alignment"));
        contextData.put("entity_class", getCharacterProp("Class"));
        contextData.put("entity_skills", getCharacterProp("Skills"));
        contextData.put("entity_background", getCharacterProp("Background"));
        if (entity.age < 0) {
            contextData.put("entity_maturity", "Baby");
        } else {
            contextData.put("entity_maturity", "Adult");
        }

        PlayerData playerData = this.getPlayerData(player.getDisplayName().getString());
        if (playerData != null) {
            contextData.put("entity_friendship", String.valueOf(playerData.friendship));
        } else {
            contextData.put("entity_friendship", String.valueOf(0));
        }

        return contextData;
    }

    public void generateCharacterMessage(MessageData data, Consumer<String> onSuccess, Consumer<String> onError,
            Consumer<String> onGreeting) {
        String systemPrompt = "system-character";
        if (data.is_auto_message) {
            this.auto_generated++;
        } else {
            this.auto_generated = 0;
        }

        this.addMessage(data.userMessage, ChatDataManager.ChatSender.USER, data.player, systemPrompt);
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(),
                systemPrompt);
        Map<String, String> contextData = getPlayerContext(data.player, data.userLanguage, config);

        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData, previousMessages, false, "")
                .thenAccept(output_message -> {
                    // get rid of the "generate greeting with ..." user message
                    previousMessages.clear();
                    try {
                        if (output_message != null) {
                            previousMessages.clear();
                            this.characterSheet = output_message;
                            String characterName = Optional.ofNullable(getCharacterProp("name"))
                                    .filter(s -> !s.isEmpty())
                                    .orElse("N/A");
                            if (characterName.equals("N/A")) {
                                throw new RuntimeException("Generated N/A as a character name");
                            }
                            if (data.type == MessageDataType.GreetingAndCharacter) {
                                String shortGreeting = Optional.ofNullable(getCharacterProp("short greeting"))
                                        .filter(s -> !s.isEmpty())
                                        .orElse(Randomizer.getRandomMessage(Randomizer.RandomType.NO_RESPONSE))
                                        .replace("\n", " ");
                                this.addMessage(shortGreeting, ChatDataManager.ChatSender.ASSISTANT, data.player,
                                        systemPrompt);
                                onGreeting.accept(shortGreeting);
                            }
                            onSuccess.accept(characterName);
                        } else {
                            throw new RuntimeException(ChatGPTRequest.lastErrorMessage);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing LLM response, clearing msg. Error:", e);
                        String errorMessage = "Error: ";
                        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                            errorMessage += truncateString(e.getMessage(), 55) + "\n";
                        }
                        errorMessage += "Help is available at elefant.gg/discord";
                        onError.accept(errorMessage);
                    }

                });
    }

    public void generateEntityResponse(String userLanguage, ServerPlayerEntity player, Consumer<String> onGenerate,
            Consumer<String> onError) {
        String systemPrompt = "system-chat";
        // Get config (api key, url, settings)
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(),
                systemPrompt);

        // Add PLAYER context information
        Map<String, String> contextData = getPlayerContext(player, userLanguage, config);

        // Get messages for player

        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData, previousMessages, false,
                "Reminder: Respond with a empty message only when \\\"\\\" you detect a lot of repetitive content in conversations (multiple byes, etc.).")
                .thenAccept(ent_msg -> {
                    try {
                        if (ent_msg != null) {
                            ParsedMessage result = MessageParser.parseMessage(ent_msg.replace("\n", " "));
                            PlayerData playerData = this.getPlayerData(player.getDisplayName().getString());
                            BehaviorApplier.apply(result.getBehaviors(), player, entityId, playerData);

                            String cleanedMessage = result.getCleanedMessage();
                            if (cleanedMessage.isEmpty()) {
                                // do not call addMessage
                                this.addMessage("...", ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);
                                onGenerate.accept("");
                                return;
                            }
                            // Add ASSISTANT message to history
                            this.addMessage(cleanedMessage, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);
                            // Update the last entry in previousMessages to use the original message
                            this.previousMessages.set(this.previousMessages.size() - 1,
                                    new ChatMessage(result.getOriginalMessage(), ChatDataManager.ChatSender.ASSISTANT,
                                            player.getDisplayName().getString()));

                            onGenerate.accept(cleanedMessage);
                        } else {
                            // No valid LLM response
                            throw new RuntimeException(ChatGPTRequest.lastErrorMessage);
                        }
                    } catch (Exception e) {
                        // Log the exception for debugging
                        LOGGER.error("Error processing LLM response", e);

                        // Error / No Chat Message (Failure)
                        String randomErrorMessage = Randomizer.getRandomMessage(Randomizer.RandomType.ERROR);
                        this.addMessage(randomErrorMessage, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

                        // Remove the error message from history to prevent it from affecting future
                        // ChatGPT requests
                        if (!previousMessages.isEmpty()) {
                            previousMessages.remove(previousMessages.size() - 1);
                        }
                        // Send clickable error message
                        String errorMessage = "Error: ";
                        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                            errorMessage += truncateString(e.getMessage(), 55) + "\n";
                        }
                        errorMessage += "Help is available at elefant.gg/discord";
                        onError.accept(errorMessage);
                    }
                });
    }

    public static String truncateString(String input, int maxLength) {
        return input.length() > maxLength ? input.substring(0, maxLength - 3) + "..." : input;
    }

    // Add a message to the history and update the current message
    public void addMessage(String message, ChatDataManager.ChatSender sender, ServerPlayerEntity player,
            String systemPrompt) {
        // Truncate message (prevent crazy long messages... just in case)
        String truncatedMessage = message.substring(0,
                Math.min(message.length(), ChatDataManager.MAX_CHAR_IN_USER_MESSAGE));

        // Add context-switching logic for USER messages only
        String playerName = player.getDisplayName().getString();
        if (sender == ChatDataManager.ChatSender.USER && previousMessages.size() > 1) {
            ChatMessage lastMessage = previousMessages.get(previousMessages.size() - 1);
            if (lastMessage.name == null || !lastMessage.name.equals(playerName)) { // Null-safe check
                boolean isReturningPlayer = previousMessages.stream().anyMatch(msg -> playerName.equals(msg.name)); // Avoid
                                                                                                                    // NPE
                                                                                                                    // here
                                                                                                                    // too
                String note = isReturningPlayer
                        ? "<returning player: " + playerName + " resumes the conversation>"
                        : "<a new player has joined the conversation: " + playerName + ">";
                previousMessages.add(new ChatMessage(note, sender, playerName));

                // Log context-switching message
                LOGGER.info(
                        "Conversation-switching message: status=PENDING, sender={}, message={}, player={}, entity={}",
                        ChatDataManager.ChatStatus.PENDING, note, playerName, entityId);
            }
            status = ChatDataManager.ChatStatus.PENDING;
        }

        // Add message to history
        previousMessages.add(new ChatMessage(truncatedMessage, sender, playerName));

        // Update current message and reset line number of displayed text
        this.currentMessage = truncatedMessage;
        this.currentLineNumber = 0;
        this.sender = sender;

        if (sender == ChatDataManager.ChatSender.ASSISTANT) {
            status = ChatDataManager.ChatStatus.DISPLAY;
        }

        // if (sender == ChatDataManager.ChatSender.USER &&
        // systemPrompt.equals("system-chat") && auto_generated == 0) {
        // // Broadcast new player message (when not auto-generated)
        // ServerPackets.BroadcastPlayerMessage(this, player, false);
        // }

        // Broadcast new entity message status (i.e. pending)
        ServerPackets.BroadcastEntityMessage(this);
    }

    // Get wrapped lines
    public List<String> getWrappedLines() {
        return LineWrapper.wrapLines(this.currentMessage, ChatDataManager.MAX_CHAR_PER_LINE);
    }

    public boolean isEndOfMessage() {
        int totalLines = this.getWrappedLines().size();
        // Check if the current line number plus DISPLAY_NUM_LINES covers or exceeds the
        // total number of lines
        return currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES >= totalLines;
    }

    public void setLineNumber(Integer lineNumber) {
        int totalLines = this.getWrappedLines().size();
        // Ensure the lineNumber is within the valid range
        currentLineNumber = Math.min(Math.max(lineNumber, 0), totalLines);

        // Broadcast to all players
        ServerPackets.BroadcastEntityMessage(this);
    }

    public void setStatus(ChatDataManager.ChatStatus new_status) {
        status = new_status;

        // Broadcast to all players
        ServerPackets.BroadcastEntityMessage(this);
    }
}